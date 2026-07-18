# VaidyaLink Frontend

Minimal React + Vite client for the VaidyaLink platform API.

## Stack

- React 19 + TypeScript
- Vite (dev server on `http://localhost:5173`)
- React Router
- Native `fetch` against Spring Boot (`http://localhost:8080`)

CORS is already enabled for port `5173` in the backend.

## Run

```powershell
cd frontend
npm install
npm run dev
```

Optional: set `VITE_API_BASE_URL` in `.env` (defaults to `http://localhost:8080`).

## Screens

| Route | Role | Purpose |
|---|---|---|
| `/` | Public | Landing |
| `/login`, `/register` | Public | Auth |
| `/home` | Patient | Care workspace / next visit |
| `/chat` | Patient | AI symptom triage |
| `/doctors` | Patient | Browse by specialty |
| `/book/:doctorId` | Patient | Pick slot & book |
| `/appointments` | Patient | View / cancel |
| `/doctor` | Doctor | Schedule & status |
| `/doctor/slots` | Doctor | Generate availability |

## Build

```powershell
npm run build
npm run preview
```
