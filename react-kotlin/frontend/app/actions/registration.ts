'use server'

import type {
  PublicKeyCredentialCreationOptionsJSON,
  RegistrationResponseJSON,
} from '@simplewebauthn/browser'
import * as backend from '@/lib/backend'
import { ActionResult } from '@/types/api'

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/**
 * Registration step 1: ask the backend for registration options (including a fresh challenge).
 */
export async function startRegistrationAction(
  username: string,
  name: string
): Promise<ActionResult<PublicKeyCredentialCreationOptionsJSON>> {
  if (!name.trim()) {
    return { ok: false, error: 'Vennligst skriv inn navnet ditt.' }
  }
  if (!EMAIL_REGEX.test(username)) {
    return { ok: false, error: 'Vennligst skriv inn en gyldig e-postadresse.' }
  }

  try {
    const options = await backend.startRegistration(username, name)
    return { ok: true, data: options }
  } catch (err) {
    return { ok: false, error: `Registreringsfeil: ${errorMessage(err)}` }
  }
}

/**
 * Registration step 3: send the attestation created by the browser to the backend for verification.
 */
export async function finishRegistrationAction(
  username: string,
  attestationResponse: RegistrationResponseJSON
): Promise<ActionResult> {
  try {
    const result = await backend.finishRegistration(username, attestationResponse)
    if (!result.verified) {
      return { ok: false, error: 'Registrering feilet: Ukjent feil' }
    }
    return { ok: true, data: undefined }
  } catch (err) {
    return { ok: false, error: `Registreringsfeil: ${errorMessage(err)}` }
  }
}

function errorMessage(err: unknown) {
  return err instanceof Error ? err.message : 'Ukjent feil'
}
