# VaidyaLink Backend

![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.2-brightgreen)
![PostgreSQL](https://img.shields.io/badge/Database-PostgreSQL-336791)
![Build](https://img.shields.io/badge/Build-Maven-C71A36)
![Security](https://img.shields.io/badge/Auth-JWT-orange)

Backend service for a healthcare appointment platform where patients and doctors authenticate via JWT, access role-protected APIs, and manage appointments with business-rule enforcement.

## Table of Contents

- [Implemented Scope](#implemented-scope)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Security Model](#security-model)
- [Data Model](#data-model)
- [Validation and Rules](#validation-and-rules)
- [API Endpoints](#api-endpoints)
- [Sample Requests](#sample-requests)
- [Error Handling](#error-handling)
- [Configuration](#configuration)
- [Run Locally](#run-locally)
- [Troubleshooting Notes](#troubleshooting-notes)

## Implemented Scope

- JWT-based authentication and role extraction (`ROLE_PATIENT`, `ROLE_DOCTOR`)
- Public auth endpoints for registration and login
- Protected patient and doctor read APIs
- Appointment booking with conflict prevention and time validation
- Appointment listing by patient/doctor with pagination
- Date-filtered doctor appointment retrieval
- Appointment status updates with terminal-state guardrails
- Available-slot generation for doctor/day
- Doctor-side appointment search by patient name/mobile
- Patient-side upcoming appointments API

## Tech Stack

- Java 21
- Spring Boot 4.0.2
- Spring Web MVC
- Spring Data JPA
- Spring Security
- Jakarta Bean Validation
- JJWT (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`)
- PostgreSQL
- Lombok
- Springdoc OpenAPI (Swagger UI)
- Maven

## Architecture

```text
Controller -> Service -> Repository -> PostgreSQL
```

- `controller`: accepts HTTP requests and returns API responses
- `service`: applies domain/business rules
- `repository`: query abstraction through Spring Data JPA
- `entity`: relational table mappings
- `dto`: request/response contracts (`AppointmentRequest`, register/login DTOs)
- `security`: JWT utility, auth filter, user loading, access config
- `exception`: centralized JSON error responses

## Security Model

### Authentication

- Login endpoint returns a JWT token and caller role.
- Token is expected in `Authorization: Bearer <token>`.
- App is stateless (`SessionCreationPolicy.STATELESS`).

### Authorization

- Public routes:
  - `/api/auth/**`
  - `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`
- All other endpoints require authentication.
- Method-level role checks are enabled via `@EnableMethodSecurity` + `@PreAuthorize`.

### CORS

Configured allowed origins:

- `http://localhost:3000`
- `http://localhost:5173`

Allowed methods: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`.

## Data Model

### `Patient`

- `id` (Long, PK)
- `name`
- `email` (unique)
- `mobile` (unique)
- `gender`
- `age`
- `password` (BCrypt-hashed)

### `Doctor`

- `id` (Long, PK)
- `name`
- `email` (unique)
- `speciality`
- `experience`
- `password` (BCrypt-hashed)

### `Appointment`

- `id` (Long, PK)
- `patient` (`@ManyToOne` -> `patient_id`)
- `doctor` (`@ManyToOne` -> `doctor_id`)
- `appointmentDate` (`LocalDate`)
- `appointmentTime` (`LocalTime`)
- `status` (`AppointmentStatus` enum)

## Validation and Rules

### Request Validation

- Patient registration DTO:
  - name required
  - email required + valid format
  - mobile required + exact 10 digits
  - age must be `>= 0`
  - password required
- Doctor registration DTO:
  - name required
  - email required + valid format
  - speciality required
  - experience must be `>= 0`
  - password required
- Appointment request DTO:
  - date must be present or future
  - time required

### Appointment Booking Rules

- Appointment time is normalized to minute precision.
- Same-day past time booking is blocked.
- Allowed minute boundaries are fixed to `:00`, `:20`, `:40`.
- Slot conflict check blocks duplicate doctor/date/time bookings (except cancelled).
- Backend enforces initial status as `PENDING`.

### Status Transition Guard

- If current status is `CANCELLED` or `COMPLETED`, status update is rejected.

## API Endpoints

### Auth (`/api/auth`)

| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| `POST` | `/api/auth/register/patient` | Public | Register patient |
| `POST` | `/api/auth/register/doctor` | Public | Register doctor |
| `POST` | `/api/auth/login` | Public | Authenticate and receive JWT |

### Patients (`/api/patients`)

| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| `GET` | `/api/patients/{id}` | `PATIENT`, `DOCTOR` | Get patient by ID |
| `GET` | `/api/patients/all` | `DOCTOR` | List all patients |

### Doctors (`/api/doctors`)

| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| `GET` | `/api/doctors/{id}` | `PATIENT`, `DOCTOR` | Get doctor by ID |
| `GET` | `/api/doctors?speciality=Cardiologist` | `PATIENT`, `DOCTOR` | Filter by speciality |
| `GET` | `/api/doctors/all` | `PATIENT`, `DOCTOR` | List all doctors |

### Appointments (`/api/appointments`)

| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| `POST` | `/api/appointments/book` | `PATIENT` | Book appointment |
| `GET` | `/api/appointments/patient/{patientId}?page=0&size=10` | `PATIENT`, `DOCTOR` | Patient appointments (paged) |
| `GET` | `/api/appointments/patient/{patientId}/upcoming?page=0&size=10` | `PATIENT` | Upcoming patient appointments |
| `GET` | `/api/appointments/doctor/{doctorId}?page=0&size=10` | `DOCTOR` | Doctor appointments (paged) |
| `GET` | `/api/appointments/doctor/{doctorId}?date=2026-03-14&page=0&size=10` | `DOCTOR` | Doctor appointments by date |
| `PATCH` | `/api/appointments/{appointmentId}/status?status=CONFIRMED` | `PATIENT`, `DOCTOR` | Update appointment status |
| `GET` | `/api/appointments/doctor/{doctorId}/available-slots?date=2026-03-14` | `PATIENT`, `DOCTOR` | Get available slots |
| `GET` | `/api/appointments/doctor/{doctorId}/search?query=rahul&page=0&size=10` | `DOCTOR` | Search appointments |

## Sample Requests

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

### Register doctor

```json
{
  "name": "Dr. Mehta",
  "email": "mehta@example.com",
  "speciality": "Cardiologist",
  "experience": 10,
  "password": "mehta@123"
}
```

### Login

```json
{
  "email": "rahul@example.com",
  "password": "rahul@123"
}
```

### Book appointment

```json
{
  "patientId": 5,
  "doctorId": 2,
  "appointmentDate": "2026-03-20",
  "appointmentTime": "10:20:00"
}
```

## Error Handling

`GlobalExceptionHandler` maps key exceptions to clean JSON messages:

- `MethodArgumentNotValidException` -> `400 Bad Request`
- `DataIntegrityViolationException` -> `409 Conflict`
- `MethodArgumentTypeMismatchException` -> `400 Bad Request`
- `IllegalStateException` -> `400 Bad Request`
- `EntityNotFoundException` -> `404 Not Found`
- `HttpRequestMethodNotSupportedException` -> `405 Method Not Allowed`
- `AccessDeniedException` -> `403 Forbidden`
- fallback `Exception` -> `500 Internal Server Error`

## Configuration

`src/main/resources/application.yml` expects:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET` (Base64-encoded secret)

## Run Locally

### Prerequisites

- Java 21
- PostgreSQL running
- A database created (example: `vaidyalink`)

### Windows PowerShell

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/vaidyalink"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your_password_here"
$env:JWT_SECRET="your_base64_secret"
.\mvnw.cmd spring-boot:run
```

### Linux/macOS

```bash
export DB_URL="jdbc:postgresql://localhost:5432/vaidyalink"
export DB_USERNAME="postgres"
export DB_PASSWORD="your_password_here"
export JWT_SECRET="your_base64_secret"
./mvnw spring-boot:run
```

### OpenAPI

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI docs: `http://localhost:8080/v3/api-docs`

## Troubleshooting Notes

- If you add new fields (for example `appointmentTime`) after table creation, keep DB schema synchronized before testing APIs.
- Keep secrets in environment variables; do not commit production credentials/secrets.
- If auth fails unexpectedly, verify Bearer token format and role constraints on the target endpoint.
