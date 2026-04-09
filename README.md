# 💳 Digital Wallet & Money Transfer API

> A production-grade fintech REST API built with Spring Boot — the backend engine that powers a digital wallet application similar to eSewa. Features double-entry ledger accounting, idempotency-safe transfers, JWT authentication with refresh token rotation, KYC verification, OTP-based password reset, and multi-step transfer flow with transaction codes.

![Java](https://img.shields.io/badge/Java-25-orange?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.4-brightgreen?style=flat-square)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue?style=flat-square)
![Flyway](https://img.shields.io/badge/Flyway-11-red?style=flat-square)
![Docker](https://img.shields.io/badge/Docker-ready-2496ED?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)

**Live API:** `https://digital-wallet-api-yumj.onrender.com`  
**Swagger UI:** `https://digital-wallet-api-yumj.onrender.com/swagger-ui/index.html`

---

## 📌 Overview

The Digital Wallet & Money Transfer API simulates the complete backend of a real payment platform — built from scratch, one migration at a time, with production patterns throughout.

### What makes this fintech-grade

- **Double-entry ledger** — the `wallets` table has no balance column. Ever. Balance is always computed as `SUM(credits) − SUM(debits)` from `ledger_entries`. A balance column can drift. A ledger never lies.
- **Idempotency keys** — every transfer carries a client-generated UUID. Network retries return the original result. Duplicate charges are structurally impossible.
- **Atomic transactions** — `@Transactional` wraps the full 9-step transfer engine. Both ledger entries succeed or both roll back. Partial transfers cannot occur.
- **Flexible login** — email + password or phone + PIN. Auto-detected from the identifier. Email cannot use PIN. Phone cannot use password. PIN is a mobile-first credential tied to money movement.
- **Temporary account lockout** — 3 failed attempts locks the account for 30 minutes. Auto-unlocks when the duration expires. No admin intervention needed.
- **Refresh token rotation** — raw token is a UUID. Only the SHA-256 hash is stored. On refresh, old token is deleted and a new one is issued. On logout, token is revoked immediately.
- **OTP-based reset** — forgot password sends OTP to email. Forgot PIN sends OTP to registered email. OTPs expire in 5 minutes and can only be used once.
- **Trusted email validation** — disposable and unknown email domains are rejected at registration. Only Gmail, Outlook, Yahoo, iCloud, and ProtonMail are accepted.
- **KYC lifecycle** — personal details, family information, marital status with conditional spouse fields, document upload, and admin review workflow with approval and rejection reason.
- **Multi-step transfer flow** — receiver lookup by phone or email shows name before proceeding. Transfer receipt includes unique transaction code, sender and receiver details, purpose, and remarks.
- **Admin transfer reversal** — completed transfers can be reversed. Creates two new REVERSAL ledger entries, preserving the originals.
- **File storage** — local folder in dev, Supabase Storage in prod. Profile photo on user record, document photo on KYC record.
- **Rate limiting** — 5 transfers per minute per user, 10 login attempts per minute per IP. Returns HTTP 429 with `Retry-After` header.

---

## 🛠 Tech Stack

| Dependency | Version | Reason |
|---|---|---|
| Spring Boot | 4.0.4 | Latest release, Java 25 support |
| Java | 25 | Records for clean immutable DTOs, modern language features |
| Spring Security | 7.0 | Stateless JWT, method-level `@PreAuthorize`, HSTS |
| Spring Data JPA | 4.x | Hibernate 7, custom `@Query` for ledger SUM aggregations |
| PostgreSQL | 17 | ACID-compliant, native UUID PKs, row-level locking |
| Flyway | 11 | Versioned migrations, `ddl-auto=validate` in all environments |
| JJWT | 0.12.6 | HS256 JWT signing, access 15 min, refresh 7 days |
| Bucket4j | 8.x | Token bucket algorithm for rate limiting |
| BCrypt | Spring built-in | Password and PIN hashing at strength 12, separate encoders |
| MapStruct | 1.6.3 | Compile-time DTO mapping, zero reflection |
| SpringDoc OpenAPI | 2.8.6 | Swagger UI auto-generated, JWT bearer configured |
| Lombok | 1.18.x | `@Builder`, `@RequiredArgsConstructor` |
| Testcontainers | 1.20.4 | Real PostgreSQL in integration tests, no H2 anywhere |
| Docker | — | Multi-stage Dockerfile for production build |

---

## 📁 Project Structure

Feature-based packages — never layer-based. Each feature owns its entity, repository, service, controller, and DTOs.

```
com.yunesh.digitalwallet/
├── config/          — SecurityConfig, JwtConfig, EncoderConfig, CorsConfig, StorageProperties
├── auth/            — AuthService, JwtService, TokenRefreshService, OtpService, PasswordResetService
│                      RefreshToken, OtpToken, AuthMapper
├── user/            — User, UserService, UserController, UserMapper
│                      Enums: UserRole, AccountStatus, KycStatus, Gender, MaritalStatus, KycDocumentType
├── wallet/          — Wallet, WalletService, WalletController
├── ledger/          — LedgerEntry, LedgerService (computeBalance, createEntryPair)
├── transfer/        — Transfer, TransferService (9-step engine), TransferLimitService
│                      TransactionCodeGenerator, TransferPurpose
├── kyc/             — KycDetail, KycService, KycController, AdminKycController, KycMapper
├── audit/           — AuditLog, AuditService (@Async), AuditAction enum
├── ratelimit/       — RateLimitFilter, RateLimitBucketService
├── storage/         — StorageService (interface), LocalStorageService (@Profile dev)
│                      SupabaseStorageService (@Profile prod), FileUploadService, FileUploadController
├── exception/       — GlobalExceptionHandler + all custom exceptions
└── common/          — ApiResponse<T>, ErrorResponse, ValidationErrorResponse, AppConstants
```

---

## 🗄 Database Schema

### Flyway migration sequence

| Version | File | Description |
|---|---|---|
| V1 | `V1__create_users_table.sql` | Users — email, phone, password, PIN, role, status |
| V2 | `V2__create_wallets_table.sql` | Wallets with `@Version` for optimistic locking |
| V3 | `V3__create_ledger_entries_table.sql` | Core financial table — debit and credit sides |
| V4 | `V4__create_transfers_table.sql` | Transfer records with idempotency key |
| V5 | `V5__create_audit_logs_table.sql` | Audit log with JSONB metadata |
| V6 | `V6__create_statements_table.sql` | Monthly statement records |
| V7 | `V7__add_indexes.sql` | Indexes on FK columns and hot query columns |
| V8 | `V8__create_refresh_tokens_table.sql` | Hashed refresh tokens with revocation flag |
| V9 | `V9__add_gender_to_users.sql` | Gender column on users |
| V10 | `V10__alter_users_for_new_schema.sql` | failed_login_attempts, account_locked_until, last_login_at |
| V11 | `V11__create_kyc_details_table.sql` | KYC — personal info, document, review workflow |
| V12 | `V12__add_profile_picture_to_users.sql` | Profile picture URL on users |
| V13 | `V13__create_otp_tokens_table.sql` | OTP tokens for password and PIN reset |
| V14 | `V14__alter_transfers_add_purpose_remarks.sql` | transaction_code, purpose, remarks on transfers |

> **Key design decisions**
>
> - `wallets` has **no balance column** — balance always computed from `ledger_entries`
> - All monetary columns use `NUMERIC(19,4)` — never `float` or `double`
> - All PKs are UUID with `DEFAULT gen_random_uuid()`
> - `ddl-auto=validate` in all environments — Flyway owns the schema, Hibernate only validates
> - Never edit an existing migration — every schema change is a new versioned file

---

## 🔌 API Endpoints

All paths use the `/api/v1/` prefix. Every response follows the `ApiResponse<T>` envelope.

### Auth (public)

| Method | Path | Description | Status |
|---|---|---|---|
| POST | `/auth/register` | Register with trusted email, phone, PIN, gender | 201 |
| POST | `/auth/login` | Login with email+password or phone+PIN | 200 |
| POST | `/auth/refresh` | Rotate refresh token | 200 |
| POST | `/auth/logout` | Revoke refresh token | 200 |
| POST | `/auth/forgot-password` | Send OTP to email | 200 |
| POST | `/auth/reset-password` | Verify OTP + set new password | 200 |
| POST | `/auth/forgot-pin` | Send OTP to registered email | 200 |
| POST | `/auth/reset-pin` | Verify OTP + set new PIN | 200 |

### User (authenticated)

| Method | Path | Description | Status |
|---|---|---|---|
| GET | `/users/me` | Get own profile | 200 |
| PUT | `/users/me/password` | Change password | 200 |
| PUT | `/users/me/pin` | Change PIN | 200 |
| POST | `/users/me/kyc` | Submit KYC details | 201 |
| GET | `/users/me/kyc` | Get own KYC status | 200 |
| POST | `/users/me/profile-photo` | Upload profile photo (multipart, max 5MB) | 200 |
| POST | `/users/me/kyc/document-photo` | Upload document photo (multipart, max 5MB) | 200 |

### Wallet (authenticated)

| Method | Path | Description | Status |
|---|---|---|---|
| GET | `/wallets/me` | Get wallet with computed balance | 200 |
| POST | `/wallets/deposit` | Deposit funds | 200 |

### Transfers (authenticated)

| Method | Path | Description | Status |
|---|---|---|---|
| POST | `/transfers/lookup` | Lookup receiver by phone or email | 200 |
| POST | `/transfers` | Execute transfer — returns full receipt | 201 |
| GET | `/transfers/me` | Own transfer history (paginated) | 200 |
| GET | `/transfers/{id}` | Single transfer by ID | 200 |

### Admin (ADMIN role)

| Method | Path | Description | Status |
|---|---|---|---|
| GET | `/admin/transfers` | All transfers paginated | 200 |
| POST | `/admin/transfers/{id}/reverse` | Reverse a completed transfer | 200 |
| GET | `/admin/kyc/pending` | Pending KYC list paginated | 200 |
| GET | `/admin/kyc/{userId}` | Get KYC for a user | 200 |
| PUT | `/admin/kyc/{userId}/review` | Approve or reject KYC | 200 |

---

## 🔄 Transfer Engine — Step Order

`TransferService.executeTransfer()` always follows this exact sequence. Steps never reorder.

1. **Idempotency check** — return existing if duplicate key
2. **Self-transfer guard** — resolve wallets, throw 400 if IDs match. Before PIN check — never waste a PIN attempt on a structurally invalid request
3. **Account status check** — throw 423 if sender not ACTIVE
4. **PIN verification** — BCrypt match. On failure increment `failed_login_attempts`. If `>= 3` lock account for 30 minutes
5. **Balance check** — `LedgerService.computeBalance()`. Throw 400 if insufficient
6. **Daily limit check** — NPR 100,000 per day, NPR 25,000 per transaction
7. **Persist Transfer** — status PENDING
8. **Create ledger entries** — `LedgerService.createEntryPair()`. Two rows, same idempotency key
9. **Mark Transfer COMPLETED** — set `completed_at`, generate `transactionCode`, write audit log, return receipt

---

## 🔒 Security

### Authentication
- **Password** — BCrypt strength 12, `passwordEncoder` bean
- **PIN** — BCrypt strength 12, separate `pinEncoder` bean via `EncoderConfig`
- **JWT access token** — 15 minutes, HS256, secret from `${JWT_SECRET}` env var — never hardcoded
- **Refresh token** — UUID stored as SHA-256 hash, 7 days, rotated on every use

### Lockout
- 3 failed login or PIN attempts → account locked for 30 minutes
- Auto-unlocks when `account_locked_until` expires
- Successful login resets counter and updates `last_login_at`

### OTP
- 6-digit secure random, 5-minute expiry, single-use
- Previous OTPs invalidated on new request

### Rate Limiting
- Transfers: 5 per 60 seconds per user
- Login: 10 per 60 seconds per IP
- Returns 429 with `Retry-After` header

### HTTPS + HSTS
- Enforced via `X-Forwarded-Proto` from reverse proxy
- HSTS: `includeSubDomains=true`, `maxAgeInSeconds=31536000`

### RBAC
- Route-level: `/admin/**` → ADMIN, `/compliance/**` → COMPLIANCE_OFFICER
- Filter chain: HTTPS → rate limit → JWT → authorization

---

## 📊 HTTP Status Reference

| Code | When used |
|---|---|
| 200 | Successful GET, login, token refresh, deposit |
| 201 | Register, transfer completed, KYC submitted |
| 400 | Insufficient funds, self-transfer, daily limit, validation failure, invalid OTP |
| 401 | Missing, expired, or invalid JWT. Bad credentials — message includes attempts remaining |
| 404 | User, wallet, or transfer not found |
| 409 | Duplicate idempotency key, email/phone already registered, optimistic lock failure |
| 423 | Account locked — message includes locked until timestamp |
| 429 | Rate limit exceeded — includes `Retry-After` header |
| 500 | Unhandled exception — stack trace never exposed |

---

## 🚀 Local Setup

### Prerequisites
- Java 21+
- PostgreSQL 17
- Maven 3.9+

### Steps

```bash
# 1. Clone
git clone https://github.com/yuneshbyte01/digital-wallet-api.git
cd digital-wallet-api

# 2. Create local database
psql -U postgres -c "CREATE DATABASE digital_wallet_db;"

# 3. Create .env
cp .env.example .env
# fill in DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET

# 4. Run
mvn spring-boot:run
```

Flyway runs all 14 migrations automatically on startup.

**Swagger UI:** `http://localhost:8080/swagger-ui/index.html`

### Generate JWT secret

```powershell
# Windows PowerShell
$bytes = New-Object byte[] 32
[Security.Cryptography.RNGCryptoServiceProvider]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

```bash
# Linux / macOS
openssl rand -base64 32
```

### Seed admin user

Register via API then promote in SQL:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'admin@gmail.com';

INSERT INTO wallets (id, user_id, currency, status, version)
VALUES ('00000000-0000-0000-0000-000000000000',
    (SELECT id FROM users WHERE email = 'admin@gmail.com'),
    'SYSTEM', 'ACTIVE', 0);
```

---

## 🔧 Environment Variables

| Variable | Purpose |
|---|---|
| `DB_URL` | JDBC connection string |
| `DB_USERNAME` | Database user |
| `DB_PASSWORD` | Database password — never commit |
| `JWT_SECRET` | Base64-encoded 256-bit signing key |
| `SPRING_PROFILES_ACTIVE` | `dev` or `prod` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins |
| `SUPABASE_URL` | Supabase project URL (prod only) |
| `SUPABASE_SERVICE_KEY` | Supabase service role key (prod only) |
| `SUPABASE_BUCKET` | Storage bucket name (prod only) |

---

## 🗂 File Storage

| Profile | Implementation | Location |
|---|---|---|
| `dev` | `LocalStorageService` | `uploads/` folder on device, served at `/uploads/` |
| `prod` | `SupabaseStorageService` | Supabase Storage bucket via REST API |

- Max file size: 5MB
- Allowed types: JPEG, PNG, PDF
- Profile photo stored on `users.profile_picture_url`
- KYC document photo stored on `kyc_details.document_picture_path`

---

## 🧪 Testing

```bash
# All tests (Docker required for Testcontainers)
mvn test

# Unit tests only
mvn test -Dtest="*ServiceTest"
```

| Layer | Tool | Target |
|---|---|---|
| Service (unit) | JUnit 5 + Mockito | > 90% coverage |
| Controller (integration) | MockMvc + Testcontainers | > 80% coverage |

> No H2 anywhere. Integration tests use real PostgreSQL 17 via Testcontainers. All test classes extend `AbstractIntegrationTest` which starts a shared container once per test run.

---

## 🚢 Deployment

| Service | Provider | Notes |
|---|---|---|
| Application | Render (Docker) | Free tier, auto-deploys on push to main |
| Database | Supabase PostgreSQL | Session pooler, port 5432, SSL required |
| File storage | Supabase Storage | Private bucket, accessed via service role key |

---

## 👤 Author

**Yunesh Timsina**  
GitHub: [@yuneshbyte01](https://github.com/yuneshbyte01)

---

*Digital Wallet & Money Transfer API — v1.0.0 — April 2026*