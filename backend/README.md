# VaidyaLink Backend

![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.2-brightgreen)
![PostgreSQL](https://img.shields.io/badge/Database-PostgreSQL-336791)
![Build](https://img.shields.io/badge/Build-Maven-C71A36)

Spring Boot backend for a healthcare appointment platform where patients can register, discover doctors by speciality, and book/manage appointments.

## Quick Navigation

- [Current Features](#current-features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Data Model](#data-model)
- [Validation and Business Rules](#validation-and-business-rules)
- [API Endpoints](#api-endpoints)
- [Sample Payloads](#sample-payloads)
- [Error Handling](#error-handling)
- [Configuration](#configuration)
- [Run Locally](#run-locally)

## Current Features

- Patient registration and patient lookup by ID
- Doctor registration and doctor filtering by speciality
- Appointment booking using DTO (`patientId`, `doctorId`, `appointmentDate`, `appointmentTime`)
- Appointment status updates (`PENDING`, `CONFIRMED`, `CANCELLED`, `COMPLETED`)
- Appointment listing with pagination for patient and doctor
- Available-slot helper for doctor/day
- Doctor appointment search by patient name/mobile
- Upcoming appointments API for patients
- Validation and centralized exception handling

## Tech Stack

- Java 21
- Spring Boot 4.0.2
- Spring Web MVC
- Spring Data JPA
- PostgreSQL
- Jakarta Validation (`spring-boot-starter-validation`)
- Lombok
- Maven

## Architecture

```
Controller -> Service -> Repository -> PostgreSQL
```

- `controller`: receives HTTP requests and returns JSON responses
- `service`: business logic and orchestration
- `repository`: query abstraction using Spring Data JPA
- `entity`: JPA models mapped to tables
- `dto`: controlled request contracts (e.g., booking input)
- `exception`: centralized API error shaping

## Project Structure

```
src/main/java/com/vaidyalink/backend/
  controller/
  service/
  repository/
  entity/
  dto/
  exception/
src/main/resources/
  application.yml
  application-dev.yml
```

## Data Model

### `Patient`
- `id` (Long, PK)
- `name`
- `email` (unique)
- `mobile` (unique)
- `gender`
- `age`

### `Doctor`
- `id` (Long, PK)
- `name`
- `email` (unique)
- `speciality`
- `experience`

### `Appointment`
- `id` (Long, PK)
- `patient` (`@ManyToOne`)
- `doctor` (`@ManyToOne`)
- `appointmentDate` (`LocalDate`)
- `appointmentTime` (`LocalTime`)
- `status` (`AppointmentStatus` enum)

## Validation and Business Rules

### Validation

- Patient:
  - `name`: required
  - `email`: required + valid format
  - `mobile`: required + exactly 10 digits
  - `age`: must be `>= 0`
- Doctor:
  - `name`: required
  - `email`: required + valid format
  - `speciality`: required
  - `experience`: must be `>= 0`
- Appointment request:
  - `appointmentDate`: future or present
  - `appointmentTime`: required

### Booking and Status Rules

- Backend sets default booking status to `PENDING`
- Slot granularity is fixed to 20-minute boundaries (`:00`, `:20`, `:40`)
- Booking past time for today is blocked
- Same doctor cannot be double-booked for same date/time (excluding cancelled)
- Closed statuses (`CANCELLED`, `COMPLETED`) are protected from invalid updates

## API Endpoints

Base paths:
- `/api/patients`
- `/api/doctors`
- `/api/appointments`

### Patient

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/patients/register` | Register patient |
| `GET` | `/api/patients/{id}` | Fetch patient by ID |

### Doctor

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/doctors/register` | Register doctor |
| `GET` | `/api/doctors?speciality=Cardiologist` | List doctors by speciality |

### Appointment

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/appointments/book` | Book appointment |
| `GET` | `/api/appointments/patient/{patientId}?page=0&size=10` | Patient appointments (paged) |
| `GET` | `/api/appointments/patient/{patientId}/upcoming?page=0&size=10` | Upcoming patient appointments |
| `GET` | `/api/appointments/doctor/{doctorId}?page=0&size=10` | Doctor appointments (paged) |
| `GET` | `/api/appointments/doctor/{doctorId}?date=2026-03-14&page=0&size=10` | Doctor appointments by date |
| `PATCH` | `/api/appointments/{appointmentId}/status?status=CONFIRMED` | Update appointment status |
| `GET` | `/api/appointments/doctor/{doctorId}/available-slots?date=2026-03-14` | Available slots for a date |
| `GET` | `/api/appointments/doctor/{doctorId}/search?query=rahul&page=0&size=10` | Search doctor appointments |

## Sample Payloads

### Register patient
```json
{
  "name": "Rahul Sharma",
  "email": "rahul@example.com",
  "mobile": "9876543210",
  "gender": "Male",
  "age": 27
}
```

### Register doctor
```json
{
  "name": "Dr. Mehta",
  "email": "mehta@example.com",
  "speciality": "Cardiologist",
  "experience": 10
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

`GlobalExceptionHandler` currently maps:

- `MethodArgumentNotValidException` -> `400 Bad Request`
- `DataIntegrityViolationException` -> `409 Conflict`
- `MethodArgumentTypeMismatchException` -> `400 Bad Request`
- `IllegalStateException` -> `400 Bad Request`
- `EntityNotFoundException` -> `404 Not Found`
- `HttpRequestMethodNotSupportedException` -> `405 Method Not Allowed`
- Fallback `Exception` -> `500 Internal Server Error`

## Configuration

`application.yml` reads DB config from environment variables:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

Example values:

```bash
DB_URL=jdbc:postgresql://localhost:5432/vaidyalink
DB_USERNAME=postgres
DB_PASSWORD=your_password_here
```

## Run Locally

### Prerequisites

- Java 21
- PostgreSQL running
- Database created (example: `vaidyalink`)

### Windows PowerShell

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/vaidyalink"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your_password_here"
.\mvnw.cmd spring-boot:run
```

### Linux/macOS

```bash
export DB_URL="jdbc:postgresql://localhost:5432/vaidyalink"
export DB_USERNAME="postgres"
export DB_PASSWORD="your_password_here"
./mvnw spring-boot:run
```

## Notes

- If you add fields in entities, ensure the database schema is updated before testing APIs.
- Prefer environment variables for secrets; avoid committing credentials.


---

Built as a backend learning project with production-style layering and incremental business logic implementation.
