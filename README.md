# VaidyaLink

![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.2-brightgreen)
![Python](https://img.shields.io/badge/Python-3.12+-3776AB)
![FastAPI](https://img.shields.io/badge/FastAPI-0.115-009688)
![LangGraph](https://img.shields.io/badge/LangGraph-AI%20Triage-1C3C3C)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-336791)
![Redis](https://img.shields.io/badge/Redis-Cache-DC382D)
![Security](https://img.shields.io/badge/Auth-JWT-orange)

**AI-assisted healthcare appointment platform** — patients describe symptoms in chat, get routed to a specialty, and book a real doctor slot. Built as a clean separation between a Spring Boot platform API and a LangGraph triage microservice.

---

## Why this project

Most appointment demos stop at CRUD. VaidyaLink models a realistic clinic path:

1. **Authenticate** as a patient or doctor (JWT + role-based access)
2. **Triage** symptoms through a conversational AI flow (safety → completeness → specialty)
3. **Discover** doctors for the recommended specialty
4. **Book** a conflict-checked slot with lifecycle status (`PENDING` → `CONFIRMED` / `CANCELLED` / `COMPLETED`)

The AI service is intentionally **stateless**. Java owns identity, persistence, and specialty truth; Python owns reasoning.

---

## Architecture

```text
┌─────────────┐     JWT      ┌──────────────────────────────┐
│  Frontend   │─────────────▶│  Spring Boot Backend (:8080) │
│  React :5173│              │  Auth · Chat · Doctors ·     │
└─────────────┘              │  Appointments · Slots        │
                             └──────────────┬───────────────┘
                                    │       │
                         PostgreSQL │       │ Redis cache
                                    │       │
                                    │       ▼
                                    │  specialty allowlist
                                    │  (from DB, cached)
                                    │
                                    ▼
                             ┌──────────────────────────────┐
                             │  AI Triage Service (:8000)   │
                             │  FastAPI + LangGraph         │
                             │  OpenAI (gpt-4o-mini)        │
                             └──────────────────────────────┘
```

| Concern | Owned by |
|---|---|
| Auth, sessions, chat history, doctors, booking | Java / Spring Boot |
| Symptom reasoning, follow-ups, specialty routing | Python / LangGraph |
| Specialty allowlist source of truth | Database via Java |
| Hot read caching (doctors, specialties) | Redis |

---

## Repository layout

```text
VaidyaLink/
├── README.md                 ← you are here
├── frontend/                 ← React + Vite SPA
├── backend/                  ← Spring Boot platform API
│   ├── README.md
│   ├── docker-compose.yml    ← Redis for local cache
│   └── src/...
└── ai-triage-service/        ← FastAPI + LangGraph microservice
    ├── README.md
    ├── requirements.txt
    └── app/
        ├── main.py
        ├── graph/            ← LangGraph nodes + routing
        ├── llm/              ← OpenAI client
        └── schemas/          ← Pydantic contracts
```

---

## Key capabilities

### Platform (`backend/`)
- JWT authentication with `PATIENT` / `DOCTOR` roles
- Patient & doctor registration, `/api/auth/me`
- Appointment booking with slot conflict and time-boundary rules
- Doctor slot generation and available-slot queries
- Redis-backed caching for doctor pages and distinct specialties
- AI chat orchestration: persist messages, call triage, return recommended doctors

### AI triage (`ai-triage-service/`)
- LangGraph workflow: **safety → assess completeness → follow-up | route specialty**
- Rule-based emergency keyword short-circuit
- LLM follow-up questions when history is incomplete
- Specialty choice constrained to Java-provided allowlist (fallback: General Physician)
- Stub mode in Java for offline / test runs without Python or OpenAI

---

## Quick start

### 1. Infrastructure

```bash
# PostgreSQL: create a database named vaidyalink
# Redis (from backend/):
cd backend
docker compose up -d
```

### 2. Backend (port `8080`)

```powershell
cd backend
$env:DB_URL="jdbc:postgresql://localhost:5432/vaidyalink"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your_password"
$env:JWT_SECRET="your_base64_secret"
$env:AI_TRIAGE_STUB="true"   # set false when Python service is running
.\mvnw.cmd spring-boot:run
```

Swagger UI: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

### 3. AI triage service (port `8000`)

```powershell
cd ai-triage-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
copy .env.example .env   # then set OPENAI_API_KEY
uvicorn app.main:app --reload --port 8000
```

Then point the backend at it:

```powershell
$env:AI_TRIAGE_STUB="false"
$env:AI_TRIAGE_URL="http://localhost:8000"
```

### 4. Frontend (port `5173`)

```powershell
cd frontend
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173). Details: [`frontend/README.md`](frontend/README.md).

---

## Documentation

| Doc | Contents |
|---|---|
| [`frontend/README.md`](frontend/README.md) | SPA routes, local run, env |
| [`backend/README.md`](backend/README.md) | Full API matrix, security model, booking rules, config |
| [`ai-triage-service/README.md`](ai-triage-service/README.md) | Graph design, request/response contract, local run |

---

## Design highlights (for reviewers)

- **Clear service boundaries** — platform concerns stay in Java; LLM reasoning is isolated and replaceable
- **Allowlist-driven routing** — AI cannot invent specialties that do not exist in the clinic roster
- **Transactional chat persistence** — patient message saved before the AI call; AI reply saved after
- **Testable AI integration** — `AiTriageClient` interface with stub + REST implementations
- **Production-minded defaults** — secrets via env vars, Redis caching, centralized exception handling, OpenAPI

---

## Status

| Area | State |
|---|---|
| Auth, doctors, appointments, slots | Implemented |
| AI triage chat + specialty routing | Implemented |
| Recommended doctors after triage | Implemented |
| Frontend client | Implemented (Vite + React) |
| RAG over clinical knowledge | Deferred |

---

## License

Private / educational project unless otherwise noted.
