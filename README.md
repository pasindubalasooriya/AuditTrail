# AuditTrail

A production-style **payment-gateway compliance audit system**: every sensitive action is recorded
with a **cryptographically verified identity**, and audit records **physically cannot be altered or
deleted** - enforced at the database, not just in application code.

Built to demonstrate hands-on WSO2 Identity Server, Spring Boot, OAuth2/JWT, and PostgreSQL.

---

## The problem it solves

When something goes wrong in a payment gateway - a fraudulent transaction approved, an unjustified
refund, a merchant onboarded without checks - regulators ask: *who authorised this, when, and under
what role?* Real audit findings recur because logs are missing, tied to **shared admin accounts**,
or **editable after the fact**. AuditTrail closes those gaps:

- **Identity from a signed token, not a request field** - you can only act as `sara` by
  authenticating as `sara`; the identity is extracted from a JWT signed by WSO2, never user-supplied.
- **Database-level immutability** - a PostgreSQL trigger blocks `UPDATE`/`DELETE` on the audit log,
  and the application's DB user isn't even *granted* those privileges (defense in depth).
- **Role-based separation of duties** - a fraud analyst can flag transactions but not pull
  compliance reports; a compliance officer, vice-versa.
- **Versioned schema** - every structural change is a Flyway migration committed to git.

## Tech stack

| Layer | Technology |
|---|---|
| Identity Provider | WSO2 Identity Server 7.3 (runs on Java 21) |
| Frontend | React 18, Vite, Tailwind CSS, react-oidc-context (OIDC Auth Code + PKCE) |
| Backend | Java 17 (Amazon Corretto), Spring Boot 3.5.16, Maven |
| Security | Spring Security OAuth2 Resource Server (JWT via WSO2 JWKS) |
| Persistence | Spring Data JPA / Hibernate, `ddl-auto=none` |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Tests | JUnit 5, Mockito, Spring Security Test, Testcontainers |

## Architecture

```
Browser ──Auth Code + PKCE──▶ WSO2 Identity Server (:9443)  ← issues tokens, holds users/roles
   │                                     │
   │  React SPA (:5173)                  │ JWKS (signature verification)
   ▼                                     ▼
access_token (Bearer) ───────▶ Spring Boot API (:8080) ──▶ PostgreSQL (:5432)
```

The SPA authenticates the user against WSO2 and receives an id_token (for the browser session)
and an access_token (sent as the API bearer). The resource server validates the access_token's
signature against WSO2's JWKS and reads `sub`/`roles` from its claims.

Backend layout (`backend/src/main/java/com/audittrail/`): `controller` → `service` → `repository`
→ `model`, with `dto`, `exception`, and `config` (security). Frontend layout (`frontend/src/`):
`pages` (Login, Dashboard) → `components` (NavBar, SummaryCards, Actions, HighRiskPanel,
EventsTable), with `auth` (OIDC config) and `api` (backend client).

---

## Security model

**1. Verified identity.** The client sends the OIDC **access_token** as the bearer credential
(standard OAuth2 usage - the id_token authenticates the user to the SPA, the access_token
authorizes API calls). WSO2's app is configured to issue JWT access tokens (RFC 9068,
`typ: at+jwt`) carrying the same `sub`/`roles` claims as the id_token. The resource server
validates the signature against WSO2's JWKS, checks the issuer and expiry, and reads the
username from `sub`. `performedBy` on every record comes from this token - impossible to forge.

**2. Immutability - two independent layers.**
- *Privilege layer:* the app connects as `audittrail_app`, granted only `SELECT, INSERT` - so
  `UPDATE`/`DELETE` fail with `permission denied` before anything else runs.
- *Trigger layer:* `V2__add_immutable_log_trigger.sql` raises an exception on any `UPDATE`/`DELETE`,
  so even a privileged role cannot tamper.

**3. RBAC.** Roles `FRAUD_ANALYST` / `COMPLIANCE_OFFICER` arrive in a `roles` claim; a custom
converter maps them to Spring authorities, and `@PreAuthorize` guards each endpoint.

---

## Getting started

### Prerequisites
Java 17 (Corretto) and Java 21 (for WSO2), PostgreSQL 16, Docker (for integration tests),
WSO2 Identity Server 7.3, Node.js (for the frontend). No global Maven needed - use the
`./mvnw` wrapper.

### 1. Database + least-privilege user
```sql
-- as postgres superuser
CREATE DATABASE audittrail_db;
CREATE ROLE audittrail_app LOGIN PASSWORD '<app-password>';
GRANT CONNECT ON DATABASE audittrail_db TO audittrail_app;
\c audittrail_db
GRANT USAGE ON SCHEMA public TO audittrail_app;
-- after the first run (once Flyway has created the table):
GRANT SELECT, INSERT ON audit_events TO audittrail_app;      -- deliberately no UPDATE/DELETE
GRANT USAGE, SELECT ON SEQUENCE audit_events_id_seq TO audittrail_app;
```

### 2. WSO2 Identity Server
- Download WSO2 Identity Server 7.3 from the
  [wso2/product-is GitHub releases](https://github.com/wso2/product-is/releases) page
  (the OSS distribution zip) and unzip it anywhere.
- Start with Java 21: `$env:JAVA_HOME="<jdk21>"; .\bin\wso2server.bat`; console at
  `https://localhost:9443/console` (admin/admin).
- Register a **Single-Page Application** template app (public client, PKCE) for the React
  frontend: redirect URI `http://localhost:5173/`, grant **Code**; **Access Token → Token Type:
  JWT** (WSO2 then issues RFC 9068 `at+jwt` access tokens carrying the same claims as the id_token).
- **Roles:** create `FRAUD_ANALYST` / `COMPLIANCE_OFFICER` under **User Management → Roles**
  with **Audience: Organization** (not Application) so the SPA app can see them; assign
  `sara`→`FRAUD_ANALYST`, `joe`→`COMPLIANCE_OFFICER`. On the app itself, set **Roles tab →
  Role Audience: Organization** to match.
- **User Attributes:** enable the **Roles** attribute for both the **ID Token** and the
  **Access Token** columns (otherwise `roles` is only present on the id_token, not the
  access_token the backend actually validates); set **Subject → alternate subject
  identifier → Username** (otherwise `sub` is a UUID).
- **Trust the cert:** import WSO2's TLS cert into the JDK truststore so the backend can fetch JWKS:
  ```
  keytool -importcert -alias wso2carbon -file wso2carbon.pem -cacerts -storepass changeit
  ```

### 3. Backend - environment variables & run
```powershell
$env:DB_PASSWORD = '<app-password>'          # audittrail_app
$env:FLYWAY_PASSWORD = '<postgres-password>' # Flyway runs migrations as owner
cd backend
.\mvnw.cmd spring-boot:run                    # starts on :8080, Flyway builds the schema
```

### 4. Frontend
```powershell
cd frontend
cp .env.example .env    # set VITE_OIDC_CLIENT_ID to the SPA app's client ID
npm install
npm run dev              # starts on :5173
```

---

## Testing

```powershell
cd backend
.\mvnw.cmd test      # unit + security-slice tests (no Docker)
.\mvnw.cmd verify    # + Testcontainers immutability test (Docker must be running)
```

- `AuditServiceTest` - identity provenance, duplicate-flag rule, dashboard aggregation
- `AuditControllerSecurityTest` - the RBAC matrix (401 / 403 / 2xx)
- `AuditImmutabilityIT` - real PostgreSQL; proves `UPDATE`/`DELETE` are rejected by the trigger

---

## API

Base path `/api/audit`. All endpoints require a valid Bearer token.

| Method | Path | Role |
|---|---|---|
| POST | `/refund` | FRAUD_ANALYST or COMPLIANCE_OFFICER |
| POST | `/fraud-flag` | FRAUD_ANALYST |
| GET  | `/events` | either |
| GET  | `/events/by-type?type=` | COMPLIANCE_OFFICER |
| POST | `/search` | either |
| GET  | `/dashboard` | COMPLIANCE_OFFICER |
| GET  | `/high-risk` | either |

Errors return a consistent JSON shape: `{ "error", "status", "timestamp" }` (401 no/invalid token,
403 wrong role, 400 validation, 409 duplicate flag).

---

## Notable design decisions

- **`BigDecimal` + `DECIMAL(19,2)`** for money - exact, no floating-point drift.
- **`TIMESTAMPTZ` + `Instant`** for audit times - unambiguous across time zones.
- **JPA Specification** for dynamic search - avoids the `:param IS NULL OR …` pattern that Postgres
  can't type-infer.
- **Immutability in the database**, not app code - app-level rules can be bypassed; the DB engine
  cannot.

## Known trade-offs (dev scope)

- WSO2's self-signed cert is trusted via the local JDK truststore (dev only).
- Future work: hash-chaining rows (`prev_hash`/`row_hash`) for tamper *detection* on top of the
  existing tamper *prevention*.
