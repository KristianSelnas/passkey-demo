import Link from 'next/link'
import { Container } from '@/components/ui/Container'
import { DemoRibbon } from '@/components/ui/DemoRibbon'
import { RegisterForm } from '@/components/features/RegisterForm'

export default function RegisterPage() {
  return (
    <>
      <DemoRibbon />
      <Container>
        <h1>
          Passkey Demo
        </h1>
        <p className="text-center text-muted mb-8">
          Registrer deg med passkey
        </p>

        <RegisterForm />

        <div className="text-center mt-6 text-muted">
          Allerede registrert?{' '}
          <Link
            href="/"
            className="text-brand-light no-underline font-semibold hover:text-brand-lighter hover:underline"
          >
            Logg inn her
          </Link>
        </div>
      </Container>
    </>
  )
}
