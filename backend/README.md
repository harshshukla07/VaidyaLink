# VaidyaLink Backend

![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.2-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-336791)
![Redis](https://img.shields.io/badge/Redis-Cache-DC382D)
![Maven](https://img.shields.io/badge/Build-Maven-C71A36)
![JWT](https://img.shields.io/badge/Auth-JWT-orange)

Spring Boot platform API for VaidyaLink: authentication, doctor discovery, appointment booking, slot management, and AI triage chat orchestration.

> Parent overview: [`../README.md`](../README.md) · AI service: [`../ai-triage-service/README.md`](../ai-triage-service/README.md)

## Table of contents

- [Scope](#scope)
- [Tech stack](#tech-stack)
- [Architecture](#architecture)
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

## Scope

- JWT auth with `ROLE_PATIENT` / `ROLE_DOCTOR`
- Patient and doctor registration + `/api/auth/me`
- Doctor listing, specialty filter, distinct specialties (cached)
- Appointment booking, status lifecycle, search, upcoming list
- Doctor shift slot generation + available-slot queries
- Patient AI chat: session create/load, send message, triage orchestration
- Pluggable AI client (`stub` for tests / offline, `REST` for the Python service)

## Tech stack

| Layer | Choice |
|---|---|
| Runtime | Java 21 |
| Framework | Spring Boot 4.0.2 |
| Web | Spring Web MVC |
| Persistence | Spring Data JPA + PostgreSQL |
| Cache | Spring Cache + Redis |
| Security | Spring Security + JJWT |
| Validation | Jakarta Bean Validation |
| Docs | Springdoc OpenAPI (Swagger UI) |
| Build | Maven Wrapper |
| Tests | JUnit 5, Mockito, H2 |

## Architecture

```text
Controller  →  Service  →  Repository  →  PostgreSQL
                 │
                 ├─→ Redis (Spring Cache)
                 └─→ AiTriageClient
                        ├─ StubAiTriageClient   (AI_TRIAGE_STUB=true)
                        └─ RestAiTriageClient   → Python :8000
```

Notable packages:

| Package | Responsibility |
|---|---|
| `controller` | HTTP surface, auth principal resolution |
| `service` | Business rules and orchestration |
| `client` | Outbound AI triage HTTP / stub |
| `repository` | Spring Data JPA |
| `entity` | Relational model |
| `dto` | Request/response contracts (no entity leakage on public APIs) |
| `security` | JWT filter, user details, method security |
| `exception` | Centralized JSON error responses |
| `config` | Security, Redis, RestClient beans |

## Security

### Authentication

- Login returns a JWT + role
- Clients send `Authorization: Bearer <token>`
- Stateless sessions (`SessionCreationPolicy.STATELESS`)
- Passwords stored with BCrypt

### Authorization

**Public**

- `/api/auth/register/**`, `/api/auth/login`
- `/v3/api-docs/**`, `/swagger-ui/**`

**Authenticated** — all other `/api/**` routes, with method-level `@PreAuthorize` where needed.

Chat endpoints resolve the patient from the JWT email (not from a client-supplied id), then enforce session ownership on send.

### CORS

Allowed origins: `http://localhost:3000`, `http://localhost:5173`  
Methods: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`

## AI triage integration

Chat flow on `POST /api/chat/send`:

1. Verify the session belongs to the authenticated patient
2. Persist the patient message (+ load history)
3. Load distinct specialties from DB (Redis-cached)
4. Call `AiTriageClient.triage(...)` with messages + `allowedSpecialties`
5. Persist the AI reply and session status
6. If triage is complete (and not `Emergency`), attach matching `recommendedDoctors`

Config (`application.yml`):

| Property / env | Default | Meaning |
|---|---|---|
| `ai.triage.base-url` / `AI_TRIAGE_URL` | `http://localhost:8000` | Python service URL |
| `ai.triage.stub-enabled` / `AI_TRIAGE_STUB` | `true` | Use in-process stub (no Python/OpenAI) |
| connect / read timeouts | `5s` / `30s` | RestClient timeouts |

## Caching

Redis-backed Spring Cache (`spring.cache.type=redis`, TTL 10m):

| Cache name | Used for |
|---|---|
| `doctors_page` | Paginated doctor list |
| `distinct_specialities` | Specialty allowlist for triage + API |

Both are evicted when a new doctor is registered.

Local Redis via Docker:

```bash
cd backend
docker compose up -d
```

## Domain model

### Patient
`id`, `name`, `email` (unique), `mobile` (unique), `gender`, `age`, `password` (BCrypt)

### Doctor
`id`, `name`, `email` (unique), `speciality`, `experience`, `password` (BCrypt)

### Appointment
`id`, `patient`, `doctor`, `appointmentDate`, `appointmentTime`, `status` (`PENDING` \| `CONFIRMED` \| `CANCELLED` \| `COMPLETED`)

### DoctorSlot
Persisted bookable slots for a doctor/day (generated from shift window + duration)

### ChatSession / ChatMessage
Patient-owned triage conversation; messages store sender type + text (embedding column reserved for future RAG)

## Business rules

### Registration validation
- Patient: name, valid email, 10-digit mobile, age ≥ 0, password required
- Doctor: name, valid email, speciality, experience ≥ 0, password required

### Booking
- Date must be today or future; same-day past times blocked
- Times normalized to minute precision
- Slot minutes constrained to `:00`, `:20`, `:40` (or generated slot grid)
- Duplicate doctor/date/time blocked unless prior booking is cancelled
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
| `GET` | `/api/doctors/specialties` | `PATIENT`, `DOCTOR` | Distinct specialties |
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
  "sessionId": 1,
  "aiReply": "Based on your symptoms, a General Physician is a good next step.",
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
| AI triage client failures | `503` (when Rest client is active) |
| Unhandled `Exception` | `500` |

## Configuration

Required environment variables:

| Variable | Purpose |
|---|---|
| `DB_URL` | JDBC URL, e.g. `jdbc:postgresql://localhost:5432/vaidyalink` |
| `DB_USERNAME` | DB user |
| `DB_PASSWORD` | DB password |
| `JWT_SECRET` | Base64-encoded HMAC secret |

Optional:

| Variable | Default | Purpose |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` | Active profile |
| `PORT` | `8080` | HTTP port |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Cache |
| `AI_TRIAGE_URL` | `http://localhost:8000` | Python service |
| `AI_TRIAGE_STUB` | `true` | Stub vs REST client |

Secrets belong in environment variables or a gitignored local profile (`application-dev.yml`). Do not commit credentials.

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

## Tests

```powershell
.\mvnw.cmd test
```

Unit tests cover services, stub AI client, and chat orchestration (including specialty allowlist wiring). Integration tests use H2 and disable Redis autoconfig via `application-test.yml`.

## Troubleshooting

- **Auth 403** — confirm Bearer token and `@PreAuthorize` role on the endpoint
- **Cache misses / Redis errors** — ensure Redis is running on `6379`
- **AI 503** — if `AI_TRIAGE_STUB=false`, confirm the Python service is up at `AI_TRIAGE_URL`
- **Schema drift** — Hibernate `ddl-auto=update` helps locally; keep migrations intentional for production
