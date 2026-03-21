# VaidyaLink - Healthcare Appointment Platform

![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.2-brightgreen)
![PostgreSQL](https://img.shields.io/badge/Database-PostgreSQL-336791)
![Build](https://img.shields.io/badge/Build-Maven-C71A36)
![Security](https://img.shields.io/badge/Auth-JWT-orange)

VaidyaLink is a backend-first healthcare scheduling system built with Spring Boot. It models a realistic clinic workflow where patients discover doctors, book slots, and track appointment lifecycle with validation, role-based access, and business-rule enforcement.

This repository currently contains the backend service in `backend/`.

## Product Goal

The project focuses on moving beyond CRUD and implementing production-style scheduling constraints:

- secure authentication and authorization
- consistent API contracts with DTOs
- validation at request boundaries
- business logic in service layer
- paginated retrieval and search for scale-ready APIs

## Key Features

- JWT login flow with stateless Spring Security
- Role-based endpoints for `PATIENT` and `DOCTOR`
- Patient and doctor onboarding via auth APIs
- Appointment booking with slot conflict checks
- Appointment status lifecycle via enum (`PENDING`, `CONFIRMED`, `CANCELLED`, `COMPLETED`)
- Available-slot generation (20-minute intervals)
- Doctor-side appointment search (name/mobile)
- Upcoming appointment retrieval for patients
- Centralized exception handling for clean API errors

## Architecture

```text
Controller -> Service -> Repository -> PostgreSQL
```

- `controller`: API routes, request parsing, response shaping
- `service`: business rules and orchestration
- `repository`: Spring Data JPA query layer
- `entity`: relational domain model
- `dto`: request/response contracts
- `security`: JWT auth, request filtering, role checks
- `exception`: global error handling

## Tech Stack

- Java 21
- Spring Boot 4.0.2
- Spring Web MVC
- Spring Data JPA
- Spring Security
- Jakarta Validation
- JJWT (`io.jsonwebtoken`)
- PostgreSQL
- Lombok
- Maven

## Repository Layout

```text
VaidyaLink/
  backend/
	src/main/java/com/vaidyalink/backend/
	  controller/
	  service/
	  repository/
	  entity/
	  dto/
	  security/
	  exception/
	src/main/resources/
	  application.yml
	  application-dev.yml
```

## API Overview

- Auth: `/api/auth/**`
- Patient reads: `/api/patients/**`
- Doctor reads: `/api/doctors/**`
- Appointments: `/api/appointments/**`

See `backend/README.md` for full endpoint list, payloads, role matrix, and booking rules.

## Local Run (Backend)

From `backend/` set environment variables and start the app.

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/vaidyalink"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your_password_here"
$env:JWT_SECRET="your_base64_secret"
.\mvnw.cmd spring-boot:run
```

## Reviewer Notes

- The codebase demonstrates end-to-end API lifecycle design (auth -> validation -> service rules -> persistence -> structured errors).
- DTO usage prevents direct entity exposure for registration/login contracts.
- Security is stateless and token-driven, suitable for SPA/mobile clients.

## Important Configuration Note

`application.yml` reads secrets via environment variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`). Use env vars for local and deployed setups; avoid committing credentials/secrets.


