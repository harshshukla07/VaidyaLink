# VaidyaLink

![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.2-brightgreen)
![Python](https://img.shields.io/badge/Python-3.12+-3776AB)
![FastAPI](https://img.shields.io/badge/FastAPI-0.115-009688)
![LangGraph](https://img.shields.io/badge/LangGraph-AI%20Triage-1C3C3C)
![React](https://img.shields.io/badge/React-19-61DAFB)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-336791)
![Redis](https://img.shields.io/badge/Redis-Cache-DC382D)
![Security](https://img.shields.io/badge/Auth-JWT-orange)

**VaidyaLink** is an AI-assisted healthcare appointment platform. Patients describe symptoms in a guided chat, get routed to a clinic specialty that actually exists in the roster, pick a doctor, and book a conflict-checked slot. Doctors manage schedules and appointment status through a separate workspace.

The system is deliberately split into three deployable pieces:

| Piece | Role |
|---|---|
| **Frontend** (`frontend/`) | React SPA for patients and doctors |
| **Backend** (`backend/`) | Spring Boot platform API — auth, persistence, booking, chat orchestration |
| **AI triage** (`ai-triage-service/`) | FastAPI + LangGraph microservice — symptom reasoning only |

---

## Why this project exists

Most “doctor booking” demos stop at CRUD. VaidyaLink models a clinic-shaped journey with real constraints:

1. **Authenticate** as a patient or doctor (JWT + role-based access)
2. **Triage** symptoms through a conversational AI flow  
   `safety → topic guard → completeness → follow-up | specialty`
3. **Discover** doctors for the recommended specialty (and only specialties that exist in the DB)
4. **Book** a slot with conflict checks and a status lifecycle  
   `PENDING → CONFIRMED / CANCELLED / COMPLETED`

Design principle: the AI service is **stateless**. Java owns identity, chat history, and specialty truth. Python owns reasoning. That keeps LLM experiments from contaminating the transactional core, and lets the backend run with a stub client when OpenAI or Python is unavailable.

---

## End-to-end user journeys

### Patient

1. Register / login → land on the care home
2. Open **Symptom triage** chat → answer follow-ups until a specialty is recommended
3. See **recommended doctors** for that specialty
4. Open a doctor → pick an available slot → book
5. Track or cancel appointments from **My appointments**

### Doctor

1. Login → view schedule and update appointment status
2. Generate day slots (shift start/end + duration) so patients can book

---

## Architecture

```text
┌─────────────────┐      JWT       ┌────────────────────────────────┐
│  React Frontend │ ──────────────▶│  Spring Boot Backend (:8080)   │
│  Vite :5173     │                │  Auth · Chat · Doctors ·       │
└─────────────────┘                │  Appointments · Slots · Cache  │
                                   └───────────────┬────────────────┘
                                           │       │
                                PostgreSQL │       │ Redis (Spring Cache)
                                           │       │
                                           │       ▼
                                           │  distinct specialties
                                           │  (allowlist for AI)
                                           │
                                           ▼  HTTP (RestClient)
                                   ┌────────────────────────────────┐
                                   │  AI Triage Service (:8000)     │
                                   │  FastAPI + LangGraph           │
                                   │  OpenAI gpt-4o-mini            │
                                   └────────────────────────────────┘
```

| Concern | Owner | Why |
|---|---|---|
| Auth, patients, doctors, appointments | Java | Transactional source of truth |
| Chat sessions & message history | Java | Persistence + ownership checks |
| Specialty allowlist | Java (DB + Redis cache) | AI must not invent specialties |
| Symptom reasoning & routing | Python | Isolated LLM graph, easy to iterate |
| UI | React | Thin client over JWT APIs |

### Chat orchestration (happy path)

1. Patient sends a message (`POST /api/chat/send`)
2. Backend verifies session ownership, saves the patient message
3. Backend loads distinct specialties (cached) and builds `TriageRequest`
4. `AiTriageClient` calls Python (`RestAiTriageClient`) or returns stub data
5. Backend saves the AI reply; if triage is complete (and not `Emergency`), loads matching doctors into `ChatResponse.recommendedDoctors`

---

## Repository layout

```text
VaidyaLink/
├── README.md                      ← monorepo overview (this file)
├── .gitignore                     ← secrets, venv, target, .env, etc.
│
├── frontend/                      ← React 19 + Vite + TypeScript SPA
│   ├── README.md
│   ├── package.json
│   └── src/
│       ├── api/                   ← fetch client + types
│       ├── pages/                 ← patient & doctor screens
│       └── components/            ← shell, guards, shared UI
│
├── backend/                       ← Spring Boot 4 / Java 21 API
│   ├── README.md
│   ├── docker-compose.yml         ← Redis for local cache
│   ├── pom.xml
│   └── src/main/java/.../backend/
│       ├── controller/
│       ├── service/
│       ├── client/                ← AiTriageClient (stub + REST)
│       ├── security/
│       ├── entity/ · dto/ · repository/
│       └── exception/
│
└── ai-triage-service/             ← FastAPI + LangGraph microservice
    ├── README.md
    ├── requirements.txt
    ├── tests/                     ← pytest (mocked LLM)
    └── app/
        ├── main.py
        ├── errors.py
        ├── graph/                 ← nodes, builder, constants
        ├── llm/
        └── schemas/
```

---

## Key capabilities

### Frontend
- Public landing, login, and registration (patient / doctor)
- Patient: home, AI triage chat, doctor browse, booking, appointments
- Doctor: schedule view, slot generation
- JWT stored client-side; role-gated routes via `ProtectedRoute`
- Configurable API base URL (`VITE_API_BASE_URL`)

### Backend
- JWT authentication with `ROLE_PATIENT` / `ROLE_DOCTOR`
- Bean Validation on request DTOs; centralized JSON error handling
- Appointment booking rules (date/time, conflicts, status transitions)
- Doctor slot generation and available-slot queries
- Redis-backed caches for doctor pages and distinct specialties
- Pluggable AI client: stub (default) or REST to Python
- OpenAPI / Swagger UI for interactive exploration

### AI triage
- LangGraph: safety → topic guard → assess → follow-up **or** specialty route
- Word-boundary emergency phrase matching (no LLM)
- Jailbreak / role-override short-circuit on the **latest** patient message
- Full conversation context (`PATIENT` + `AI_BOT`) to avoid repeated questions
- Structured LLM outputs via Pydantic (`with_structured_output`)
- Allowlist + General Physician fallback
- HTTP `400` / `503` for validation and LLM failures
- pytest suite with mocked OpenAI

---

## Quick start (full local stack)

### Prerequisites

- Java 21
- Maven Wrapper (included under `backend/`)
- PostgreSQL with a database named `vaidyalink`
- Docker (for Redis) or a local Redis on `6379`
- Python 3.12+ and an OpenAI API key (only if using live triage)
- Node.js 18+ (for the frontend)

### 1. Redis

```powershell
cd backend
docker compose up -d
```

### 2. Backend (`:8080`)

```powershell
cd backend
$env:DB_URL="jdbc:postgresql://localhost:5432/vaidyalink"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your_password"
$env:JWT_SECRET="your_base64_secret"
$env:AI_TRIAGE_STUB="true"   # flip to false when Python is running
.\mvnw.cmd spring-boot:run
```

- Swagger UI: http://localhost:8080/swagger-ui/index.html  
- OpenAPI JSON: http://localhost:8080/v3/api-docs  

### 3. AI triage (`:8000`) — optional if stub is on

```powershell
cd ai-triage-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
copy .env.example .env
# set OPENAI_API_KEY in .env
uvicorn app.main:app --reload --port 8000
```

Then restart the backend with:

```powershell
$env:AI_TRIAGE_STUB="false"
$env:AI_TRIAGE_URL="http://localhost:8000"
```

### 4. Frontend (`:5173`)

```powershell
cd frontend
npm install
npm run dev
```

Open http://localhost:5173  

Optional `.env` in `frontend/`:

```env
VITE_API_BASE_URL=http://localhost:8080
```

---

## Documentation map

| Document | Audience | Contents |
|---|---|---|
| [`frontend/README.md`](frontend/README.md) | UI / fullstack | Routes, auth flow, env, scripts |
| [`backend/README.md`](backend/README.md) | Backend | Security, domain model, full API matrix, booking rules, AI client, config |
| [`ai-triage-service/README.md`](ai-triage-service/README.md) | AI / ML eng | Graph design, schemas, guards, errors, tests, roadmap |

---

## Design highlights (for reviewers / recruiters)

- **Service boundaries** — platform concerns stay in Java; LLM reasoning is isolated and replaceable
- **Allowlist-driven routing** — the model cannot invent specialties missing from the clinic roster
- **Hardened triage graph** — emergency rules, jailbreak guard, structured outputs, conversation-aware follow-ups
- **Transactional chat persistence** — patient message saved before the AI call; AI reply saved after
- **Testable AI integration** — Java `AiTriageClient` (stub + REST); Python pytest with mocked LLM
- **Production-minded defaults** — secrets via env vars / gitignored profiles, Redis caching, OpenAPI, global exception handling

---

## Project status

| Area | State |
|---|---|
| Auth, doctors, appointments, slots | Implemented |
| AI triage chat + specialty routing | Implemented |
| Triage hardening (structured outputs, topic guard, emergency boundaries, tests) | Implemented |
| Recommended doctors after triage | Implemented |
| Frontend (Vite + React) | Implemented |
| Internal AI service auth (shared API key) | Planned |
| Dockerfile / deploy packaging for AI service | Planned |
| RAG over clinical knowledge | Deferred |

---

## Security notes

- Never commit `.env`, `application-dev.yml`, or real API keys (covered by root / service `.gitignore`)
- JWT secret and DB password must come from environment variables
- Chat endpoints resolve the patient from the JWT — clients do not pick arbitrary patient IDs for triage
- The AI service currently has **no** internal API-key auth (planned); keep it on localhost or behind a private network for now

---

## License

Private / educational project unless otherwise noted.
