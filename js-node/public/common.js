// Felles hjelpefunksjoner for alle sider

export function showStatus(message, type) {
  const statusEl = document.getElementById("status");
  if (statusEl) {
    statusEl.innerHTML = message;
    statusEl.className = type;
  }
}

export function getUsername() {
  const usernameInput = document.getElementById("username");
  if (!usernameInput) return null;

  const username = usernameInput.value.trim();
  if (!username) {
    showStatus("Vennligst skriv inn et brukernavn.", "error");
    return null;
  }
  return username;
}

export function clearStatus() {
  const statusEl = document.getElementById("status");
  if (statusEl) {
    statusEl.className = "";
    statusEl.textContent = "";
  }
}

export async function fetchJSON(url, options = {}) {
  const response = await fetch(url, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...options.headers,
    },
  });
  return response.json();
}

export function navigateTo(page) {
  window.location.href = page;
}

export function saveUsername(username) {
  sessionStorage.setItem("username", username);
}

export function getStoredUsername() {
  return sessionStorage.getItem("username");
}

export function clearStoredUsername() {
  sessionStorage.removeItem("username");
  sessionStorage.removeItem("name");
}
