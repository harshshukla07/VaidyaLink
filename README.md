# VaidyaLink - Healthcare Appointment Platform

![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.2-brightgreen)
![PostgreSQL](https://img.shields.io/badge/Database-PostgreSQL-336791)
![Build](https://img.shields.io/badge/Build-Maven-C71A36)

VaidyaLink is a product-oriented healthcare backend project built to simulate real-world scheduling flows between patients and doctors.

This repository currently contains the backend service in the `backend/` folder, with layered architecture, validation, business rules, pagination, and centralized exception handling.

## Why This Project Exists

Healthcare booking workflows are deceptively complex. Basic CRUD is not enough. Systems need to enforce constraints such as slot conflicts, time validity, and controlled status transitions.

VaidyaLink is designed to solve that with a clean Spring Boot architecture and production-style API contracts.

## What Reviewers Should Notice

- Domain modeling with relational mapping: `Patient`, `Doctor`, `Appointment`
- DTO-driven API input for secure request contracts (`AppointmentRequest`)
- Business-rule-first service layer (not controller-level shortcuts)
- Validation at request boundary with clean, consumable error responses
- Pagination and filtering to handle growth scenarios
- Explicit appointment lifecycle using enum statuses

## Current Scope (Implemented)

### Patient APIs
- Register patient
- Get patient by ID

### Doctor APIs
- Register doctor
- Filter doctors by speciality

### Appointment APIs
- Book appointment from IDs + date/time payload
- Get appointments by patient (paged)
- Get appointments by doctor (paged)
- Filter doctor appointments by date
- Update appointment status
- Get available slots for a doctor/date
- Search doctor appointments by patient name or mobile
- Get upcoming appointments for a patient

## Business Rules Implemented

- Backend controls default appointment status (`PENDING`)
- Slot structure enforced (20-minute boundaries: `:00`, `:20`, `:40`)
- Booking past-time slots for today is blocked
- Double booking for same doctor/date/time is blocked
- Invalid status transitions are prevented for terminal states

## Technical Stack

- Java 21
- Spring Boot 4.0.2
- Spring Web MVC
- Spring Data JPA
- PostgreSQL
- Jakarta Bean Validation
- Lombok
- Maven

## Architecture

```
Controller -> Service -> Repository -> PostgreSQL
```

- `controller`: API contract and request/response handling
- `service`: business logic and workflow orchestration
- `repository`: query abstraction using Spring Data JPA
- `entity`: database model mapping
- `dto`: request shaping for safer boundaries
- `exception`: centralized and consistent error responses

## Project Layout

```
VaidyaLink/
  backend/
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

## API Snapshot

Base paths:
- `/api/patients`
- `/api/doctors`
- `/api/appointments`

Representative endpoints:
- `POST /api/patients/register`
- `GET /api/patients/{id}`
- `POST /api/doctors/register`
- `GET /api/doctors?speciality=Cardiologist`
- `POST /api/appointments/book`
- `PATCH /api/appointments/{appointmentId}/status?status=CONFIRMED`

For full endpoint details and payload samples, see `backend/README.md`.

## Run the Backend Locally

From `backend/`, set DB environment variables and run the app.

Windows PowerShell:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/vaidyalink"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your_password_here"
.\mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/vaidyalink"
export DB_USERNAME="postgres"
export DB_PASSWORD="your_password_here"
./mvnw spring-boot:run
```

## Engineering Notes

- This project is intentionally built as a learning-to-production progression.
- Design choices prioritize correctness and maintainability over shortcut implementations.
- Schema and entity changes should be synchronized carefully to avoid runtime mismatch issues.

## For Hiring Review

This project demonstrates readiness for backend engineering work in:

- API design and layered architecture
- Data modeling and relational mapping
- Business rule implementation
- Validation and error handling
- Query optimization foundations (pagination/filtering)

If you are reviewing this for internship/full-time backend hiring, please start with `backend/README.md` for implementation-level details.


