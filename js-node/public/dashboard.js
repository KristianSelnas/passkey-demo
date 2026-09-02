import { fetchJSON, getStoredUsername, clearStoredUsername, navigateTo } from "./common.js";

// Last og vis passkeys ved oppstart
async function loadPasskeys() {
  const username = getStoredUsername();

  if (!username) {
    navigateTo("index.html");
    return;
  }

  try {
    const data = await fetchJSON(`/api/passkeys?username=${encodeURIComponent(username)}`);

    if (data.error) {
      console.error("Feil ved henting av passkeys:", data.error);
      return;
    }

    // Vis navn hvis tilgjengelig, ellers e-postadresse
    const displayNameEl = document.getElementById("display-name");
    if (displayNameEl) {
      const storedName = sessionStorage.getItem("name");
      displayNameEl.textContent = data.name || storedName || username;
    }

    displayPasskeys(data.passkeys);
  } catch (err) {
    console.error("Feil ved lasting av passkeys:", err);
  }
}

function displayPasskeys(passkeys) {
  const listEl = document.getElementById("passkeys-list");
  if (!listEl) return;

  if (!passkeys || passkeys.length === 0) {
    listEl.innerHTML = '<p class="no-passkeys">Ingen passkeys registrert</p>';
    return;
  }

  listEl.innerHTML = passkeys.map((passkey, index) => `
    <div class="passkey-item">
      <div class="passkey-info">
        <strong>Passkey ${index + 1}</strong>
        <span class="passkey-id">${passkey.id}</span>
        <span class="passkey-counter">Teller: ${passkey.counter}</span>
        ${passkey.transports ? `<span class="passkey-transports">Transporttyper: ${passkey.transports.join(", ")}</span>` : ""}
      </div>
    </div>
  `).join("");
}

function logout() {
  clearStoredUsername();
  navigateTo("index.html");
}

// Eksponer funksjoner globalt
window.logout = logout;

// Last passkeys ved oppstart
document.addEventListener("DOMContentLoaded", loadPasskeys);
