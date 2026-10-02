'use client'

import { useActionState } from 'react'
import { useRouter } from 'next/navigation'
import { startRegistration } from '@simplewebauthn/browser'
import { Button } from '../ui/Button'
import { Input } from '../ui/Input'
import { FormGroup } from '../ui/FormGroup'
import { StatusMessage } from '../ui/StatusMessage'
import { startRegistrationAction, finishRegistrationAction } from '@/app/actions/registration'

interface RegisterState {
  name: string
  username: string
  status: { type: 'success' | 'error'; message: string } | null
}

const initialState: RegisterState = { name: '', username: '', status: null }

export function RegisterForm() {
  const router = useRouter()

  async function register(_prevState: RegisterState, formData: FormData): Promise<RegisterState> {
    const name = String(formData.get('name') ?? '')
    const username = String(formData.get('username') ?? '')

    // Step 1 (server): get registration options and challenge from the backend
    const options = await startRegistrationAction(username, name)
    if (!options.ok) {
      return { name, username, status: { type: 'error', message: options.error } }
    }

    // Step 2 (browser): let the authenticator create a new key pair (Touch ID, Face ID, security key)
    let attestationResponse
    try {
      attestationResponse = await startRegistration({ optionsJSON: options.data })
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Ukjent feil'
      return { name, username, status: { type: 'error', message: `Registreringsfeil: ${errorMessage}` } }
    }

    // Step 3 (server): verify the attestation and store the public key
    const result = await finishRegistrationAction(username, attestationResponse)
    if (!result.ok) {
      return { name, username, status: { type: 'error', message: result.error } }
    }

    setTimeout(() => {
      router.push('/')
    }, 1500)
    return { name, username, status: { type: 'success', message: 'Passkey registrert! Går til innlogging...' } }
  }

  const [state, formAction, pending] = useActionState(register, initialState)

  return (
    <>
      <form action={formAction} className="flex flex-col gap-2">
        <FormGroup label="Navn" htmlFor="name">
          <Input
            type="text"
            id="name"
            name="name"
            placeholder="Ditt navn"
            defaultValue={state.name}
            autoComplete="name"
            required
          />
        </FormGroup>

        <FormGroup label="E-postadresse" htmlFor="username">
          <Input
            type="email"
            id="username"
            name="username"
            placeholder="din@epost.no"
            defaultValue={state.username}
            autoComplete="email webauthn"
            required
          />
        </FormGroup>

        <div className="flex gap-4">
          <Button type="submit" variant="primary" disabled={pending}>
            Registrer
          </Button>
        </div>
      </form>

      {pending && <StatusMessage type="info" message="Starter registrering..." />}
      {!pending && state.status && (
        <StatusMessage type={state.status.type} message={state.status.message} />
      )}
    </>
  )
}
