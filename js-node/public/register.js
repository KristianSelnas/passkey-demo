import {
  parseRegistrationOptions as mapToCredentialOptions,
  createAttestationResponse as mapToAttestationResponse,
} from "./webauthn-utils.js";
import { showStatus, getUsername, navigateTo } from "./common.js";

function getName() {
  const nameInput = document.getElementById("name");
  if (!nameInput) return null;

  const name = nameInput.value.trim();
  if (!name) {
    showStatus("Vennligst skriv inn navnet ditt.", "error");
    return null;
  }
  return name;
}

function validateEmail(email) {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
}

async function register(event) {
  event.preventDefault();

  const name = getName();
  if (!name) return;

  const username = getUsername();
  if (!username) return;

  if (!validateEmail(username)) {
    showStatus("Vennligst skriv inn en gyldig e-postadresse.", "error");
    return;
  }

  try {
    showStatus("Starter registrering...", "info");

    // Hent registreringsopsjoner fra serveren
    const optionsResponse = await fetch("/api/register/start", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, name }),
    });
    const registrationOptions = await optionsResponse.json();

    // Kall WebAuthn API for å opprette passkey
    const credentialOptions = mapToCredentialOptions(registrationOptions);
    const credential = await navigator.credentials.create(credentialOptions);

    if (!credential) {
      showStatus("Opprettelse av passkey feilet!", "error");
      return;
    }

    // Send attestation til serveren for verifisering
    const attestationResponse = mapToAttestationResponse(credential);
    const verificationResponse = await fetch("/api/register/finish", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, attestationResponse }),
    });
    const result = await verificationResponse.json();

    if (result.verified) {
      showStatus(
        'Passkey registrert! <a href="index.html">Gå til innlogging</a>',
        "success",
      );
    } else {
      showStatus(
        "Registrering feilet: " + (result.error || "Ukjent feil"),
        "error",
      );
    }
  } catch (err) {
    showStatus("Registreringsfeil: " + err.message, "error");
  }
}

document.getElementById("register-form").addEventListener("submit", register);
