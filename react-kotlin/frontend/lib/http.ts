import 'server-only'

// Only the Next.js server talks to the backend, so the browser never needs CORS
const BACKEND_URL = process.env.BACKEND_URL ?? 'http://localhost:8080'

export class UnauthorizedError extends Error {}

export async function get<T>(path: string, token: string, fallbackError: string): Promise<T> {
  const response = await fetch(`${BACKEND_URL}${path}`, {
    headers: { Authorization: `Bearer ${token}` },
    cache: 'no-store',
  })

  return handleResponse(response, fallbackError)
}

export async function post<T>(path: string, body: unknown, fallbackError: string): Promise<T> {
  const response = await fetch(`${BACKEND_URL}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })

  return handleResponse(response, fallbackError)
}

async function handleResponse<T>(response: Response, fallbackError: string): Promise<T> {
  // Token expired or invalid
  if (response.status === 401) {
    throw new UnauthorizedError('Authentication required')
  }

  if (!response.ok) {
    const error = await response.json().catch(() => ({}))
    throw new Error(error.error || fallbackError)
  }

  return response.json()
}
