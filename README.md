# MedSimple

[![CI](https://github.com/Lawaltunde/med_simple/actions/workflows/ci.yml/badge.svg)](https://github.com/Lawaltunde/med_simple/actions/workflows/ci.yml)

A patient intake and triage app for primary-care clinics: patients report symptoms as they happen,
and physicians see a queue ordered by urgency instead of booking time.

Demo video: https://www.youtube.com/watch?v=YZbIeKGoZLc

## What it does

- **Patients** report symptoms as text, a voice note or a photo (upload or camera). Entries for the
  same complaint are grouped into one episode. A symptom assistant chat gives general,
  non-diagnostic guidance and shows a call-911 banner when it detects a red flag.
- **Physicians** work from a triage queue sorted by urgency (ROUTINE, SEE SOON, URGENT, EMERGENCY).
  Each episode has an AI summary next to the patient's original notes, recordings and photos. The
  physician confirms or overrides the urgency, with a required reason that is logged.
- From the episode, the physician writes a care plan with prescriptions (checked for allergies and
  known drug interactions before signing), lab requisitions and specialist referrals, books
  appointments (with `.ics` calendar export) and releases results with their own interpretation.
- **Clinic admins** manage staff and turn integrations on or off per clinic. A physician must have
  verified credentials before they can take patients.

The app is a proof of concept with six roles (patient, physician, nurse, receptionist, clinic
admin, super admin). It has not been used with real patients.

## How it's built

| Area | Technology |
|---|---|
| Backend | Java 17, Spring Boot 4, Spring Security (JWT), Spring Data JPA |
| Database | PostgreSQL 16, Flyway migrations |
| AI | Spring AI with OpenAI (chat, Whisper transcription, vision), off by default |
| Clinical data | HAPI FHIR R4 |
| Frontend | React 19, TypeScript, Vite, Tailwind CSS, TanStack Query |
| Testing | JUnit, Mockito, AssertJ, Vitest, React Testing Library |
| CI | GitHub Actions: backend tests against PostgreSQL, frontend tests and build, gitleaks |

Code is organised by feature (`patient`, `symptom`, `physician`, `prescription`, `referral`,
`diagnostic`, `media`, `ai`, `audit`, ...), each with its own entity, repository, service and
controller layers.

Engineering decisions worth calling out:

- **Every external integration sits behind an interface with a fallback.** For prescriptions,
  referrals and lab requisitions, a resolver picks the electronic adapter or the printable fallback
  at runtime from the clinic's settings (`clinic_capabilities`), so a clinic with no integrations
  can still prescribe, refer and order tests. Each AI feature (triage, medication safety, specialty
  suggestion, result summary, chat, voice, photo) is switched separately between OpenAI and a
  deterministic rule-based fallback. The default is the fallback, so the app and its tests run
  with no API keys. The fallback rules lean toward flagging: a false alarm costs a physician a
  second look, a missed one could harm a patient.
- **Electronic lab requisitions are real FHIR.** The requisition is built as a FHIR R4
  `ServiceRequest` with HAPI FHIR and posted to a FHIR server (a local HAPI FHIR sandbox in
  `docker-compose.yml`). If the lab endpoint is down, the order is still recorded and the failure
  is logged instead of blocking the physician.
- **Privacy is enforced in code and in the database.** Health card and insurance numbers are
  encrypted with AES-256-GCM before they are stored. The audit log is append-only, enforced by a
  PostgreSQL trigger, and integration tests check that updates and deletes are rejected. Patient
  free text is passed through a redactor that strips identifiers (health card numbers, phone
  numbers, emails, postal codes) before it is sent to an AI model.
- **Voice and photo processing is asynchronous.** Uploads are stored first (local disk or any
  S3-compatible store such as Supabase Storage), then a background poller transcribes or analyses
  them, with a bounded retry so one timeout from the model does not leave a permanent failure note.
- **Two test lanes.** The default suite is deterministic and free and runs on every push. AI
  quality is measured separately by eval tests that call OpenAI against labelled cases with pass
  thresholds (for example, every emergency case must be classified EMERGENCY). They are tagged
  `eval`, excluded by default and run on demand with `-Pevals`.

### Not built yet

The electronic PrescribeIT and Ocean eReferral adapters and the CPSO credential lookup are
placeholders that record a status without calling the real service (they need vendor onboarding).
The printable fallbacks record a document reference, but PDF rendering is not implemented. Email
notifications are logged instead of sent, and wearable sync uses synthetic data.

## Run it locally

Prerequisites: Java 17+, Docker, Node.js 22 (for the frontend).

```bash
# 1. Start PostgreSQL on localhost:5333 (and a HAPI FHIR sandbox on 8090)
docker compose up -d

# 2. Required: a JWT signing secret of at least 32 bytes (there is no default)
export JWT_SECRET="$(openssl rand -base64 48)"
# Allow the refresh cookie over plain http on localhost
export AUTH_COOKIE_SECURE=false

# 3. Run the backend. Flyway applies the migrations on startup.
#    The local profile seeds a demo clinic and users (see config/DemoSeedRunner.java).
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# 4. Run the frontend dev server (proxies /api to localhost:8080)
cd frontend && npm ci && npm run dev
```

The database defaults match `docker-compose.yml`; override them with `DB_URL`, `DB_USERNAME` and
`DB_PASSWORD`. To use OpenAI instead of the fallbacks, set `OPENAI_API_KEY` and, per feature,
`AI_TRIAGE_ADAPTER=openai`, `AI_SAFETY_ADAPTER=openai` and so on (see
`src/main/resources/application.properties`).

To build one JAR that also serves the frontend: `./mvnw package -Pfrontend`.

## Run the tests

```bash
# Backend: unit tests use H2; the compliance tests need PostgreSQL on localhost:5333
docker compose up -d db
./mvnw verify

# Frontend: type check, unit tests and production build
cd frontend && npm ci && npm run lint && npm test && npm run build
```

CI runs the same commands on every push and pull request to `main`
(see `.github/workflows/ci.yml`).
