'use server'

import type {
  AuthenticationResponseJSON,
  PublicKeyCredentialRequestOptionsJSON,
} from '@simplewebauthn/browser'
import * as backend from '@/lib/backend'
import { createSession } from '@/lib/session'
import { ActionResult } from '@/types/api'

/**
 * Login step 1: ask the backend for authentication options (including a fresh challenge).
 */
export async function startLoginAction(
  username: string
): Promise<ActionResult<PublicKeyCredentialRequestOptionsJSON>> {
  if (!username.trim()) {
    return { ok: false, error: 'Vennligst skriv inn et brukernavn.' }
  }

  try {
    const options = await backend.startLogin(username)
    return { ok: true, data: options }
  } catch (err) {
    return { ok: false, error: `Innloggingsfeil: ${errorMessage(err)}` }
  }
}

/**
 * Login step 3: send the signed assertion to the backend for verification.
 * On success the backend JWT is stored in an httpOnly cookie.
 */
export async function finishLoginAction(
  username: string,
  assertionResponse: AuthenticationResponseJSON
): Promise<ActionResult> {
  try {
    const result = await backend.finishLogin(username, assertionResponse)
    if (!result.verified) {
      return { ok: false, error: 'Innlogging feilet: Ukjent feil' }
    }
    await createSession(result.token)
    return { ok: true, data: undefined }
  } catch (err) {
    return { ok: false, error: `Innloggingsfeil: ${errorMessage(err)}` }
  }
}

function errorMessage(err: unknown) {
  return err instanceof Error ? err.message : 'Ukjent feil'
}
