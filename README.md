# MedSimple

AI-coordinated patient intake, triage support, and care orchestration built on top of Canada's existing healthcare systems.

---

## What It Does

MedSimple is a B2B digital health platform that coordinates the journey between patients, physicians, pharmacies, and diagnostic centres. It is an orchestration layer — not a replacement for existing clinic software — that solves three measurable failures in Canadian primary care:

- **The waiting gap** — symptoms go uncaptured between occurrence and appointment
- **The memory gap** — patients reconstruct symptoms from memory in a 10-minute visit
- **The prioritization gap** — physician queues are ordered by booking time, not clinical urgency

Patients submit symptoms as they occur. An AI engine consolidates them, assigns urgency, and surfaces a structured clinical summary to the assigned physician. The platform then manages the full coordination loop: prescriptions, diagnostic requisitions, referrals, results, and follow-up — until the episode of care is closed.

---

## Architecture

**Package-by-feature with N-tier layers.** Each domain module (clinic, patient, symptom, careplan, prescription, diagnostic, referral, notification, audit, ai) is self-contained with its own entity, DTO, repository, service, and controller. Services communicate via dependency injection. DAO interfaces are used wherever multiple implementations exist — real adapter and fallback adapter implementing the same interface.

**Fallback-first design.** Every external integration ships with a built-in fallback that works with zero third-party connectivity:

| Integration | Real adapter | Fallback |
|---|---|---|
| PrescribeIT | Electronic e-prescribing | Signed PDF for fax or hand delivery |
| Ocean eReferral | Electronic referral submission | PDF referral letter |
| Lab networks | Electronic requisition + booking | Printable requisition form |
| Pharmacy ordering | Electronic order dispatch | Patient walks in with printed prescription |

A per-clinic capability registry (`clinic_capabilities` table) resolves which implementation to use at runtime. A clinic with no integrations can adopt MedSimple on day one.

**FHIR R4 data model.** Clinical entities are modelled on HL7 FHIR R4:

| Table | FHIR resource |
|---|---|
| `symptom_episodes` | EpisodeOfCare |
| `symptom_submissions` | Observation |
| `care_plans` | CarePlan |
| `prescriptions` | MedicationRequest |
| `referrals` | ServiceRequest (intent = referral) |
| `diagnostic_requisitions` | ServiceRequest |
| `diagnostic_results` | DiagnosticReport |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 4.x |
| AI integration | Spring AI |
| Clinical data modelling | HAPI FHIR R4 |
| Database | PostgreSQL 16 |
| Schema migrations | Flyway |
| Testing | JUnit 5, Mockito |
| Frontend | React (built output served from `src/main/resources/static`) |
| Local dev database | Docker Compose (PostgreSQL 16) |
| Staging / production database | Supabase (PostgreSQL, Storage, Realtime) |

---

## Project Structure

```
src/
├── main/
│   ├── java/com/medsimple/
│   │   ├── clinic/
│   │   │   ├── Clinic.java
│   │   │   ├── ClinicCapability.java
│   │   │   ├── ClinicDto.java
│   │   │   ├── ClinicRepository.java
│   │   │   ├── ClinicService.java
│   │   │   ├── ClinicController.java
│   │   │   └── exception/
│   │   ├── patient/
│   │   │   ├── Patient.java
│   │   │   ├── PatientProfile.java
│   │   │   ├── PatientHealthCoverage.java
│   │   │   ├── PatientClinicRegistration.java
│   │   │   ├── PatientConsent.java
│   │   │   ├── PatientDto.java
│   │   │   ├── PatientRepository.java
│   │   │   ├── PatientService.java
│   │   │   ├── PatientController.java
│   │   │   └── exception/
│   │   ├── symptom/
│   │   │   ├── SymptomEpisode.java
│   │   │   ├── SymptomSubmission.java
│   │   │   ├── SymptomDto.java
│   │   │   ├── SymptomEpisodeRepository.java
│   │   │   ├── SymptomSubmissionRepository.java
│   │   │   ├── SymptomService.java
│   │   │   ├── SymptomController.java
│   │   │   └── exception/
│   │   ├── careplan/
│   │   │   ├── CarePlan.java
│   │   │   ├── FollowUp.java
│   │   │   ├── CarePlanDto.java
│   │   │   ├── CarePlanRepository.java
│   │   │   ├── FollowUpRepository.java
│   │   │   ├── CarePlanService.java
│   │   │   ├── CarePlanController.java
│   │   │   └── exception/
│   │   ├── prescription/
│   │   │   ├── Prescription.java
│   │   │   ├── Pharmacy.java
│   │   │   ├── PrescriptionDto.java
│   │   │   ├── PrescriptionRepository.java
│   │   │   ├── PharmacyRepository.java
│   │   │   ├── PrescriptionDeliveryDao.java      (interface)
│   │   │   ├── PrescribItDeliveryService.java
│   │   │   ├── PdfPrescriptionDeliveryService.java
│   │   │   ├── PrescriptionService.java
│   │   │   ├── PrescriptionController.java
│   │   │   └── exception/
│   │   ├── diagnostic/
│   │   │   ├── DiagnosticRequisition.java
│   │   │   ├── DiagnosticResult.java
│   │   │   ├── DiagnosticCentre.java
│   │   │   ├── DiagnosticDto.java
│   │   │   ├── DiagnosticRequisitionRepository.java
│   │   │   ├── DiagnosticResultRepository.java
│   │   │   ├── DiagnosticCentreRepository.java
│   │   │   ├── DiagnosticDeliveryDao.java         (interface)
│   │   │   ├── ElectronicRequisitionService.java
│   │   │   ├── PrintableRequisitionService.java
│   │   │   ├── DiagnosticService.java
│   │   │   ├── DiagnosticController.java
│   │   │   └── exception/
│   │   ├── referral/
│   │   │   ├── Referral.java
│   │   │   ├── ReferralDto.java
│   │   │   ├── ReferralRepository.java
│   │   │   ├── ReferralDeliveryDao.java            (interface)
│   │   │   ├── OceanReferralService.java
│   │   │   ├── PdfReferralService.java
│   │   │   ├── ReferralService.java
│   │   │   ├── ReferralController.java
│   │   │   └── exception/
│   │   ├── ai/
│   │   │   ├── AiService.java
│   │   │   ├── UrgencyAssessmentService.java
│   │   │   ├── ClinicalSummaryService.java
│   │   │   └── SafetyCheckService.java
│   │   ├── notification/
│   │   │   ├── Notification.java
│   │   │   ├── NotificationDto.java
│   │   │   ├── NotificationRepository.java
│   │   │   ├── NotificationDao.java               (interface)
│   │   │   ├── EmailNotificationService.java
│   │   │   ├── InAppNotificationService.java
│   │   │   ├── NotificationService.java
│   │   │   └── exception/
│   │   ├── audit/
│   │   │   ├── AuditLog.java
│   │   │   ├── AuditLogRepository.java
│   │   │   └── AuditService.java
│   │   ├── auth/                                  (implemented in later PR)
│   │   ├── config/
│   │   └── shared/
│   └── resources/
│       ├── application.properties
│       ├── db/
│       │   └── migration/
│       │       └── V1__init_schema.sql
│       ├── static/                                (React production build)
│       └── templates/
└── test/
    └── java/com/medsimple/                        (mirrors main structure)
```

---

## Local Development

### Prerequisites

- Java 17+
- Docker and Docker Compose
- Maven (or use `./mvnw`)

### Setup

**1. Start PostgreSQL:**

```bash
docker-compose up -d
```

This starts `medsimple-db` (PostgreSQL 16) mapped to host port `5333`.

**2. Set environment variables:**

```bash
export DB_URL=jdbc:postgresql://localhost:5333/medsimple
export DB_USERNAME=medsimple
export DB_PASSWORD=password
```

**3. Run the application:**

Flyway migrations apply automatically on startup.

```bash
./mvnw spring-boot:run
```

### Build and Test

```bash
# Compile
./mvnw package -DskipTests

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=SymptomServiceTest

# Run a single test method
./mvnw test -Dtest=SymptomServiceTest#shouldDispatchEmergencyAlert
```

---

## Database

Flyway manages all schema changes. Files live in `src/main/resources/db/migration/` with naming convention `V{version}__{description}.sql`.

Migrations apply automatically at startup. **Never modify a committed migration** — always add a new versioned file.

Spring JPA is configured with `ddl-auto=none`. Flyway is the sole owner of schema state — Hibernate never creates or validates tables.

### Tables

| Table | Purpose |
|---|---|
| `clinics` | Tenant root — one row per clinic customer |
| `clinic_capabilities` | Per-clinic adapter registry (PrescribeIT, Ocean, etc.) |
| `users` | All actors: patients, physicians, nurses, admins |
| `clinic_staff` | Maps physician/staff users to clinic with role and license |
| `pharmacies` | Pharmacy directory with electronic-Rx flag |
| `diagnostic_centres` | Diagnostic centre directory with electronic-booking flag |
| `patient_profiles` | DOB, allergies, current medications, preferred pharmacy |
| `patient_health_coverage` | Provincial health cards and private insurance (encrypted, supports multiples) |
| `patient_clinic_registrations` | Patient-to-clinic relationship; holds assigned physician for non-walk-in patients |
| `patient_consents` | Versioned consent records — immutable append-only |
| `symptom_episodes` | One episode per complaint; AI urgency, summary, and physician confirmation |
| `symptom_submissions` | Individual entries (text/voice/photo) accumulating into an episode |
| `care_plans` | Physician's response plan; owns all prescriptions, requisitions, referrals |
| `follow_ups` | Physician-patient messages and AI-monitoring-generated prompts |
| `prescriptions` | FHIR MedicationRequest — dual delivery path |
| `referrals` | AI-assisted specialty referral — dual delivery path |
| `diagnostic_requisitions` | FHIR ServiceRequest — dual delivery path |
| `diagnostic_results` | FHIR DiagnosticReport — released to patient after physician interpretation |
| `notifications` | Email and in-app alerts with delivery and read tracking |
| `audit_logs` | Immutable PHIPA-required trail of every PHI access and state change |

---

## Feature Set

### Patient

- Submit symptoms at the moment they occur: free text, voice note, or photograph of a visible symptom
- Voice notes transcribed automatically; photos analyzed by AI
- Submissions timestamped and accumulated into a single evolving symptom record per episode
- Emergency pathway: when AI detects red-flag patterns, patient receives immediate 911 instruction before any queueing occurs
- Choose prescription fulfilment: walk into any pharmacy with a printed document, or send via app to a chosen pharmacy
- Choose diagnostic path: print requisition and walk in, or send via app for the centre to book the appointment
- Appointment reminders delivered by email and in-app alert
- Access own test results alongside physician's interpretation
- Follow-up communication with physician through the app
- AI-generated prompts to report symptom updates during ongoing care

### Physician

- Live triage queue sorted by AI-assessed urgency (not booking time); sickest patient always at the top
- AI clinical summary per patient: chief complaint, key symptoms, duration, and relevant context — readable in under 30 seconds
- Original patient notes and recordings accessible with one tap
- One-click urgency override with mandatory reason; every override logged for regulatory validation evidence
- Prescription authoring within the platform; AI performs drug-interaction checks, allergy flags, and dose-range verification before signing
- AI-assisted referral triage: specialty suggested from consolidated clinical summary; physician confirms or amends before submission
- Integration with Ocean eReferral (Ontario) with PDF fallback
- Trigger diagnostic testing within the care plan; platform manages requisition, booking, reminders, and result return
- Follow-up messages to patients; AI monitors care plan and escalates if patient reports deterioration or misses a critical step
- Results returned with AI structured summary; physician adds final clinical interpretation
- Results released to patient with physician interpretation layered on top

### AI

- Urgency assessment: ROUTINE / SEE SOON / URGENT / EMERGENCY
- Red-flag detection: exertional chest pain, stroke signs, anaphylaxis, severe breathing difficulty — triggers immediate emergency pathway
- Clinical summary consolidation: multiple submissions and voice notes compressed into a 30-second physician read
- Specialty routing suggestion for referrals, drawn from consolidated clinical picture
- Prescription safety checks: drug interactions, allergy flags, dose-range verification
- Care plan monitoring: medication pickup confirmation tracking, symptom-update prompts, appointment reminders, deterioration escalation
- Diagnostic result structuring and summary for physician review

---

## Compliance

Designed for PHIPA (Ontario) and PIPEDA compliance from the first migration:

- **Canadian data residency** — all PHI hosted on Canadian infrastructure (Supabase CA regions)
- **Encryption** — health card and insurance numbers encrypted with AES-256 at the application layer before storage; TLS in transit
- **Role-based access control** — enforced at the API layer; clinic staff only see their clinic's data
- **Full audit trail** — every PHI access and state change written to `audit_logs`; rows are immutable and append-only
- **Versioned consent** — every patient consent event recorded with type, version, timestamp, and IP in `patient_consents`; rows are immutable
- **AI de-identification** — PHI stripped or tokenized before any data is sent to AI model providers
- **Clinician-in-the-loop** — every AI urgency assessment requires physician confirmation or override; override logging builds the validation evidence base for future regulatory submissions

Health Canada classification: the platform launches as a clinical communication and documentation tool. All clinical decisions (urgency confirmation, referral, prescription, result interpretation) are made by a physician. AI functions requiring device classification (autonomous triage, AI-drafted prescriptions) are Phase 3, pursued through the proper licensing pathway.

---

## Branch and PR Strategy

One feature per branch. Branch naming: `feature/<short-description>`.

| PR | Branch | Scope |
|---|---|---|
| 1 | `feature/db-schema` | Flyway V1 initial schema, pom.xml foundation |
| 2 | `feature/patient-module` | Patient entity, profile, health coverage, clinic registration |
| 3 | `feature/clinic-module` | Clinic entity, capabilities, staff management |
| 4 | `feature/symptom-capture` | Symptom episode and submission API (text input) |
| 5 | `feature/ai-triage` | Spring AI urgency assessment and clinical summary |
| 6 | `feature/physician-queue` | Triage queue endpoint, urgency override |
| 7 | `feature/voice-photo-intake` | Voice transcription, photo upload, async processing |
| 8 | `feature/care-plan` | Care plan creation, follow-up messaging |
| 9 | `feature/prescription-workflow` | Prescription authoring, AI safety checks, PDF fallback |
| 10 | `feature/referral-workflow` | Referral creation, specialty suggestion, PDF fallback |
| 11 | `feature/diagnostic-workflow` | Requisition, booking, result loop |
| 12 | `feature/notifications` | Email and in-app dual-channel delivery |
| 13 | `feature/auth` | Spring Security, JWT, token management |
| 14 | `feature/react-frontend` | React app wired to REST API |

---

## Roadmap

Phase 0 is complete and verified end to end. Three phases remain. The codebase already
over-delivers on parts of Phases 1–2 — voice + photo intake is live, and the real-adapter seams
for PrescribeIT, Ocean eReferral, and lab booking are scaffolded behind their fallbacks — so the
remaining work is increasingly operational and regulatory rather than greenfield engineering.

| Phase | Timeframe | Focus | Status |
|---|---|---|---|
| 0 — POC | — | Proven core loop and architecture | ✅ Complete |
| 1 — Pilot | Months 1–6 | Production build, real clinics on fallback paths | 🚧 In progress |
| 2 — Integration | Months 6–18 | Live national/provincial rails behind the same workflows | ◻️ Planned |
| 3 — Intelligence | Months 18+ | Elevated AI through the medical-device licensing pathway | ◻️ Future |

### Phase 1 — Pilot (months 1–6)

Real-world validation on the existing fallback-first build, with no integration prerequisites.

- Production build on the Java/FHIR stack; PHIPA-compliant, Canadian-resident hosting.
- Secrets and config moved to a managed secret store; security assessment before pilot.
- Voice transcription and photo intake live. ✅ _(shipped in the POC)_
- Onboard 2–3 pilot clinics (walk-in networks first) on fallback paths only.
- Surface the physician-override and outcome instrumentation as a dataset / report.

**Outcome:** real-world validation data, the physician-override dataset, and clinic testimonials.

### Phase 2 — Integration (months 6–18)

Same workflows, faster rails. Each integration is an adapter swap, not a re-architecture — the
seams already exist behind every fallback.

- PrescribeIT vendor onboarding (e-prescribing).
- Ocean eReferral (Ontario) for electronic referrals.
- First laboratory booking + results integration.
- EMR connectivity via FHIR, read/write where the clinic's system allows.
- Follow-up and reminder automation at scale.
- **Structured clarification / eConsult** — a scoped, audited message thread on a referral or
  requisition so a diagnostic centre or specialist can ask a question (or send advice) without a
  full visit, with the AI summarizing the thread. Maps onto Ontario eConsult and Ocean messaging.

**Outcome:** the same workflows on faster rails, plus a defensible integration moat.

### Phase 3 — Intelligence (months 18+)

The full original vision, achieved through the approval pathway rather than around it.

- Pursue Health Canada medical-device licensing for elevated AI functions.
- Autonomous triage assistance and AI-drafted prescriptions under physician review.
- Backed by the validation evidence (override logs, outcomes) accumulated from Phase 1 onward.

**Outcome:** the full original vision, licensed and defensible.
