import 'server-only'
import type {
  AuthenticationResponseJSON,
  PublicKeyCredentialCreationOptionsJSON,
  PublicKeyCredentialRequestOptionsJSON,
  RegistrationResponseJSON,
} from '@simplewebauthn/browser'
import {
  LoginFinishResponse,
  PasskeysResponse,
  RegistrationFinishResponse,
} from '@/types/api'
import { get, post } from '@/lib/http'

export { UnauthorizedError } from '@/lib/http'

export async function startRegistration(
  username: string,
  name: string
): Promise<PublicKeyCredentialCreationOptionsJSON> {
  return post('/api/register/start', { username, name }, 'Registration start failed')
}

export async function finishRegistration(
  username: string,
  attestationResponse: RegistrationResponseJSON
): Promise<RegistrationFinishResponse> {
  return post('/api/register/finish', { username, attestationResponse }, 'Registration failed')
}

export async function startLogin(username: string): Promise<PublicKeyCredentialRequestOptionsJSON> {
  return post('/api/login/start', { username }, 'Login start failed')
}

export async function finishLogin(
  username: string,
  assertionResponse: AuthenticationResponseJSON
): Promise<LoginFinishResponse> {
  return post('/api/login/finish', { username, assertionResponse }, 'Login failed')
}

export async function getPasskeys(token: string): Promise<PasskeysResponse> {
  return get('/api/passkeys', token, 'Failed to fetch passkeys')
}
