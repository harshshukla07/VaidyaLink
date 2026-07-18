# VaidyaLink AI Triage Service

![Python](https://img.shields.io/badge/Python-3.12+-3776AB)
![FastAPI](https://img.shields.io/badge/FastAPI-0.115-009688)
![LangGraph](https://img.shields.io/badge/LangGraph-Orchestration-1C3C3C)
![OpenAI](https://img.shields.io/badge/LLM-gpt--4o--mini-412991)

Stateless medical **triage microservice** for VaidyaLink. It receives a chat history plus a clinic specialty allowlist, runs a LangGraph workflow, and returns either a follow-up question or a recommended specialty.

> Parent overview: [`../README.md`](../README.md) · Backend: [`../backend/README.md`](../backend/README.md)

## Why a separate service

Keeping LLM reasoning out of Spring Boot gives:

- **Independent scaling and deploy cadence** for AI vs platform APIs
- **Clear ownership** — Java is source of truth for auth, DB, and specialties
- **Safer experimentation** — graph/prompt changes do not require a Java rebuild
- **Stub-friendly integration** — the backend can run without OpenAI via `AI_TRIAGE_STUB=true`

This service does **not** own sessions or patients. Every call is self-contained.

## Graph design

```text
START
  └─▶ safety_check
        ├─ (emergency keywords) ─▶ emergency_response ─▶ END
        └─▶ assess_completeness (LLM)
              ├─ enough info ─▶ route_specialty (LLM + allowlist) ─▶ END
              └─ incomplete ──▶ generate_followup (LLM) ──────────▶ END
```

| Node | Type | Behavior |
|---|---|---|
| `safety_check` | Rules | Scans history for emergency keywords |
| `emergency_response` | Rules | Advises urgent care; marks triage complete |
| `assess_completeness` | LLM | Decides if symptoms are sufficient to route |
| `generate_followup` | LLM | Asks a focused clarifying question |
| `route_specialty` | LLM | Picks a specialty from `allowed_specialties` (fallback: General Physician) |

## Project layout

```text
ai-triage-service/
├── README.md
├── requirements.txt
├── .env.example
├── .gitignore
└── app/
    ├── main.py              # FastAPI entry + /api/ai/triage
    ├── llm/client.py        # OpenAI chat model helper
    ├── schemas/triage.py    # Pydantic request/response (camelCase aliases)
    └── graph/
        ├── state.py         # TypedDict graph state
        ├── constants.py     # Emergency keywords, default specialty
        ├── nodes.py         # Node implementations
        └── builder.py       # StateGraph wiring
```

## API

### `GET /health`

```json
{
  "status": "UP",
  "service": "ai-triage-service",
  "version": "0.1.0"
}
```

### `POST /api/ai/triage`

**Request** (camelCase accepted via aliases):

```json
{
  "sessionId": 1,
  "messages": [
    {
      "id": 1,
      "senderType": "PATIENT",
      "messageText": "Mild headache for two days, no fever or vomiting."
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
  "ai_reply": "Based on your symptoms, a General Physician is a good next step.",
  "is_complete": true,
  "recommended_specialty": "General Physician"
}
```

| Field | Meaning |
|---|---|
| `ai_reply` | Message shown to the patient |
| `is_complete` | `true` when routed or emergency; `false` when asking a follow-up |
| `recommended_specialty` | Specialty name, or `null` / emergency label depending on path |

## Tech stack

- **FastAPI** — HTTP API
- **LangGraph** — conditional triage workflow
- **LangChain OpenAI** — `gpt-4o-mini` (configurable)
- **Pydantic v2** — request/response validation
- **python-dotenv** — local secrets

## Configuration

Copy the example env file and set your key:

```powershell
copy .env.example .env
```

| Variable | Required | Default | Purpose |
|---|---|---|---|
| `OPENAI_API_KEY` | Yes (for live LLM) | — | OpenAI API key |
| `OPENAI_MODEL` | No | `gpt-4o-mini` | Chat model name |

`.env` is gitignored. Never commit real keys.

## Run locally

### Prerequisites

- Python 3.12+ recommended
- OpenAI API key

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

Then restart the backend so `RestAiTriageClient` is active.

## Design notes

- **Allowlist first** — routing is constrained to specialties Java loaded from the doctor table
- **Emergency is deterministic** — keyword path does not depend on the LLM
- **Stateless v1** — full message history is sent on every call; no server-side checkpointing yet
- **GP fallback** — if the model returns something outside the allowlist, default to General Physician

## Roadmap

| Item | Status |
|---|---|
| Safety + completeness + specialty routing | Done |
| Specialty allowlist from Java | Done |
| RAG / clinical knowledge base | Planned |
| Graph checkpointing / multi-turn memory in Python | Deferred (Java owns history today) |
| Stronger structured LLM outputs (JSON schema) | Planned |

## Disclaimer

This service provides **routing assistance only**. It is not a medical diagnosis tool and must not replace professional clinical judgment.
