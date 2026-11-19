# TaskFlow Backend — Phase 1

Java 21, Spring Boot 3.4.5, PostgreSQL, Liquibase, Spring Security, JWT and Spring Data JPA.

## 1. Prerequisites
- JDK 21
- Maven 3.9+
- PostgreSQL 14+
- IntelliJ IDEA

## 2. Database
Run as a PostgreSQL administrator:

```sql
CREATE DATABASE taskflow_db;
CREATE USER taskflow_user WITH PASSWORD 'CHANGE_THIS_LOCAL_PASSWORD';
GRANT CONNECT ON DATABASE taskflow_db TO taskflow_user;
```

Connect to `taskflow_db`, then run:

```sql
GRANT USAGE, CREATE ON SCHEMA public TO taskflow_user;
ALTER SCHEMA public OWNER TO taskflow_user;
```

Set the password in PowerShell before starting the app:

```powershell
$env:DB_PASSWORD="CHANGE_THIS_LOCAL_PASSWORD"
```

Optional: set a strong JWT secret (Base64-encoded, at least 32 decoded bytes):

```powershell
$env:TASKFLOW_JWT_SECRET="REPLACE_WITH_BASE64_ENCODED_32_BYTE_OR_LONGER_SECRET"
```

## 3. Run
From the project root:

```powershell
mvn clean verify
mvn spring-boot:run
```

Swagger UI: http://localhost:8080/swagger-ui/index.html

Health: http://localhost:8080/actuator/health

## 4. Authentication flow
- POST `/api/v1/auth/register`
- POST `/api/v1/auth/login`
- Send `Authorization: Bearer <accessToken>` to protected endpoints.
- POST `/api/v1/auth/refresh` with `{ "refreshToken": "..." }` to rotate the refresh token.
- POST `/api/v1/auth/logout` with `{ "refreshToken": "..." }` to revoke it.

Refresh tokens are opaque random values. Only SHA-256 hashes are persisted. This starter returns the refresh token in the response body for a simple API demo. Before a browser production deployment, move refresh-token transport to a Secure, HttpOnly, SameSite cookie and implement CSRF protection for cookie-authenticated operations.

## 5. API summary

### Auth
- POST `/api/v1/auth/register`
- POST `/api/v1/auth/login`
- POST `/api/v1/auth/refresh`
- POST `/api/v1/auth/logout`
- GET `/api/v1/users/me`

### Tasks
- POST `/api/v1/tasks`
- GET `/api/v1/tasks`
- GET `/api/v1/tasks/{id}`
- PUT `/api/v1/tasks/{id}`
- PATCH `/api/v1/tasks/{id}/status`
- DELETE `/api/v1/tasks/{id}` (soft delete)

Example filters:
`GET /api/v1/tasks?status=TODO&priority=HIGH&search=interview&page=0&size=10&sortBy=createdAt&sortDirection=DESC`

Allowed sort fields: `createdAt`, `updatedAt`, `dueDate`, `priority`, `title`.

### Categories
- POST `/api/v1/categories`
- GET `/api/v1/categories`
- PUT `/api/v1/categories/{id}`
- DELETE `/api/v1/categories/{id}`

### Dashboard
- GET `/api/v1/dashboard/summary`

## 6. Important implementation notes
- User identity comes from the authenticated JWT subject, never from request payloads.
- Task and category queries are scoped by authenticated user ID.
- Liquibase owns schema changes. Hibernate uses `ddl-auto=validate`.
- `@Version` provides optimistic locking; update requests include the version returned by the API.
- Overdue is derived from due date and current status, not persisted as a task status.
- State transitions: TODO -> IN_PROGRESS/COMPLETED/CANCELLED; IN_PROGRESS -> TODO/COMPLETED/CANCELLED; COMPLETED -> TODO; CANCELLED -> TODO.
- Do not edit an already-applied Liquibase changeset. Add a new changeset.

## 7. Important production hardening before deployment
This is a runnable interview-project baseline, not a security-certified production deployment. Before production: use separate migration/runtime database identities; use HTTPS and secret management; move refresh tokens to secure HttpOnly cookies with CSRF protection for browser flows; add rate limiting and account lockout controls; add audit events and observability; review retention and privacy requirements; add integration and concurrency tests.
