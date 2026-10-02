import { redirect } from 'next/navigation'
import { Container } from '@/components/ui/Container'
import { Button } from '@/components/ui/Button'
import { UserInfo } from '@/components/features/UserInfo'
import { PasskeysList } from '@/components/features/PasskeysList'
import { getSessionToken } from '@/lib/session'
import { getPasskeys, UnauthorizedError } from '@/lib/backend'
import { logoutAction } from '@/app/actions/session'

export default async function DashboardPage() {
  // The JWT lives in an httpOnly cookie, so it can be read here on the server
  const token = await getSessionToken()
  if (!token) {
    redirect('/')
  }

  // The backend validates the token and returns 401 if it is invalid or expired
  const data = await getPasskeys(token).catch((err) => {
    if (err instanceof UnauthorizedError) {
      redirect('/')
    }
    throw err
  })

  return (
    <Container>
      <UserInfo name={data.name ?? ''} />

      <div className="my-6">
        <h3 className="text-base text-fg-label mb-4">
          Dine passkeys
        </h3>
        <PasskeysList passkeys={data.passkeys} />
      </div>

      <form action={logoutAction} className="flex">
        <Button type="submit" variant="secondary">
          Logg ut
        </Button>
      </form>
    </Container>
  )
}
