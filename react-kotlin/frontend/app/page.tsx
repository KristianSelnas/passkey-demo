import Link from 'next/link'
import { Container } from '@/components/ui/Container'
import { DemoRibbon } from '@/components/ui/DemoRibbon'
import { LoginForm } from '@/components/features/LoginForm'

export default function HomePage() {
  return (
    <>
      <DemoRibbon />
      <Container>
        <h1>
          Passkey Demo
        </h1>
        <p className="text-center text-muted mb-8">
          Logg inn med passkey
        </p>

        <LoginForm />

        <div className="text-center mt-6 text-muted">
          Første gang?{' '}
          <Link
            href="/register"
            className="text-brand-light no-underline font-semibold hover:text-brand-lighter hover:underline"
          >
            Registrer deg her
          </Link>
        </div>
      </Container>
    </>
  )
}
