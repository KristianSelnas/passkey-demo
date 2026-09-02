# Passkey Demo

En enkel demonstrasjonsapplikasjon som viser hvordan man implementerer passkeys (WebAuthn) i Node.js.

## Kom i gang

```bash
npm install
npm start
```

Åpne http://localhost:3000 i nettleseren.

## Teknologi

- **Backend:** Node.js + Express + `@simplewebauthn/server`
- **Frontend:** Vanilla JavaScript med native WebAuthn API
- **Lagring:** In-memory (data slettes ved restart)

## Hovedkonsepter

### 1. Registrering (to-stegs flyt)

**Steg 1 - Generer challenge:**
```javascript
const options = await generateRegistrationOptions({
  rpName: 'Passkey Demo',
  rpID: 'localhost',
  userID: userId,
  userName: email,
  attestationType: 'none',
  authenticatorSelection: {
    residentKey: 'preferred',
    userVerification: 'preferred'
  }
});
```

**Steg 2 - Verifiser respons:**
```javascript
const verification = await verifyRegistrationResponse({
  response: credential,
  expectedChallenge: challenge,
  expectedOrigin: origin,
  expectedRPID: rpID
});
```

### 2. Autentisering (to-stegs flyt)

**Steg 1 - Generer challenge:**
```javascript
const options = await generateAuthenticationOptions({
  rpID: 'localhost',
  allowCredentials: user.credentials.map(cred => ({
    id: cred.id,
    transports: cred.transports
  }))
});
```

**Steg 2 - Verifiser respons:**
```javascript
const verification = await verifyAuthenticationResponse({
  response: credential,
  expectedChallenge: challenge,
  expectedOrigin: origin,
  expectedRPID: rpID,
  credential: authenticator
});
```

### 3. Frontend (native WebAuthn API)

**Opprett passkey:**
```javascript
const credential = await navigator.credentials.create({ publicKey: parsedOptions });
```

**Autentiser med passkey:**
```javascript
const credential = await navigator.credentials.get({ publicKey: parsedOptions });
```

## Arkitektur

```
/api/register/start  →  Generer challenge + options
/api/register/finish →  Verifiser og lagre credential

/api/login/start     →  Generer challenge + options  
/api/login/finish    →  Verifiser og autentiser
```

Alle credentials lagres med:
- **id**: Credential ID (Uint8Array)
- **publicKey**: Offentlig nøkkel for verifikasjon (Uint8Array)
- **counter**: Signatur-teller (forhindrer replay-angrep)
- **transports**: Hvordan autentikatoren kommuniserer (f.eks. "internal", "hybrid")

## Viktige detaljer

- **rpID** må matche domenet (her: "localhost")
- **origin** må matche full URL inkludert protokoll (her: "http://localhost:3000")
- **Challenge** er engangsverdi som forhindrer replay-angrep
- **Counter** oppdateres ved hver autentisering for å detektere klonede autentikatorer
