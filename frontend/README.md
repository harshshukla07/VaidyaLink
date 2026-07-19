# VaidyaLink Frontend

![React](https://img.shields.io/badge/React-19-61DAFB)
![TypeScript](https://img.shields.io/badge/TypeScript-5-3178C6)
![Vite](https://img.shields.io/badge/Vite-Dev%20Server-646CFF)
![Router](https://img.shields.io/badge/React%20Router-6-CA4245)

React + TypeScript single-page application for VaidyaLink. It is a thin JWT client over the Spring Boot platform API: patients triage symptoms and book doctors; doctors manage schedules and slots. The UI stays deliberately simple — presentation and navigation live here; business rules and AI reasoning stay in the backend and Python services.

> Parent overview: [`../README.md`](../README.md) · Backend: [`../backend/README.md`](../backend/README.md) · AI: [`../ai-triage-service/README.md`](../ai-triage-service/README.md)

## Table of contents

- [What this app does](#what-this-app-does)
- [Tech stack](#tech-stack)
- [Project layout](#project-layout)
- [Screens and routes](#screens-and-routes)
- [Auth model](#auth-model)
- [API client](#api-client)
- [Configuration](#configuration)
- [Run locally](#run-locally)
- [Build](#build)
- [How triage appears in the UI](#how-triage-appears-in-the-ui)
- [Known gaps](#known-gaps)

## What this app does

### For patients
- Register / login and land on a care home with clear next steps
- Start or resume an AI **symptom triage** conversation
- When triage completes, see **recommended specialty + matching doctors**
- Browse doctors by specialty, open booking, pick an available slot
- View upcoming / past appointments and cancel when allowed

### For doctors
- Login to a doctor workspace
- Review appointments and update status (`PENDING` → `CONFIRMED` / `CANCELLED` / `COMPLETED`)
- Generate availability slots for a given day and shift window

The frontend does **not** call the Python AI service directly, and it never holds the AI service API key. All triage traffic goes:

```text
Browser → Spring Boot (/api/chat/*, JWT) → AI triage service (:8000, X-API-Key)
```

## Tech stack

| Layer | Choice |
|---|---|
| UI library | React 19 |
| Language | TypeScript |
| Bundler / dev server | Vite (default `http://localhost:5173`) |
| Routing | React Router |
| HTTP | Native `fetch` (no Axios) |
| Auth storage | JWT in client storage (see `api/client.ts`) |

Backend CORS already allows `http://localhost:5173`.

## Project layout

```text
frontend/
├── README.md
├── package.json
├── vite.config.ts
├── index.html
└── src/
    ├── main.tsx
    ├── App.tsx                 # route table
    ├── api/
    │   ├── client.ts           # fetch wrapper, auth header, endpoints
    │   └── types.ts            # shared DTOs (ChatReply, Doctor, …)
    ├── components/
    │   ├── AppShell.tsx        # nav / layout
    │   ├── ProtectedRoute.tsx  # role gate
    │   └── …
    └── pages/
        ├── Landing.tsx
        ├── Login.tsx · Register.tsx
        ├── patient/            # Home, Chat, Doctors, Book, Appointments
        └── doctor/             # Dashboard, Slots
```

## Screens and routes

| Route | Access | Purpose |
|---|---|---|
| `/` | Public | Product landing |
| `/login` | Public | JWT login |
| `/register` | Public | Patient or doctor registration |
| `/home` | `ROLE_PATIENT` | Care workspace / shortcuts |
| `/chat` | `ROLE_PATIENT` | AI symptom triage |
| `/doctors` | `ROLE_PATIENT` | Browse / filter by specialty |
| `/book/:doctorId` | `ROLE_PATIENT` | Pick slot and book |
| `/appointments` | `ROLE_PATIENT` | List / cancel appointments |
| `/doctor` | `ROLE_DOCTOR` | Schedule and status updates |
| `/doctor/slots` | `ROLE_DOCTOR` | Generate day slots |
| `*` | — | Redirect to `/` |

Protected routes wrap content with `ProtectedRoute` + `AppShell`.

## Auth model

1. User submits credentials to `POST /api/auth/login`
2. Backend returns `{ token, role, email, id, name, … }`
3. Frontend stores the token and attaches `Authorization: Bearer <token>` on subsequent calls
4. `ProtectedRoute` checks role (`ROLE_PATIENT` vs `ROLE_DOCTOR`) before rendering a page
5. `GET /api/auth/me` can refresh the current-user profile when needed

Patients never pass another patient’s id into chat — the backend resolves the authenticated patient from the JWT.

## API client

`src/api/client.ts` centralizes:

- Base URL (`VITE_API_BASE_URL` or `http://localhost:8080`)
- JSON request/response handling
- Auth header injection
- Typed helpers for chat, doctors, appointments, auth, slots

Key chat helpers:

| Client method | Backend |
|---|---|
| `getChatSession()` | `GET /api/chat/session` |
| `sendChatMessage(sessionId, text)` | `POST /api/chat/send` |

`ChatReply` includes `aiReply`, `triageComplete`, `recommendedSpecialty`, and `recommendedDoctors`.

## Configuration

Create `frontend/.env` if you need a non-default API host:

```env
VITE_API_BASE_URL=http://localhost:8080
```

| Variable | Default | Purpose |
|---|---|---|
| `VITE_API_BASE_URL` | `http://localhost:8080` | Spring Boot origin |

Vite only exposes variables prefixed with `VITE_`.

## Run locally

### Prerequisites

- Node.js 18+
- Backend running on `:8080` (see [`../backend/README.md`](../backend/README.md))
- For live triage: AI service on `:8000` and `AI_TRIAGE_STUB=false` on the backend

### Dev server

```powershell
cd frontend
npm install
npm run dev
```

Open http://localhost:5173  

### Typical demo flow

1. Register a patient (and at least one doctor with a specialty, if the DB is empty)
2. Login as patient → **Symptom triage**
3. Describe symptoms until a specialty is recommended
4. Choose a recommended doctor → book a slot
5. Login as doctor → confirm the appointment / generate more slots

If triage replies look stubby or always recommend the same path, the backend is probably still on `AI_TRIAGE_STUB=true`. See the [backend README](../backend/README.md) for live AI mode.

## Build

```powershell
npm run build
npm run preview
```

Production assets are emitted to `frontend/dist/`. Serve that folder behind any static host, or use `npm run preview` for a local smoke check.

## How triage appears in the UI

On `/chat`:

1. `GET /api/chat/session` loads or creates an `ACTIVE` session and prior messages
2. Each send calls `POST /api/chat/send`
3. While `triageComplete` is false, the composer stays open for follow-ups
4. When complete, the UI shows the recommended specialty and a list of doctors (unless the specialty is `Emergency`)
5. Off-topic / jailbreak redirects from the AI service still return `triageComplete: false`, so the patient can continue with real symptoms

Backend failures during live triage typically surface as an error state on send (often from a **503** when the Python service is down, misconfigured, or rejecting the shared API key).

For graph details (safety, topic guard, structured LLM outputs, API-key auth), see the [AI triage README](../ai-triage-service/README.md).

## Known gaps

- No automated frontend test suite yet (`*.test.*` / Vitest / Playwright)
- No production Docker image for the SPA in this repo (Vite `build` + static hosting is the current path)
- Auth token storage is client-side; treat local demos accordingly and avoid sharing browser profiles with real credentials
