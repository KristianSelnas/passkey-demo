'use client'

import { useActionState } from 'react'
import { useRouter } from 'next/navigation'
import { startAuthentication } from '@simplewebauthn/browser'
import { Button } from '../ui/Button'
import { Input } from '../ui/Input'
import { FormGroup } from '../ui/FormGroup'
import { StatusMessage } from '../ui/StatusMessage'
import { startLoginAction, finishLoginAction } from '@/app/actions/authentication'

interface LoginState {
  username: string
  error: string | null
}

export function LoginForm() {
  const router = useRouter()

  async function login(_prevState: LoginState, formData: FormData): Promise<LoginState> {
    const username = String(formData.get('username') ?? '')

    // Step 1 (server): get authentication options and challenge from the backend
    const options = await startLoginAction(username)
    if (!options.ok) {
      return { username, error: options.error }
    }

    // Step 2 (browser): let the authenticator sign the challenge (Touch ID, Face ID, security key)
    let assertionResponse
    try {
      assertionResponse = await startAuthentication({ optionsJSON: options.data })
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Ukjent feil'
      return { username, error: `Innloggingsfeil: ${errorMessage}` }
    }

    // Step 3 (server): verify the assertion. On success the server sets the session cookie
    const result = await finishLoginAction(username, assertionResponse)
    if (!result.ok) {
      return { username, error: result.error }
    }

    router.push('/dashboard')
    return { username, error: null }
  }

  const [state, formAction, pending] = useActionState(login, { username: '', error: null })

  return (
    <>
      <form action={formAction} className="flex flex-col gap-2">
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
            Logg inn
          </Button>
        </div>
      </form>

      {pending && <StatusMessage type="info" message="Starter innlogging..." />}
      {!pending && state.error && <StatusMessage type="error" message={state.error} />}
    </>
  )
}
