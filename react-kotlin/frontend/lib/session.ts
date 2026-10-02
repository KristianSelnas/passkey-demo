import 'server-only'
import { cookies } from 'next/headers'

const SESSION_COOKIE = 'session'

/**
 * Stores the JWT issued by the backend in an httpOnly cookie.
 * The browser sends it automatically, but JavaScript cannot read it.
 */
export async function createSession(token: string) {
  const cookieStore = await cookies()
  cookieStore.set(SESSION_COOKIE, token, {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax',
    path: '/',
    maxAge: 60 * 60 * 2, // Same lifetime as the backend JWT (jwt.expiration)
  })
}

export async function getSessionToken(): Promise<string | undefined> {
  const cookieStore = await cookies()
  return cookieStore.get(SESSION_COOKIE)?.value
}

export async function deleteSession() {
  const cookieStore = await cookies()
  cookieStore.delete(SESSION_COOKIE)
}
