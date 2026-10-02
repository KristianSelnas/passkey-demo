import { PasskeyInfo } from '@/types/api'

interface PasskeysListProps {
  passkeys: PasskeyInfo[]
}

export function PasskeysList({ passkeys }: PasskeysListProps) {
  if (!passkeys || passkeys.length === 0) {
    return (
      <div className="bg-canvas border border-line rounded-lg p-4">
        <p className="text-subtle text-center p-6">
          Ingen passkeys registrert
        </p>
      </div>
    )
  }

  return (
    <div className="bg-canvas border border-line rounded-lg p-4 max-h-[30rem] overflow-y-auto">
      {passkeys.map((passkey, index) => (
        <div
          key={passkey.id}
          className={`
            p-4
            ${index < passkeys.length - 1 ? 'border-b border-line-subtle' : ''}
          `}
        >
          <div className="flex flex-col gap-2">
            <strong>
              Passkey {index + 1}
            </strong>
            <span className="text-sm text-muted font-mono break-all">
              {passkey.id}
            </span>
            <span className="text-sm text-muted">
              Teller: {passkey.counter}
            </span>
            {passkey.transports && passkey.transports.length > 0 && (
              <span className="text-sm text-muted">
                Transporttyper: {passkey.transports.join(', ')}
              </span>
            )}
          </div>
        </div>
      ))}
    </div>
  )
}
