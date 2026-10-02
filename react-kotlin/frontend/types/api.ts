export interface PasskeyInfo {
  id: string
  counter: number
  transports?: string[]
}

export interface PasskeysResponse {
  passkeys: PasskeyInfo[]
  name: string | null
}

export interface LoginFinishResponse {
  verified: boolean
  username: string
  name: string | null
  token: string
}

export interface RegistrationFinishResponse {
  verified: boolean
}

/**
 * Return value from Server Functions. Errors are returned (not thrown) because
 * Next.js hides thrown error messages from the client in production.
 */
export type ActionResult<T = undefined> =
  | { ok: true; data: T }
  | { ok: false; error: string }
