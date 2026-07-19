# VaidyaLink Backend

![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.2-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-336791)
![Redis](https://img.shields.io/badge/Redis-Cache-DC382D)
![Maven](https://img.shields.io/badge/Build-Maven-C71A36)
![JWT](https://img.shields.io/badge/Auth-JWT-orange)
![OpenAPI](https://img.shields.io/badge/Docs-Swagger-85EA2D)

Spring Boot **platform API** for VaidyaLink. This service is the system of record for users, doctors, appointments, slots, and chat history. It also orchestrates AI triage by calling (or stubbing) the Python LangGraph microservice — optionally authenticated with a shared `X-API-Key` — then attaching recommended doctors when routing completes.

> Parent: [`../README.md`](../README.md) · Frontend: [`../frontend/README.md`](../frontend/README.md) · AI: [`../ai-triage-service/README.md`](../ai-triage-service/README.md)

## Table of contents

- [What this service owns](#what-this-service-owns)
- [Tech stack](#tech-stack)
- [Architecture](#architecture)
- [Package guide](#package-guide)
- [Security](#security)
- [AI triage integration](#ai-triage-integration)
- [Caching](#caching)
- [Domain model](#domain-model)
- [Business rules](#business-rules)
- [API reference](#api-reference)
- [Sample payloads](#sample-payloads)
- [Error handling](#error-handling)
- [Configuration](#configuration)
- [Run locally](#run-locally)
- [Tests](#tests)
- [Troubleshooting](#troubleshooting)

## What this service owns

| Area | Details |
|---|---|
| Identity | Patient / doctor registration, login, `/api/auth/me` |
| Authorization | JWT + `@PreAuthorize` roles |
| Catalog | Doctors, specialties, Redis-cached lists |
| Scheduling | Slot generation, available slots, booking, status lifecycle |
| Chat | Session create/load, message persistence, triage client call |
| AI boundary | `AiTriageClient` — stub for offline/tests, REST for live Python |

It does **not** run LangGraph or call OpenAI directly. Reasoning lives in `ai-triage-service`.

## Tech stack

| Layer | Choice |
|---|---|
| Runtime | Java 21 |
| Framework | Spring Boot 4.0.2 |
| Web | Spring Web MVC |
| Persistence | Spring Data JPA + PostgreSQL |
| Cache | Spring Cache + Redis |
| Security | Spring Security + JJWT 0.11.5 |
| Validation | Jakarta Bean Validation |
| Docs | Springdoc OpenAPI 2.7 (Swagger UI) |
| Build | Maven Wrapper (`mvnw` / `mvnw.cmd`) |
| Tests | JUnit 5, Mockito, H2 (Redis autoconfig disabled in tests) |

## Architecture

```text
HTTP Controllers
      │
      ▼
  Services  ──► Repositories ──► PostgreSQL
      │
      ├──► Spring Cache ──► Redis
      │
      └──► AiTriageClient
              ├── StubAiTriageClient     (AI_TRIAGE_STUB=true)
              └── RestAiTriageClient     → http://localhost:8000
```

Typical chat request:

1. Controller resolves authenticated patient from JWT
2. `ChatPersistenceHelper` saves the patient message (transaction)
3. Service loads specialty allowlist (cached)
4. Client calls Python triage (or stub)
5. Helper saves AI reply (transaction)
6. If complete and not `Emergency`, service loads doctors by specialty into the response

## Package guide

| Package | Responsibility |
|---|---|
| `controller` | HTTP mapping, principal resolution, thin orchestration |
| `service` | Business rules (booking, chat, doctors, slots) |
| `client` | Outbound AI triage (`AiTriageClient` + stub/REST) |
| `repository` | Spring Data JPA queries |
| `entity` | JPA tables (`Patient`, `Doctor`, `Appointment`, `DoctorSlot`, `ChatSession`, `ChatMessage`, …) |
| `dto` | Request/response contracts (entities are not exposed raw on public APIs) |
| `security` | JWT util, filter, `UserDetailsService`, security filter chain |
| `exception` | `GlobalExceptionHandler` → consistent JSON errors |
| `config` | Security, Redis, OpenAPI, cache, `AiTriageProperties` + AI `RestClient` |

## Security

### Authentication

- `POST /api/auth/login` returns JWT + role (+ id/name enrichment)
- Clients send `Authorization: Bearer <token>`
- Stateless sessions (`SessionCreationPolicy.STATELESS`)
- Passwords hashed with BCrypt

### Authorization

**Public (no JWT)**

- `POST /api/auth/register/patient`
- `POST /api/auth/register/doctor`
- `POST /api/auth/login`
- `/v3/api-docs/**`, `/swagger-ui/**`

**Authenticated** — all other `/api/**` routes. Method-level roles via `@EnableMethodSecurity` + `@PreAuthorize`.

### Chat ownership

- `GET /api/chat/session` and `POST /api/chat/send` require `ROLE_PATIENT`
- Patient id is resolved from the JWT email, not from a client-supplied id
- Send verifies the session belongs to that patient before persisting

### CORS

Allowed origins: `http://localhost:3000`, `http://localhost:5173`  
Methods: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`

## AI triage integration

The backend never calls OpenAI directly. Chat orchestration goes through `AiTriageClient`, which has two implementations selected by configuration.

### Client switch

| Env | Behavior |
|---|---|
| `AI_TRIAGE_STUB=true` (default) | In-process `StubAiTriageClient` — no Python/OpenAI needed; useful for demos and unit tests |
| `AI_TRIAGE_STUB=false` | `RestAiTriageClient` → `AI_TRIAGE_URL` (default `http://localhost:8000`) |

Configuration lives in `AiTriageProperties` (`ai.triage.*`) and `AiTriageConfig`, which builds a dedicated `RestClient` bean when stub mode is off.

| Property | Env / YAML | Default | Purpose |
|---|---|---|---|
| Base URL | `AI_TRIAGE_URL` / `ai.triage.base-url` | `http://localhost:8000` | Python service origin |
| Stub flag | `AI_TRIAGE_STUB` / `ai.triage.stub-enabled` | `true` | Stub vs live REST |
| API key | `AI_TRIAGE_API_KEY` / `ai.triage.api-key` | empty | Sent as `X-API-Key` when non-blank |
| Connect / read timeouts | `ai.triage.connect-timeout` / `read-timeout` | `5s` / `30s` | `RestClient` timeouts |

When `ai.triage.api-key` is set, every outbound triage call includes `X-API-Key`. Use the **same** value as `AI_TRIAGE_API_KEY` in the Python service `.env`.

### Request shape sent to Python

- `sessionId`
- Full message history (`senderType`: `PATIENT` | `AI_BOT`, `messageText`)
- `allowedSpecialties` from `doctorService.getDistinctSpecialities()` (Redis-cached)

### Response handling

| Python outcome | Backend behavior |
|---|---|
| Follow-up / off-topic (`is_complete=false`) | Save AI text; session stays active |
| Specialty route (`is_complete=true`) | Save AI text; mark triage; attach `recommendedDoctors` (skip if specialty is `Emergency`) |
| Emergency | Save AI text; no doctor list |
| Auth failure (`401`) | Surfaces as triage client failure → **503** when REST client is active |
| HTTP / timeout failures | Mapped to **503** when REST client is active |

See the [AI triage README](../ai-triage-service/README.md) for graph nodes (safety, topic guard, structured outputs, API-key auth).

## Caching

Redis-backed Spring Cache (`spring.cache.type=redis`, TTL 10m):

| Cache name | Contents |
|---|---|
| `doctors_page` | Paginated doctor list |
| `distinct_specialities` | Specialty allowlist for triage + `GET /api/doctors/specialties` |

Both are evicted when a new doctor is registered.

Local Redis:

```powershell
cd backend
docker compose up -d
```

(`docker-compose.yml` runs `redis:latest` as `vaidyalink-redis` on port `6379`.)

## Domain model

### Patient
`id`, `name`, `email` (unique), `mobile` (unique), `gender`, `age`, `password` (BCrypt)

### Doctor
`id`, `name`, `email` (unique), `speciality`, `experience`, `password` (BCrypt)

### Appointment
`id`, `patient`, `doctor`, `appointmentDate`, `appointmentTime`, `status`  
Status enum: `PENDING` | `CONFIRMED` | `CANCELLED` | `COMPLETED`

### DoctorSlot
Persisted bookable intervals for a doctor/day (generated from shift window + duration)

### ChatSession / ChatMessage
Patient-owned triage conversation. Messages store `senderType` (`PATIENT` | `AI_BOT`) and text. An embedding column exists for future RAG work and is unused in the current path.

## Business rules

### Registration validation
- Patient: name, valid email, 10-digit mobile, age ≥ 0, password required
- Doctor: name, valid email, speciality, experience ≥ 0, password required

### Booking
- Date must be today or future; same-day past times blocked
- Times normalized to minute precision
- Slot grid / minute boundaries enforced (e.g. `:00`, `:20`, `:40` depending on generation rules)
- Duplicate doctor/date/time blocked unless the prior booking is cancelled
- New bookings start as `PENDING`

### Status updates
- Terminal states `CANCELLED` / `COMPLETED` cannot be updated further

## API reference

### Auth — `/api/auth`

| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| `POST` | `/api/auth/register/patient` | Public | Register patient |
| `POST` | `/api/auth/register/doctor` | Public | Register doctor |
| `POST` | `/api/auth/login` | Public | Login → JWT |
| `GET` | `/api/auth/me` | Authenticated | Current user profile |

### Patients — `/api/patients`

| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| `GET` | `/api/patients/{id}` | `PATIENT`, `DOCTOR` | Get patient |
| `GET` | `/api/patients/all` | `DOCTOR` | List patients |

### Doctors — `/api/doctors`

| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| `GET` | `/api/doctors/specialties` | `PATIENT`, `DOCTOR` | Distinct specialties (cached) |
| `GET` | `/api/doctors/{id}` | `PATIENT`, `DOCTOR` | Get doctor |
| `GET` | `/api/doctors?speciality=…` | `PATIENT`, `DOCTOR` | Filter by specialty |
| `GET` | `/api/doctors/all` | `PATIENT`, `DOCTOR` | List doctors (paged/cached) |
| `POST` | `/api/doctors/{doctorId}/slots/generate` | Authenticated | Generate day slots |

### Appointments — `/api/appointments`

| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| `POST` | `/api/appointments/book` | `PATIENT` | Book appointment |
| `GET` | `/api/appointments/patient/{patientId}` | `PATIENT`, `DOCTOR` | Patient appointments (paged) |
| `GET` | `/api/appointments/patient/{patientId}/upcoming` | `PATIENT` | Upcoming appointments |
| `GET` | `/api/appointments/doctor/{doctorId}` | `DOCTOR` | Doctor appointments (optional `date`) |
| `PATCH` | `/api/appointments/{id}/status?status=` | `PATIENT`, `DOCTOR` | Update status |
| `GET` | `/api/appointments/doctor/{doctorId}/available-slots` | `PATIENT`, `DOCTOR` | Open slots for a date |
| `GET` | `/api/appointments/doctor/{doctorId}/search` | `DOCTOR` | Search by patient name/mobile |

### Chat — `/api/chat`

| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| `GET` | `/api/chat/session` | `PATIENT` | Get or create triage session + history |
| `POST` | `/api/chat/send` | `PATIENT` | Send message → AI reply (+ doctors when complete) |

## Sample payloads

### Register patient

```json
{
  "name": "Rahul Sharma",
  "email": "rahul@example.com",
  "mobile": "9876543210",
  "gender": "Male",
  "age": 27,
  "password": "rahul@123"
}
```

### Login

```json
{
  "email": "rahul@example.com",
  "password": "rahul@123"
}
```

### Send chat message

```json
{
  "sessionId": 1,
  "messageText": "I have had a mild headache for two days, no fever."
}
```

Example response when triage completes:

```json
{
  "aiReply": "Based on your symptoms, I recommend seeing a General Physician. You can book an appointment now.",
  "triageComplete": true,
  "recommendedSpecialty": "General Physician",
  "recommendedDoctors": [
    {
      "id": 2,
      "name": "Dr. Mehta",
      "email": "mehta@example.com",
      "speciality": "General Physician",
      "experience": 10
    }
  ]
}
```

### Book appointment

```json
{
  "patientId": 5,
  "doctorId": 2,
  "appointmentDate": "2026-07-20",
  "appointmentTime": "10:20:00"
}
```

### Generate slots

```json
{
  "date": "2026-07-20",
  "shiftStartTime": "09:00:00",
  "shiftEndTime": "13:00:00",
  "durationInMinutes": 20
}
```

## Error handling

`GlobalExceptionHandler` maps exceptions to consistent JSON:

| Exception | Status |
|---|---|
| `MethodArgumentNotValidException` | `400` |
| `IllegalStateException` / type mismatch | `400` |
| `AccessDeniedException` | `403` |
| `EntityNotFoundException` | `404` |
| `HttpRequestMethodNotSupportedException` | `405` |
| `DataIntegrityViolationException` | `409` |
| AI triage client failures | `503` (REST client active) |
| Unhandled `Exception` | `500` |

## Configuration

### Required environment variables

| Variable | Purpose |
|---|---|
| `DB_URL` | JDBC URL, e.g. `jdbc:postgresql://localhost:5432/vaidyalink` |
| `DB_USERNAME` | DB user |
| `DB_PASSWORD` | DB password |
| `JWT_SECRET` | Base64-encoded HMAC secret |

### Optional

| Variable | Default | Purpose |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` | Active profile |
| `PORT` | `8080` | HTTP port |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Cache |
| `REDIS_PASSWORD` / `REDIS_SSL` | empty / `false` | Redis auth / TLS |
| `AI_TRIAGE_URL` | `http://localhost:8000` | Python service |
| `AI_TRIAGE_STUB` | `true` | Stub vs REST client |
| `AI_TRIAGE_API_KEY` | empty | Shared secret sent as `X-API-Key` to Python (set the same value in the AI service `.env`) |

Secrets belong in environment variables or a **gitignored** local profile (`application-dev.yml`). Do not commit credentials.

## Run locally

### Prerequisites

- Java 21
- PostgreSQL with a `vaidyalink` database
- Redis (`docker compose up -d` from this folder)

### Windows PowerShell

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/vaidyalink"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your_password_here"
$env:JWT_SECRET="your_base64_secret"
$env:AI_TRIAGE_STUB="true"
.\mvnw.cmd spring-boot:run
```

### Linux / macOS

```bash
export DB_URL="jdbc:postgresql://localhost:5432/vaidyalink"
export DB_USERNAME="postgres"
export DB_PASSWORD="your_password_here"
export JWT_SECRET="your_base64_secret"
export AI_TRIAGE_STUB="true"
./mvnw spring-boot:run
```

### OpenAPI

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

### Live AI mode

1. Start `ai-triage-service` on `:8000` with `AI_TRIAGE_API_KEY` set in its `.env`
2. Set the **same** key on the backend: `AI_TRIAGE_API_KEY`, plus `AI_TRIAGE_STUB=false` (and optionally `AI_TRIAGE_URL`)
3. Restart this backend — `RestClient` sends `X-API-Key` on every triage call

## Tests

```powershell
.\mvnw.cmd test
```

Coverage includes services, stub AI client, chat orchestration (specialty allowlist wiring), and doctor specialty queries. Integration-style tests use H2 and disable Redis autoconfiguration via `src/test/resources/application-test.yml`.

Still light: dedicated tests for `RestAiTriageClient` (WireMock / MockWebServer) and controller-level security integration tests.

## Troubleshooting

| Symptom | Likely fix |
|---|---|
| Auth `403` | Check Bearer token and `@PreAuthorize` role |
| Redis / cache errors | Ensure Redis is running on `6379` |
| AI `503` | If stub is off, confirm Python is up at `AI_TRIAGE_URL`, and that both sides share the same `AI_TRIAGE_API_KEY` (or both leave it empty) |
| AI key mismatch | Python returns `401`; backend surfaces that as a triage failure (`503` to the patient-facing API) |
| Schema drift after entity changes | Hibernate `ddl-auto=update` helps locally; use intentional migrations for production |
| Chat returns off-topic loop after jailbreak | Fixed in AI service (latest-message topic guard); update/redeploy Python service |
