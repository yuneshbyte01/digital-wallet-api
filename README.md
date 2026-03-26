# 💳 Digital Wallet & Money Transfer API

> Built a production-style digital wallet API with double-entry ledger design, idempotency-safe transfers, and fraud rate-limiting — handling end-to-end transaction lifecycle from deposit to peer-to-peer transfer.

![Java](https://img.shields.io/badge/Java-25-orange?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.4-brightgreen?style=flat-square)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue?style=flat-square)
![Docker](https://img.shields.io/badge/Docker-ready-2496ED?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)

---

## 📌 Overview

The Digital Wallet & Money Transfer API is a production-style fintech backend built with Spring Boot 4, simulating a complete digital wallet system as used by real payment platforms like eSewa, Khalti, and IME Pay. It features double-entry ledger accounting, idempotency-safe peer-to-peer transfers, JWT-based authentication with refresh token revocation, role-based access control, and fraud rate-limiting.

### What makes this fintech-grade

- **Double-entry bookkeeping** — every transaction creates exactly one ledger row carrying both debit and credit sides. Balance = `SUM(credits) − SUM(debits)`. Prevents balance drift permanently.
- **Idempotency keys** — every transfer carries a client-generated UUID. Network retries return the original result — duplicate charges are structurally impossible.
- **Atomic transactions** — `@Transactional` wraps both sides of every transfer. Either both debit and credit succeed, or both roll back. Partial transfers cannot occur.
- **Refresh token revocation** — refresh tokens are stored as SHA-256 hashes. Logout revokes the stored token immediately, preventing reuse of stolen tokens.
- **Self-transfer prevention** — the transfer service explicitly validates that sender and receiver wallets differ before performing any PIN or balance checks.
- **Optimistic locking** — a `@Version` field on the `Wallet` entity prevents concurrent write conflicts. Mapped to HTTP 409.
- **KYC lifecycle** — a dedicated `kyc_status` column (PENDING / VERIFIED / REJECTED) tracks document verification state.
- **Transaction PIN** — separate from login password. BCrypt-hashed at strength 12. Account locks after three failed attempts.
- **Rate limiting** — five transfers per minute per user, ten login attempts per minute per IP. Returns HTTP 429 with `Retry-After` header.
- **HTTPS enforcement** — security configuration requires a secure channel and sets the HSTS header.
- **Compliance audit log** — every sensitive action persisted with full context for compliance officer review.

---

## 🛠 Tech Stack

| Dependency | Version | Reason |
|---|---|---|
| Spring Boot | 4.0.4 | Latest release. Supports Java 25, improved observability. |
| Java | 25 | Virtual threads reduce I/O-bound blocking. Records provide clean immutable DTOs. |
| Spring Security | 7.0 | Stateless JWT session management. Method-level `@PreAuthorize`. HSTS enforcement. |
| Spring Data JPA | 4.x | Hibernate 7 ORM. Custom `@Query` for ledger SUM aggregations. |
| PostgreSQL | 17 | ACID-compliant. Native UUID PKs. Row-level locking for concurrent transfers. |
| Flyway | 11 | Versioned sequential migrations. `ddl-auto=validate` in all environments. |
| JJWT | 0.12.6 | HS256 JWT signing. Access tokens 15 min. Refresh tokens 7 days, stored as hashes. |
| Bucket4j | 8.x | Token bucket algorithm for rate limiting. In-memory for single-instance. |
| BCrypt | Spring built-in | Password and PIN hashing. Strength 12. Refresh tokens as SHA-256 hashes. |
| MapStruct | 1.6.3 | Compile-time DTO-to-entity mapping. Zero reflection. Fail-fast at build time. |
| SpringDoc OpenAPI | 2.8.6 | Swagger UI auto-generated at `/swagger-ui/index.html`. JWT bearer scheme configured. |
| Lombok | 1.18.x | `@Builder`, `@RequiredArgsConstructor`. Used sparingly on JPA entities. |
| JUnit 5 + Mockito | Spring built-in | Unit tests for service layer. Repositories mocked. |
| Testcontainers | 1.20.4 | Real PostgreSQL container in integration tests. No H2 anywhere. |
| Docker + Compose | — | Application, PostgreSQL, and optional Redis in a single `docker-compose.yml`. |

---

## 📁 Project Structure

Feature-based packages — never layer-based. Each feature is self-contained with its own entity, repository, service, controller, and DTOs.

```
com.yunesh.digitalwallet/
├── config/         # SecurityConfig, JwtConfig, RateLimitConfig
├── auth/           # AuthController, AuthService, JwtService, JwtAuthenticationFilter
│                   # RefreshToken entity, TokenRefreshService
├── user/           # User entity, UserService, UserController, KYC/PIN DTOs
├── wallet/         # Wallet entity, WalletService, WalletController, DepositRequest
├── ledger/         # LedgerEntry entity, LedgerRepository (balance SUM query), LedgerService
├── transfer/       # Transfer entity, TransferService (9-step engine), TransferLimitService
├── audit/          # AuditLog entity, AuditService (@Async), AuditController (compliance)
├── ratelimit/      # RateLimitFilter, RateLimitBucketService (Bucket4j)
├── statement/      # Statement entity, StatementService, StatementController
├── scheduler/      # StatementGeneratorJob, FraudFlagJob (@Scheduled)
├── exception/      # GlobalExceptionHandler, all custom exceptions
└── common/         # ApiResponse<T>, ErrorResponse, ValidationErrorResponse, AppConstants
```

---

## 🗄 Database Schema

### Flyway migration sequence

| Version | File | Description |
|---|---|---|
| V1 | `V1__create_users_table.sql` | Users with KYC fields, PIN, role, status |
| V2 | `V2__create_wallets_table.sql` | Wallets with `@Version` for optimistic locking |
| V3 | `V3__create_ledger_entries_table.sql` | Core financial table — single row per transaction |
| V4 | `V4__create_transfers_table.sql` | Transfer records with idempotency key |
| V5 | `V5__create_audit_logs_table.sql` | Audit log with JSONB metadata |
| V6 | `V6__create_statements_table.sql` | Monthly statement records |
| V7 | `V7__add_indexes.sql` | Indexes on all FK columns and hot query columns |
| V8 | `V8__create_refresh_tokens_table.sql` | Hashed refresh tokens with revocation flag |

> **Key design decisions**
>
> - The `wallets` table has **no balance column**. Balance is always computed: `SUM(amount WHERE credit_wallet_id = :id) − SUM(amount WHERE debit_wallet_id = :id)`
> - All monetary columns use `NUMERIC(19,4)` — never `float` or `double`
> - All PKs are UUID with `DEFAULT gen_random_uuid()`
> - `ddl-auto=validate` in all environments — Flyway owns the schema, Hibernate only validates

---

## 🔌 API Endpoints

All paths use the `/api/v1/` prefix. Every response follows the `ApiResponse<T>` envelope.

### Public (no token required)

| Method | Path | Description | Response |
|---|---|---|---|
| POST | `/auth/register` | Register new user | 201 + user details |
| POST | `/auth/login` | Login with email + password | 200 + token pair |
| POST | `/auth/refresh` | Rotate refresh token | 200 + new token pair |
| POST | `/auth/logout` | Revoke refresh token | 204 |

### USER role

| Method | Path | Description | Response |
|---|---|---|---|
| GET | `/users/me` | Get own profile | 200 + UserProfileResponse |
| PUT | `/users/me` | Update name and phone | 200 + UserProfileResponse |
| POST | `/users/me/kyc` | Submit KYC document | 202 |
| POST | `/users/me/pin` | Set transaction PIN | 204 |
| GET | `/wallets/me` | Get wallet with computed balance | 200 + WalletResponse |
| POST | `/wallets/deposit` | Deposit funds | 200 + WalletResponse |
| GET | `/wallets/me/ledger-entries` | Paginated ledger history | 200 + Page |
| POST | `/transfers` | Initiate P2P transfer | 200 + TransferResponse |
| GET | `/transfers` | Own transfer history | 200 + Page |
| GET | `/transfers/{id}` | Single transfer detail | 200 + TransferResponse |
| GET | `/statements/me` | List own statements | 200 + List |
| POST | `/statements/me/generate` | Generate statement for month/year | 200 + StatementResponse |
| GET | `/statements/{year}/{month}` | Download specific statement | 200 + StatementResponse |

### ADMIN role

| Method | Path | Description | Response |
|---|---|---|---|
| GET | `/admin/users` | All users paginated | 200 + Page |
| PUT | `/admin/users/{id}/lock` | Toggle account lock | 204 |
| PUT | `/admin/users/{id}/kyc-status` | Set VERIFIED or REJECTED | 204 |
| PUT | `/admin/wallets/{id}/freeze` | Toggle wallet freeze | 200 + WalletResponse |
| PUT | `/admin/transfers/{id}/reversal` | Reverse completed transfer | 200 + TransferResponse |
| GET | `/admin/statements/{userId}` | View any user's statements | 200 + List |

### COMPLIANCE_OFFICER role

| Method | Path | Description | Response |
|---|---|---|---|
| GET | `/compliance/audit-logs` | All audit events paginated | 200 + Page |
| GET | `/compliance/flagged-accounts` | Fraud-flagged accounts | 200 + List |

---

## 🔄 Transfer Flow

The entire `TransferService.executeTransfer()` method is wrapped in `@Transactional`. Any failure at any step rolls back all database changes.

1. **Idempotency check** — if `idempotency_key` already exists, return original result immediately
2. **Self-transfer guard** — resolve wallets, throw `SelfTransferException` (400) if IDs match. Must be before PIN check — never waste a PIN attempt on a structurally invalid request
3. **Account status check** — if sender `status != ACTIVE` throw `AccountLockedException` (423)
4. **PIN verification** — `BCrypt.matches()`. On failure increment `pin_attempts`. If `>= 3` set `status = LOCKED`
5. **Balance check** — `LedgerService.computeBalance()`. If `balance < amount` throw `InsufficientFundsException` (400)
6. **Daily limit check** — `TransferLimitService`. If `sum + amount > NPR 100,000` throw `DailyLimitExceededException` (400)
7. **Persist Transfer** — status `PENDING`
8. **Create ledger entry** — `LedgerService.createEntryPair()`. Single row carrying debit and credit sides
9. **Mark Transfer COMPLETED** — set `completed_at`, write audit log, return `TransferResponse`

---

## 🔒 Security

### JWT
- Secret from `${JWT_SECRET}` env var — minimum 256-bit, base64-encoded, never hardcoded
- Access token expiry: 900,000ms (15 minutes)
- Refresh token expiry: 604,800,000ms (7 days)
- Raw refresh token is a UUID string. Only SHA-256 hash stored in DB
- On `/auth/refresh`: validate hash, delete old row, insert new row (rotation)
- On `/auth/logout`: set `revoked = true`

### PIN
- BCrypt strength 12. Never stored plaintext. Never logged.
- Account locks after `MAX_PIN_ATTEMPTS = 3` failures
- Requires admin intervention to unlock

### Rate limiting
- Transfers: 5 requests per 60 seconds per user (by email)
- Login: 10 requests per 60 seconds per IP
- Returns HTTP 429 with `Retry-After: 60` header

### HTTPS
- `requiresChannel()` reads `X-Forwarded-Proto` header from reverse proxy
- HSTS with `includeSubDomains=true`, `maxAgeInSeconds=31536000`

### RBAC
- Route-level: `/admin/**` → ADMIN only, `/compliance/**` → COMPLIANCE_OFFICER only
- Filter chain order: HTTPS enforcement → rate limit → JWT authentication → authorization

---

## ✅ Prerequisites

- Java 21+
- Maven 3.9+
- Docker and Docker Compose
- PostgreSQL 17 (for local dev without Docker)

---

## 🔧 Environment Variables

| Variable | Example | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/digital_wallet_db` | Full JDBC connection string |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | `strongpassword` | Database password — never commit |
| `JWT_SECRET` | `K7x2mQ9vN3pL8wR...==` | Base64-encoded 256-bit key for HS256 signing |
| `REDIS_HOST` | `redis` | Only required when Redis Compose profile is active |

Generate a secure JWT secret:
```powershell
$bytes = New-Object byte[] 32
[Security.Cryptography.RNGCryptoServiceProvider]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

---

## 🚀 Local Setup

**1. Clone the repository**
```bash
git clone https://github.com/yunesh/digital-wallet-api.git
cd digital-wallet-api
```

**2. Create `.env` file**
```env
DB_URL=jdbc:postgresql://localhost:5432/digital_wallet_db
DB_USERNAME=postgres
DB_PASSWORD=your_password
JWT_SECRET=your_base64_secret
```

**3. Start PostgreSQL**
```bash
docker run --name wallet-db \
  -e POSTGRES_DB=digital_wallet_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=your_password \
  -p 5432:5432 \
  -d postgres:17-alpine
```

**4. Run the application**
```bash
mvn spring-boot:run
```

Flyway runs all 8 migrations automatically on startup.

**5. Insert system wallet** (required for deposits)
```sql
INSERT INTO wallets (id, user_id, currency, status, version)
VALUES (
    '00000000-0000-0000-0000-000000000000',
    (SELECT id FROM users WHERE email = 'admin@example.com'),
    'SYSTEM', 'ACTIVE', 0
);
```

**6. Insert admin user**
```sql
INSERT INTO users (full_name, email, phone, password_hash, role, status, kyc_status)
VALUES (
    'Admin User', 'admin@example.com', '+9779800000001',
    '$2a$12$...bcrypt_hash_of_your_password...',
    'ADMIN', 'ACTIVE', 'VERIFIED'
);
```

---

## 🐳 Docker

**Standard setup**
```bash
docker-compose up --build
```

**With Redis** (for multi-instance Bucket4j rate limiting)
```bash
docker-compose --profile redis up --build
```

Required env vars for Docker:
```bash
export DB_PASSWORD=strongpassword
export JWT_SECRET=your_base64_secret
```

---

## 🧪 Testing

Two-layer test strategy — fast unit tests for business logic, real-database integration tests via Testcontainers.

```bash
# Unit tests only (no Docker required)
mvn test -Dtest="*ServiceTest"

# All tests (Docker required for Testcontainers)
mvn test
```

| Layer | Target | Tool |
|---|---|---|
| Service layer (unit) | > 90% line coverage | JUnit 5 + Mockito |
| Controller layer (integration) | > 80% endpoint coverage | MockMvc + Testcontainers |
| Repository layer | Covered by integration tests | — |

> No H2 anywhere. Integration tests use a real PostgreSQL 17 container via Testcontainers. `AbstractIntegrationTest` starts a shared container once per test run — all test classes extend it.

---

## 📖 API Documentation

Swagger UI is available at:
```
http://localhost:8080/swagger-ui/index.html
```

Configure JWT bearer authentication in Swagger UI:
1. Click **Authorize**
2. Enter `Bearer your_access_token`
3. Click **Authorize**

---

## 📊 HTTP Status Code Reference

| Code | When used |
|---|---|
| 200 | GET, PUT success. Login, token refresh. |
| 201 | POST /auth/register, POST /wallets/deposit |
| 204 | Logout, lock/unlock, PIN set |
| 400 | Insufficient funds, self-transfer, daily limit, validation failure |
| 401 | Missing, expired, or invalid JWT |
| 403 | Valid JWT but insufficient role |
| 404 | Transfer, user, or wallet not found |
| 409 | Duplicate idempotency key, optimistic lock failure |
| 423 | Account locked after three failed PIN attempts |
| 429 | Rate limit exceeded — includes `Retry-After` header |
| 500 | Unhandled exception — stack traces never exposed |

---

## 👤 Author

**Yunesh Timsina**
yuneshtimsina@gmail.com

---

*Digital Wallet & Money Transfer API — v1.0.0 — March 2026*