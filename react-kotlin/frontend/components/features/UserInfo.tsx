interface UserInfoProps {
  name: string
}

export function UserInfo({ name }: UserInfoProps) {
  return (
    <div className="text-center">
      <h1>
        Velkommen, {name}!
      </h1>
      <p className="text-muted mb-6">
        Du er logget inn med en passkey.
      </p>
    </div>
  )
}
