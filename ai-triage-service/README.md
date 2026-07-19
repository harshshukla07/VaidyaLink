# VaidyaLink AI Triage Service

![Python](https://img.shields.io/badge/Python-3.12+-3776AB)
![FastAPI](https://img.shields.io/badge/FastAPI-0.115-009688)
![LangGraph](https://img.shields.io/badge/LangGraph-Orchestration-1C3C3C)
![OpenAI](https://img.shields.io/badge/LLM-gpt--4o--mini-412991)
![Tests](https://img.shields.io/badge/Tests-pytest-0A9EDC)

Stateless medical **triage microservice** for VaidyaLink. Spring Boot sends a chat history plus a clinic specialty allowlist; this service runs a LangGraph workflow and returns either a clarifying question, an emergency message, an off-topic redirect, or a recommended specialty.

> Parent overview: [`../README.md`](../README.md) · Backend: [`../backend/README.md`](../backend/README.md) · Frontend: [`../frontend/README.md`](../frontend/README.md)

## Table of contents

- [Why a separate service](#why-a-separate-service)
- [Responsibilities (and non-goals)](#responsibilities-and-non-goals)
- [Graph design](#graph-design)
- [Guards and hardening](#guards-and-hardening)
- [Structured LLM outputs](#structured-llm-outputs)
- [Project layout](#project-layout)
- [API contract](#api-contract)
- [Tech stack](#tech-stack)
- [Configuration](#configuration)
- [Run locally](#run-locally)
- [Errors](#errors)
- [Tests](#tests)
- [Design notes](#design-notes)
- [Roadmap](#roadmap)
- [Disclaimer](#disclaimer)

## Why a separate service

Keeping LLM reasoning out of Spring Boot gives:

- **Independent scaling and deploy cadence** for AI vs platform APIs
- **Clear ownership** — Java is source of truth for auth, DB, and specialties
- **Safer experimentation** — graph/prompt changes do not require a Java rebuild
- **Stub-friendly integration** — the backend can run without OpenAI via `AI_TRIAGE_STUB=true`

## Responsibilities (and non-goals)

### This service owns
- Emergency short-circuit (rules)
- Jailbreak / off-topic short-circuit (rules)
- Completeness assessment (LLM)
- Follow-up question generation (LLM)
- Specialty recommendation within an allowlist (LLM + validation)

### This service does **not** own
- User authentication or JWT validation (today)
- Chat session persistence (Java stores history and resends it each call)
- Doctor search or booking
- Inventing specialties that are not on the allowlist

Every HTTP call is **self-contained**: full message history in, triage decision out.

## Graph design

```text
START
  └─▶ safety_check
        ├─ emergency phrases matched ─▶ emergency_response ─▶ END
        └─▶ topic_guard
              ├─ jailbreak on latest patient msg ─▶ off_topic_response ─▶ END
              └─▶ assess_completeness (LLM, structured)
                    ├─ enough info ─▶ route_specialty (LLM + allowlist) ─▶ END
                    └─ incomplete ──▶ generate_followup (LLM) ──────────▶ END
```

| Node | Type | Behavior |
|---|---|---|
| `safety_check` | Rules | Word-boundary match on emergency phrases across **all** patient text |
| `emergency_response` | Rules | Fixed urgent-care message; `is_complete=true`, specialty `Emergency` |
| `topic_guard` | Rules | Injection / role-override phrases on the **latest** patient message only |
| `off_topic_response` | Rules | Fixed “describe your symptoms” reply; `is_complete=false` so the user can continue |
| `assess_completeness` | LLM | Structured bool: enough info to recommend a specialty? |
| `generate_followup` | LLM | Structured question string (plain text, no repeated UI prefix) |
| `route_specialty` | LLM | Structured specialty name → validated against allowlist → templated reply |

### Conversation context

LLM nodes format history with `_format_conversation`:

```text
PATIENT: Mild headache for 2 days
AI: How severe is it on a scale of 1-10?
PATIENT: About a 6
```

Java sends `senderType` values `PATIENT` and `AI_BOT`. Display labels use `PATIENT:` / `AI:`.

Follow-up prompts explicitly say **do not repeat** a question already asked.

## Guards and hardening

### Emergency matching
- Phrases live in `EMERGENCY_KEYWORDS` (`constants.py`)
- Matching uses `\b…\b` word boundaries (so `"chest paintball"` does **not** match `"chest pain"`)
- Apostrophe variants are normalized for phrases like `"can't breathe"`

### Jailbreak / topic guard
- Phrases live in `INJECTION_PHRASES` (e.g. “forget previous”, “act as”, “personal chat bot”)
- Only the **latest** patient message is checked — an earlier off-topic turn must not permanently block later real symptoms
- Off-topic path never calls the LLM

### Prompt scope rules
All LLM prompts prepend `TRIAGE_SCOPE_RULES`: stay a medical triage assistant; ignore role-change / “forget instructions” attempts.

### Specialty reply template
After allowlist validation, `route_specialty` builds the user-facing sentence in code (the model only chooses the specialty name).

## Structured LLM outputs

Schemas in `app/schemas/llm.py`:

| Schema | Fields | Used by |
|---|---|---|
| `CompletenessAssessment` | `has_enough_info: bool` | `assess_completeness` |
| `FollowUpQuestion` | `question: str` | `generate_followup` |
| `SpecialtyChoice` | `specialty: str` | `route_specialty` |

Invoked via:

```python
get_llm().with_structured_output(SomeSchema).invoke(prompt)
```

Provider failures are wrapped as `LlmServiceError` → HTTP **503**.

Follow-up questions are sanitized (empty / too long / injection-like → `DEFAULT_FOLLOWUP_QUESTION`).

## Project layout

```text
ai-triage-service/
├── README.md
├── requirements.txt
├── pytest.ini
├── .env.example
├── .gitignore
├── tests/
│   ├── conftest.py          # FakeLLM helpers + fixtures
│   ├── test_nodes.py
│   ├── test_graph.py
│   └── test_api.py
└── app/
    ├── main.py              # FastAPI, validation, exception handlers
    ├── errors.py
    ├── llm/client.py
    ├── schemas/
    │   ├── triage.py        # HTTP request/response (camelCase aliases)
    │   └── llm.py           # Structured LLM schemas
    └── graph/
        ├── state.py
        ├── constants.py
        ├── nodes.py
        └── builder.py
```

## API contract

### `GET /health`

```json
{
  "status": "UP",
  "service": "ai-triage-service",
  "version": "0.1.0"
}
```

### `POST /api/ai/triage`

**Request** (camelCase accepted via Pydantic aliases):

```json
{
  "sessionId": 1,
  "messages": [
    {
      "id": 1,
      "senderType": "PATIENT",
      "messageText": "Mild headache for two days, no fever or vomiting."
    },
    {
      "id": 2,
      "senderType": "AI_BOT",
      "messageText": "How severe is the headache on a scale of 1–10?"
    },
    {
      "id": 3,
      "senderType": "PATIENT",
      "messageText": "About a 6, worse in the evening."
    }
  ],
  "allowedSpecialties": [
    "Cardiologist",
    "Dermatology",
    "General Physician",
    "Orthopedics"
  ]
}
```

**Response**

```json
{
  "ai_reply": "Based on your symptoms, I recommend seeing a General Physician. You can book an appointment now.",
  "is_complete": true,
  "recommended_specialty": "General Physician"
}
```

| Field | Meaning |
|---|---|
| `ai_reply` | Message shown to the patient |
| `is_complete` | `true` for route/emergency; `false` for follow-up or off-topic redirect |
| `recommended_specialty` | Specialty name, `Emergency`, or `null` |

## Tech stack

| Piece | Choice |
|---|---|
| HTTP | FastAPI + Uvicorn |
| Orchestration | LangGraph `StateGraph` |
| LLM | LangChain OpenAI (`gpt-4o-mini` default) |
| Validation | Pydantic v2 |
| Secrets | python-dotenv (`.env`) |
| Tests | pytest + httpx `TestClient` |

## Configuration

```powershell
copy .env.example .env
```

| Variable | Required | Default | Purpose |
|---|---|---|---|
| `OPENAI_API_KEY` | Yes (live LLM) | — | OpenAI API key |
| `OPENAI_MODEL` | No | `gpt-4o-mini` | Chat model name |

`.env` is gitignored. Never commit real keys.

## Run locally

### Prerequisites

- Python 3.12+ recommended
- OpenAI API key for live calls (tests do not need one)

### Windows PowerShell

```powershell
cd ai-triage-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
copy .env.example .env
# edit .env → set OPENAI_API_KEY
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

### Linux / macOS

```bash
cd ai-triage-service
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

- Health: http://localhost:8000/health  
- Interactive docs: http://localhost:8000/docs  

### Wire to Spring Boot

```powershell
$env:AI_TRIAGE_STUB="false"
$env:AI_TRIAGE_URL="http://localhost:8000"
```

Restart the backend so `RestAiTriageClient` is active.

> Note: on Windows, uvicorn `--reload` can print a noisy `KeyboardInterrupt` traceback during hot reload. If `Application startup complete` appears afterward, the server is fine.

## Errors

| Status | When |
|---|---|
| `400` | Empty `messages`, or no non-blank `PATIENT` text |
| `503` | Missing `OPENAI_API_KEY`, provider failure, or unexpected graph failure |

Both return JSON: `{ "detail": "..." }`.

## Tests

```powershell
pip install -r requirements.txt
pytest
```

| File | Focus |
|---|---|
| `tests/test_nodes.py` | Emergency boundaries, topic guard (incl. history regression), structured nodes |
| `tests/test_graph.py` | Emergency / injection / follow-up / route paths |
| `tests/test_api.py` | Health, validation 400, LLM 503, success + jailbreak HTTP |

LLM calls are mocked (`FakeLLM`) — no API key required in CI or local test runs.

## Design notes

- **Allowlist first** — routing is constrained to specialties Java loaded from the doctor table
- **Full conversation context** — LLM nodes see patient + AI turns
- **Emergency is deterministic** — word-boundary phrases; no LLM
- **Jailbreak short-circuit** — latest message only, so users can recover with real symptoms
- **Structured outputs** — avoid brittle free-text parsing of `True`/`False` / specialty names
- **Templated specialty reply** — user-facing routing sentence is built in code
- **Stateless v1** — Java resends history every call; no Python checkpointing yet
- **GP fallback** — invalid / unknown specialty → `General Physician`

## Roadmap

| Item | Status |
|---|---|
| Safety + completeness + specialty routing | Done |
| Specialty allowlist from Java | Done |
| Structured LLM outputs + error handling + tests | Done |
| Conversation context (patient + AI history) | Done |
| Jailbreak / off-topic guard (latest-message) | Done |
| Word-boundary emergency matching | Done |
| Internal service auth (shared API key with Java) | Planned |
| Dockerfile + structured request logging (`sessionId`) | Planned |
| RAG / clinical knowledge base | Planned |
| Graph checkpointing in Python | Deferred (Java owns history) |

## Disclaimer

This service provides **routing assistance only**. It is not a medical diagnosis tool and must not replace professional clinical judgment.
