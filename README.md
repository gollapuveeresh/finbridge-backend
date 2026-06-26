# FinBridge — Spring Boot Backend

## Stack
- Spring Boot 3.2.5
- Spring Data JPA + Hibernate
- Spring Security (stateless JWT, self-issued)
- Supabase PostgreSQL
- Flyway (auto schema migration on startup, `ddl-auto: validate`)

## Configuration

All secrets are read from environment variables. `application.yml` provides **local-dev fallbacks
only** (after the `:` in each `${VAR:fallback}`); never rely on them in a deployed environment.

| Variable | Purpose |
|----------|---------|
| `DB_URL` | JDBC URL, e.g. `jdbc:postgresql://db.<ref>.supabase.co:5432/postgres` |
| `DB_USERNAME` | Database user (default `postgres`) |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | HMAC signing secret for app-issued JWTs (**min 32 chars**) |
| `JWT_EXPIRATION_MS` | Token lifetime (default 86400000 = 24h) |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | SMTP credentials for reminder emails (optional) |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed frontend origins |
| `FRONTEND_URL` | Base URL used in outbound links |

PowerShell example:
```powershell
$env:DB_PASSWORD = '...'; $env:JWT_SECRET = '...'; ./mvnw spring-boot:run
```

> ⚠️ **Rotate the committed DB password.** A development DB password is checked into
> `application.yml` git history as a fallback. Before any real deployment: rotate the
> Supabase database password (Dashboard → Settings → Database → Reset password), set the
> new value via `DB_PASSWORD`, and likewise override `JWT_SECRET`. The committed fallbacks
> must be treated as compromised.

### Run
```bash
./mvnw spring-boot:run
```
Runs on: http://localhost:5000 — Swagger UI at `/swagger-ui.html`.

## Auth & Authorization
```
React → POST /api/auth/login → app-issued JWT (signed with JWT_SECRET)
     → Spring Boot API (Authorization: Bearer <token>)
     → JwtAuthFilter validates the token and loads the User from the DB
     → @PreAuthorize role gate (SecurityRoles) + record-level OwnershipGuard
```

**Two authorization layers:**
1. **Role-level** — `@PreAuthorize` on controllers (`SecurityRoles.STAFF` / `ADMINS` /
   `ADMIN_OR_DEPT`) restricts whole endpoints by role.
2. **Owner-level** — `OwnershipGuard` restricts a *consultant* to records they own
   (loan cases, dept cases, invoices, payments); admin-tier roles retain cross-cutting access.
   This closes the IDOR gap where a consultant could mutate a peer's record by ID.

## API Endpoints

| Method | URL | Description |
|--------|-----|-------------|
| GET | /api/health | Health check (public) |
| GET | /api/auth/me | Current user |
| POST | /api/auth/register | Register user after Supabase signup |
| GET | /api/auth/consultants | List consultants |
| GET | /api/auth/users | All users (admin only) |
| POST | /api/leads/capture | Public lead capture form |
| GET | /api/leads | All leads |
| POST | /api/leads | Create lead |
| PATCH | /api/leads/:id | Update lead |
| POST | /api/leads/:id/notes | Add note to lead |
| POST | /api/leads/:id/convert | Convert lead to client |
| GET | /api/leads/stats | Pipeline stats |
| GET | /api/proposals | Get proposals |
| POST | /api/proposals | Create proposal |
| PATCH | /api/proposals/:id | Update proposal |
| PATCH | /api/proposals/:id/decision | Client approve/reject |
| GET | /api/loans | Get loans |
| POST | /api/loans | Create loan |
| PATCH | /api/loans/:id | Update loan |
| DELETE | /api/loans/:id | Soft delete loan |
| GET | /api/investments | Get investments |
| POST | /api/investments | Create investment |
| PATCH | /api/investments/:id | Update investment |
| DELETE | /api/investments/:id | Soft delete investment |
| GET | /api/consultations | Get consultations |
| POST | /api/consultations | Book consultation |
| PATCH | /api/consultations/:id | Update consultation |
| GET | /api/notifications | Get notifications |
| GET | /api/notifications/unread-count | Unread count |
| PATCH | /api/notifications/:id/read | Mark one read |
| PATCH | /api/notifications/read-all | Mark all read |
| GET | /api/financial-profile | Get financial profile |
| POST | /api/financial-profile | Create/update profile |
| GET | /api/dashboard | Dashboard stats |
| GET | /api/loan-cases | Loan cases (scoped by role/owner) |
| POST | /api/loan-cases | Create loan case |
| PATCH | /api/loan-cases/:id | Update stage / eligibility / recommendation |
| POST | /api/loan-cases/:id/disburse | Disburse + generate EMI schedule |
| GET | /api/dept-cases/:dept | Department cases (tax/investment/insurance/wealth) |
| GET | /api/invoices | List invoices (+ `/stats`) |
| POST | /api/invoices | Create invoice (subtotal + tax) |
| PATCH | /api/invoices/:id | Update status (paid → records a payment) |
| GET | /api/payments | List payments (+ `/stats`) |
| POST | /api/payments | Record a payment against an invoice & mark it paid |
