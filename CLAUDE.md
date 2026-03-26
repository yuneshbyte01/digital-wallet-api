# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

**Build:**
```bash
./mvnw clean package -DskipTests
```

**Run:**
```bash
./mvnw spring-boot:run
```

**Run all tests:**
```bash
./mvnw test
```

**Run a single test class:**
```bash
./mvnw test -Dtest=ClassName
```

**Run a single test method:**
```bash
./mvnw test -Dtest=ClassName#methodName
```

The app starts on port 8080. Swagger UI is available at `/swagger-ui.html`; OpenAPI JSON at `/v3/api-docs`.

## Environment

The app uses `spring-dotenv` to load a `.env` file at the project root. Required variables:

```
DB_URL=jdbc:postgresql://localhost:5432/digital_wallet_db
DB_USERNAME=postgres
DB_PASSWORD=<password>
JWT_SECRET=<base64-encoded-secret>
```

`application.yaml` has fallback defaults for `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`, but `JWT_SECRET` should be set properly. JWT access tokens expire in 15 minutes (900 000 ms); refresh tokens in 7 days.

## Architecture

**Stack:** Spring Boot 4.0 / Java 21, Spring Security (stateless JWT), Spring Data JPA, PostgreSQL, Flyway migrations, MapStruct, Lombok, springdoc-openapi.

**Package layout** — each domain owns its own slice (entity, repository, service, controller, DTOs, mapper):

| Package | Responsibility |
|---|---|
| `auth` | Registration, login, logout, JWT issuance, refresh-token lifecycle |
| `user` | Profile management, KYC submission, PIN management |
| `wallet` | Wallet CRUD, deposit, admin freeze/unfreeze |
| `ledger` | Double-entry ledger; balance is always computed from entries, never stored |
| `transfer` | Peer-to-peer transfers with idempotency, PIN verification, limit enforcement |
| `admin` | Admin-only endpoints (wallet freeze toggle) |
| `config` | `SecurityConfig`, `JwtConfig` |
| `common` | `ApiResponse`, `ErrorResponse`, `ValidationErrorResponse`, `AppConstants` |
| `exception` | Domain exceptions + `GlobalExceptionHandler` |

**Database schema** is managed exclusively via Flyway migrations in `src/main/resources/db/migration/`. Hibernate DDL is set to `validate` — never let Hibernate modify the schema.

## Key design decisions

**Ledger-based balance:** Wallet balances are never stored as a column. `LedgerService.computeBalance(walletId)` aggregates debit/credit entries via a repository query. Every deposit and transfer creates a double-entry pair via `LedgerService.createEntryPair(debitWalletId, creditWalletId, amount, type, idempotencyKey)`.

**System wallet:** Deposits credit the user's wallet from a special system wallet at `UUID 00000000-0000-0000-0000-000000000000`. This wallet must exist in the database.

**Transfer flow** (`TransferService.executeTransfer`): idempotency check → resolve sender/receiver → self-transfer guard → account/wallet status check → PIN verification (locks account after `AppConstants.Security.MAX_PIN_ATTEMPTS = 3` failures) → balance check → daily limit check → persist `PENDING` transfer → create ledger entry pair → mark `COMPLETED`.

**Idempotency:** Both transfers and ledger entries accept an idempotency key (UUID). Duplicate requests return the existing result rather than re-processing.

**Transfer limits** (defined in `AppConstants.Transfer`):
- Single transfer max: NPR 25,000
- Daily total max: NPR 100,000
- Minimum amount: NPR 10

**Security:**
- Stateless JWT; `JwtAuthenticationFilter` validates tokens on every request.
- `AuthService` implements `UserDetailsService`; uses `@Lazy` injection in `SecurityConfig` to avoid circular dependency.
- HTTPS enforced via `X-Forwarded-Proto` header check (proxy-aware).
- Role-based access: `/api/v1/admin/**` requires `ROLE_ADMIN`; `/api/v1/compliance/**` requires `ROLE_COMPLIANCE_OFFICER`; all other endpoints require authentication.
- PIN is stored as a BCrypt hash (`pinHash`), separate from the login password hash.

**Response envelope:** All success responses use `ApiResponse<T>`; errors use `ErrorResponse`; validation failures use `ValidationErrorResponse` with per-field detail. All handled in `GlobalExceptionHandler`.

**MapStruct mappers** (`UserMapper`, `WalletMapper`, `LedgerMapper`, `TransferMapper`) are the only place where entities are converted to response DTOs. Lombok and MapStruct annotation processors are both configured in the Maven compiler plugin — order matters (Lombok must be listed first).