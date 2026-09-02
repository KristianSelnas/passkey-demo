import express from "express";
import {
  generateRegistrationOptions,
  verifyRegistrationResponse,
  generateAuthenticationOptions,
  verifyAuthenticationResponse,
} from "@simplewebauthn/server";

const app = express();
app.use(express.json());
app.use(express.static("public"));

// --- Konfigurasjon ---
const rpName = "Passkey Demo";
const rpID = "localhost";
const origin = "http://localhost:3000";

// --- Lagring i minnet ---
const users = new Map(); // brukernavn -> { id, username, name, credentials[] }
const challenges = new Map(); // brukernavn -> gjeldende challenge

function getOrCreateUser(username, name = null) {
  if (!users.has(username)) {
    const id = crypto.randomUUID();
    users.set(username, { id, username, name, credentials: [] });
  } else if (name && !users.get(username).name) {
    // Oppdater navn hvis det ikke er satt fra før
    users.get(username).name = name;
  }
  return users.get(username);
}

function getUser(username) {
  return users.get(username);
}

function getChallenge(username) {
  return challenges.get(username);
}

function saveChallenge(username, challenge) {
  challenges.set(username, challenge);
}

function deleteChallenge(username) {
  challenges.delete(username);
}

function saveCredential(user, credential, transports) {
  user.credentials.push({
    id: credential.id,
    publicKey: credential.publicKey,
    counter: credential.counter,
    transports,
  });
}

// ========================
//  REGISTRERING
// ========================

// Steg 1: Generer registreringsopsjoner
app.post("/api/register/start", async (req, res) => {
  const { username, name } = req.body;
  if (!username) return res.status(400).json({ error: "Username required" });
  if (!name) return res.status(400).json({ error: "Name required" });

  const user = getOrCreateUser(username, name);

  const options = await generateRegistrationOptions({
    rpName,
    rpID,
    userName: username,
    userID: new TextEncoder().encode(user.id),
    // Forhindre at brukere re-registrerer eksisterende autentiseringsenheter
    excludeCredentials: user.credentials.map((c) => ({
      id: c.id,
      transports: c.transports,
    })),
    authenticatorSelection: {
      residentKey: "preferred",
      userVerification: "preferred",
    },
  });

  saveChallenge(username, options.challenge);

  res.json(options);
});

// Steg 2: Verifiser registreringsrespons
app.post("/api/register/finish", async (req, res) => {
  const { username, attestationResponse } = req.body;
  const user = getUser(username);
  const expectedChallenge = getChallenge(username);

  if (!user || !expectedChallenge) {
    return res.status(400).json({ error: "Registration not started" });
  }

  try {
    const verification = await verifyRegistrationResponse({
      response: attestationResponse,
      expectedChallenge,
      expectedOrigin: origin,
      expectedRPID: rpID,
    });

    if (verification.verified && verification.registrationInfo) {
      const { credential } = verification.registrationInfo;

      saveCredential(user, credential, attestationResponse.response.transports);
      deleteChallenge(username);

      res.json({ verified: true });
    } else {
      res.status(400).json({ error: "Verification failed" });
    }
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// ============================
//  AUTENTISERING
// ============================

// Steg 1: Generer autentiseringsalternativer
app.post("/api/login/start", async (req, res) => {
  const { username } = req.body;
  if (!username) return res.status(400).json({ error: "Username required" });

  const user = users.get(username);
  if (!user) return res.status(404).json({ error: "User not found" });

  const options = await generateAuthenticationOptions({
    rpID,
    allowCredentials: user.credentials.map((c) => ({
      id: c.id,
      transports: c.transports,
    })),
    userVerification: "preferred",
  });

  challenges.set(username, options.challenge);

  res.json(options);
});

// Steg 2: Verifiser autentiseringsrespons
app.post("/api/login/finish", async (req, res) => {
  const { username, assertionResponse } = req.body;
  const user = users.get(username);
  const expectedChallenge = challenges.get(username);

  if (!user || !expectedChallenge) {
    return res.status(400).json({ error: "Login not started" });
  }

  const credential = user.credentials.find(
    (c) => c.id === assertionResponse.id,
  );

  if (!credential) {
    return res.status(400).json({ error: "Credential not found" });
  }

  try {
    const verification = await verifyAuthenticationResponse({
      response: assertionResponse,
      expectedChallenge,
      expectedOrigin: origin,
      expectedRPID: rpID,
      credential: {
        id: credential.id,
        publicKey: credential.publicKey,
        counter: credential.counter,
      },
    });

    if (verification.verified) {
      // Oppdater teller for å forhindre replay-angrep
      credential.counter = verification.authenticationInfo.newCounter;
      challenges.delete(username);
      res.json({ verified: true, username: user.username, name: user.name });
    } else {
      res.status(400).json({ error: "Verification failed" });
    }
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// ========================
//  PASSKEYS
// ========================

// Hent registrerte passkeys for en bruker
app.get("/api/passkeys", (req, res) => {
  const { username } = req.query;
  if (!username) return res.status(400).json({ error: "Username required" });

  const user = users.get(username);
  if (!user) return res.status(404).json({ error: "User not found" });

  const passkeys = user.credentials.map((c) => ({
    id: Buffer.from(c.id).toString("base64url").substring(0, 16) + "...",
    counter: c.counter,
    transports: c.transports,
  }));

  res.json({ passkeys, name: user.name });
});

app.listen(3000, () => {
  console.log("Passkey demo running at http://localhost:3000");
});
