# Passkey Demo - Next.js + Kotlin Spring Boot

Demo-applikasjon som viser hvordan man kan implementere passkey-basert autentisering med WebAuthn i en
applikasjon med Next.js frontend og Kotlin backend.

Dette er en applikasjon som er nærmere det man ville implementere i en produksjonsapplikasjon enn
[js-node](../js-node), som er en enkel demo med Node.js backend.

**NB: Dette er en demo-applikasjon, og bør ikke brukes i produksjon uten videre sikkerhetsvurderinger.**

## Prosjektstruktur

- `backend/` - Spring Boot Kotlin backend med Maven og webauthn4j
- `frontend/` - Next.js TypeScript frontend med Tailwind CSS

## Teknologier

### Backend
- Spring Boot 4.1
- Kotlin 2.3
- Maven
- H2 in-memory database
- webauthn4j-core for WebAuthn
- Spring Security (OAuth2 resource server) for JWT-validering
- Port: 8080

### Frontend
- Next.js 16 (App Router)
- TypeScript
- Tailwind CSS v4
- @simplewebauthn/browser for WebAuthn
- Port: 3000

## Kjøre applikasjonen

### Start begge med én kommando

Fra rotmappen:

```bash
npm install   # installerer frontend-avhengighetene
npm run dev   # starter backend og frontend samtidig
```

Ctrl+C stopper begge. Vil du heller starte dem hver for seg, følg stegene under.

### 1. Start backend

```bash
cd backend
./mvnw spring-boot:run
```

Backend starter på http://localhost:8080

### 2. Start frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend starter på http://localhost:3000

### 3. Bruk applikasjonen

1. Åpne http://localhost:3000 i nettleseren
2. Klikk "Registrer deg her" for å registrere en ny bruker
3. Skriv inn navn og e-postadresse
4. Bruk Touch ID, Face ID, eller en hardware security key for å opprette en passkey
5. Logg inn med samme e-postadresse
6. Se dine registrerte passkeys på dashboard

## Funksjonalitet

- ✅ Passkey-basert registrering (WebAuthn)
- ✅ Passkey-basert innlogging
- ✅ Dashboard med passkey-liste
- ✅ In-memory storage (H2 database)
- ✅ Integrasjonstester av registrering og innlogging med emulert autentikator
- ✅ Norsk brukergrensesnitt

## API-endepunkter

- `POST /api/register/start` - Start registrering
- `POST /api/register/finish` - Fullfør registrering
- `POST /api/login/start` - Start innlogging
- `POST /api/login/finish` - Fullfør innlogging
- `GET /api/passkeys` - Hent brukerens passkeys (krever `Authorization: Bearer <JWT>`)

## Inspisere databasen i H2-konsollen

Åpne http://localhost:8080/h2-console mens backend kjører, og logg inn med:

- **JDBC URL:** `jdbc:h2:mem:passkeydb`
- **User Name:** `sa`
- **Password:** (blankt)

Tabellene `USERS` og `CREDENTIALS` inneholder registrerte brukere og passkeys. Databasen ligger bare i minnet, så innholdet forsvinner når backend startes på nytt.
