# University Management System

A Spring Boot REST API for managing university administrators, teachers, and students. It uses JWT authentication, BCrypt password hashing, role-based access control, and runs with PostgreSQL through Docker Compose.

## Features

- CRUD REST resources for admins, teachers, and students
- JSON request/response bodies and conventional HTTP status codes
- Login, logout, and current-user endpoints
- Stateless JWT authentication with server-side token revocation
- BCrypt password hashing (strength 12)
- Role-based access control enforced by Spring Security
- PostgreSQL for Docker Compose and in-memory H2 for local development/tests
- Bootstrap accounts and an Actuator health endpoint

## Roles and permissions

| Role | `/api/admins` | `/api/teachers` | `/api/students` |
| --- | --- | --- | --- |
| `ADMIN` | Full CRUD | Full CRUD | Full CRUD |
| `TEACHER` | Forbidden | Full CRUD | Forbidden |
| `STUDENT` | Forbidden | Forbidden | Full CRUD |

Each resource exposes the same CRUD operations:

| Method | Path | Success |
| --- | --- | --- |
| `GET` | `/api/{resource}` | `200 OK` |
| `GET` | `/api/{resource}/{id}` | `200 OK` |
| `POST` | `/api/{resource}` | `201 Created` + `Location` |
| `PUT` | `/api/{resource}/{id}` | `200 OK` |
| `DELETE` | `/api/{resource}/{id}` | `204 No Content` |

`{resource}` is one of `admins`, `teachers`, or `students`. The role is determined by the endpoint, not by a client-supplied role field.

## Quick start with Docker Compose

```bash
cp .env.example .env
# Edit .env and replace every development-only value.
docker compose up --build
```

On Windows PowerShell:

```powershell
Copy-Item .env.example .env
notepad .env
docker compose up --build
```

The API is available at `http://localhost:8080`. PostgreSQL is only reachable inside the Compose network.

If you skip `.env`, the Compose defaults are used. The default bootstrap accounts are:

| Role | Username | Password |
| --- | --- | --- |
| Admin | `admin` | `ChangeMe123!` |
| Teacher | `teacher` | `ChangeMe123!` |
| Student | `student` | `ChangeMe123!` |

Change these credentials before using the application outside a local demo.

Useful commands:

```bash
docker compose logs -f app
docker compose down
docker compose down -v   # also deletes the PostgreSQL volume
```

## Run locally

Requirements: Java 21 and Maven 3.9+. The default profile uses an in-memory H2 database, so no external database is needed.

```bash
./mvnw spring-boot:run
```

On Windows, use `.\mvnw.cmd spring-boot:run`. Or with Maven installed:

```bash
mvn spring-boot:run
```

Run the tests with:

```bash
./mvnw test
# Windows: .\mvnw.cmd test
```

## Tests and continuous integration

The suite is split into three layers, all executed by `./mvnw verify`:

| Layer | Location | Tests |
| --- | --- | --- |
| Unit | `UserAccountServiceTest`, `JwtServiceTest` | 6 |
| Integration | `src/test/java/com/example/university/integration/` | 3 |
| Database | `UserRepositoryDatabaseTest` | 4 |

Unit tests use Mockito and run without a Spring context. Integration tests start the full application with MockMvc and the seeded bootstrap accounts. Database tests use `@DataJpaTest` against an embedded H2 database and verify persistence, ordering, and the unique username constraint.

GitHub Actions runs the same suite on every push and pull request through `.github/workflows/ci.yml`, prints a per-class summary on the run page, and uploads the Surefire reports as a build artifact.

## Authentication API

### Login

```bash
curl -i -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"ChangeMe123!"}'
```

Successful response:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": 1,
    "username": "admin",
    "fullName": "University Administrator",
    "email": "admin@university.example",
    "role": "ADMIN",
    "enabled": true,
    "createdAt": "2026-01-01T00:00:00Z",
    "updatedAt": "2026-01-01T00:00:00Z"
  }
}
```

Send the token as a bearer token on protected requests:

```bash
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $TOKEN"
```

### Logout

```bash
curl -i -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer $TOKEN"
```

Logout increments the account's token version, which revokes every previously issued token for that account.

## CRUD examples

Create a student as an admin:

```bash
curl -i -X POST http://localhost:8080/api/students \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice",
    "password": "AlicePass123!",
    "fullName": "Alice Johnson",
    "email": "alice@university.example",
    "enabled": true
  }'
```

List, update, and delete:

```bash
curl http://localhost:8080/api/students \
  -H "Authorization: Bearer $TOKEN"

curl -i -X PUT http://localhost:8080/api/students/2 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice",
    "fullName": "Alice Johnson-Smith",
    "email": "alice.smith@university.example",
    "enabled": true
  }'

curl -i -X DELETE http://localhost:8080/api/students/2 \
  -H "Authorization: Bearer $TOKEN"
```

`password` is optional on `PUT`; omit it to keep the current password. Sending a new password or changing `enabled` revokes the account's existing tokens.

## Error responses

Errors use a consistent JSON shape:

```json
{
  "timestamp": "2026-01-01T00:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "student 99 was not found",
  "path": "/api/students/99",
  "fieldErrors": null
}
```

Validation errors also include `fieldErrors`. Status codes used:

- `400` invalid request or validation failure
- `401` missing, invalid, expired, or revoked token; bad login credentials
- `403` authenticated but not permitted for the resource
- `404` resource not found
- `409` duplicate username or conflicting data
- `415` unsupported content type
- `500` unexpected server error

## Configuration

| Environment variable | Purpose | Default |
| --- | --- | --- |
| `APP_PORT` | Host port exposed by Compose | `8080` |
| `POSTGRES_DB` | Database name | `university` |
| `POSTGRES_USER` | Database user | `university` |
| `POSTGRES_PASSWORD` | Database password | `university` |
| `JWT_SECRET` | HMAC signing secret, at least 32 bytes | development value |
| `JWT_EXPIRATION_MS` | Token lifetime in milliseconds | `3600000` |
| `BOOTSTRAP_ENABLED` | Create missing demo accounts on startup | `true` |
| `BOOTSTRAP_ADMIN_USERNAME` | Bootstrap admin username | `admin` |
| `BOOTSTRAP_ADMIN_PASSWORD` | Bootstrap admin password | `ChangeMe123!` |
| `BOOTSTRAP_TEACHER_USERNAME` | Bootstrap teacher username | `teacher` |
| `BOOTSTRAP_TEACHER_PASSWORD` | Bootstrap teacher password | `ChangeMe123!` |
| `BOOTSTRAP_STUDENT_USERNAME` | Bootstrap student username | `student` |
| `BOOTSTRAP_STUDENT_PASSWORD` | Bootstrap student password | `ChangeMe123!` |

## Security notes

- Use a strong, random `JWT_SECRET` and strong bootstrap passwords in any non-local environment.
- Set `BOOTSTRAP_ENABLED=false` after initial provisioning if accounts are managed elsewhere.
- JWTs are stateless, so CSRF protection is disabled in favor of the explicit `Authorization: Bearer` header. Do not switch to cookie-based authentication without enabling CSRF protection.
- Passwords are never returned by the API.
- This is a demonstration application; it does not include rate limiting, account lockout, email verification, or a refresh-token flow.

## Project structure

```text
src/main/java/com/example/university
├── admin/       Admin REST resource
├── auth/        Login, logout, current user, user details
├── config/      Bootstrap account initialization
├── error/       JSON error model and exception handling
├── security/    JWT filter, password encoding, Spring Security config
├── student/     Student REST resource
├── teacher/     Teacher REST resource
└── user/        User entity, role, repository, service, and DTOs
```
