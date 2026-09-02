import {
  parseAuthenticationOptions as mapToCredentialOptions,
  formatAuthenticationResponse as mapToAssertionResponse,
} from "./webauthn-utils.js";
import { showStatus, getUsername, saveUsername, navigateTo } from "./common.js";

async function login(event) {
  event.preventDefault();

  const username = getUsername();
  if (!username) return;

  try {
    showStatus("Starter innlogging...", "info");

    // Hent autentiseringsopsjoner fra serveren
    const optionsResponse = await fetch("/api/login/start", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username }),
    });
    const options = await optionsResponse.json();

    if (options.error) {
      showStatus(options.error, "error");
      return;
    }

    // Kall WebAuthn API for å autentisere med passkey
    const credentialOptions = mapToCredentialOptions(options);
    const credential = await navigator.credentials.get(credentialOptions);

    if (!credential) {
      throw new Error("Ingen credential ble returnert");
    }

    // Send assertion til serveren for verifisering
    const assertionResponse = mapToAssertionResponse(credential);
    const verificationResponse = await fetch("/api/login/finish", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, assertionResponse }),
    });
    const result = await verificationResponse.json();

    if (result.verified) {
      saveUsername(username);
      if (result.name) {
        sessionStorage.setItem("name", result.name);
      }
      navigateTo("dashboard.html");
    } else {
      showStatus(
        "Innlogging feilet: " + (result.error || "Ukjent feil"),
        "error",
      );
    }
  } catch (err) {
    showStatus("Innloggingsfeil: " + err.message, "error");
  }
}

document.getElementById("login-form").addEventListener("submit", login);
