// Hjelpefunksjoner for WebAuthn med native API.
// Disse hadde ikke vært nødvendige dersom vi hadde brukt
// @simplewebauthn/browser.

// Base64url encoding/decoding
function base64urlToBuffer(base64url) {
  const base64 = base64url.replace(/-/g, "+").replace(/_/g, "/");
  const padLen = (4 - (base64.length % 4)) % 4;
  const padded = base64 + "=".repeat(padLen);
  const binary = atob(padded);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes.buffer;
}

function bufferToBase64url(buffer) {
  const bytes = new Uint8Array(buffer);
  let binary = "";
  for (let i = 0; i < bytes.length; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=/g, "");
}

// Konverter registreringsopsjoner fra server (JSON) til format for navigator.credentials.create()
export function parseRegistrationOptions(options) {
  return {
    publicKey: {
      challenge: base64urlToBuffer(options.challenge),
      rp: {
        name: options.rp.name,
        id: options.rp.id,
      },
      user: {
        id: base64urlToBuffer(options.user.id),
        name: options.user.name,
        displayName: options.user.displayName,
      },
      pubKeyCredParams: options.pubKeyCredParams,
      timeout: options.timeout,
      excludeCredentials: options.excludeCredentials?.map((cred) => ({
        type: cred.type,
        id: base64urlToBuffer(cred.id),
        transports: cred.transports,
      })),
      authenticatorSelection: options.authenticatorSelection,
      attestation: options.attestation,
    },
  };
}

// Konverter autentiseringsopsjoner fra server (JSON) til format for navigator.credentials.get()
export function parseAuthenticationOptions(options) {
  return {
    publicKey: {
      challenge: base64urlToBuffer(options.challenge),
      timeout: options.timeout,
      rpId: options.rpId,
      allowCredentials: options.allowCredentials?.map((cred) => ({
        type: cred.type,
        id: base64urlToBuffer(cred.id),
        transports: cred.transports,
      })),
      userVerification: options.userVerification,
    },
  };
}

// Konverter registreringsrespons til format serveren forventer
export function createAttestationResponse(credential) {
  return {
    id: credential.id,
    rawId: bufferToBase64url(credential.rawId),
    type: credential.type,
    response: {
      clientDataJSON: bufferToBase64url(credential.response.clientDataJSON),
      attestationObject: bufferToBase64url(
        credential.response.attestationObject,
      ),
      transports: credential.response.getTransports
        ? credential.response.getTransports()
        : [],
    },
  };
}

// Konverter autentiseringsrespons til format serveren forventer
export function formatAuthenticationResponse(credential) {
  return {
    id: credential.id,
    rawId: bufferToBase64url(credential.rawId),
    type: credential.type,
    response: {
      clientDataJSON: bufferToBase64url(credential.response.clientDataJSON),
      authenticatorData: bufferToBase64url(
        credential.response.authenticatorData,
      ),
      signature: bufferToBase64url(credential.response.signature),
      userHandle: credential.response.userHandle
        ? bufferToBase64url(credential.response.userHandle)
        : null,
    },
  };
}
