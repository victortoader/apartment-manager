# AGENTS.md - Apartment Manager Project Context

## Overview

Full-stack apartment management portal with JWT-based role-based access control.

## Tech Stack

- **Backend:** Java 21, Spring Boot 4.1.0, Gradle, Spring Security 7.1
- **Frontend:** React 18, React Router, 5 languages (EN/DE/FR/IT/RO)
- **Database:** H2 (local dev) / PostgreSQL 16 (Docker/production)
- **Auth:** JWT (jjwt 0.12.6), BCrypt, stateless sessions
- **OCR:** Tess4J 5.11.0 + PDFBox 3.0.3 (bill text extraction, async)
- **Email:** Jakarta Mail IMAP (Gmail bill fetching, polling every 5 min)
- **Deployment:** Docker Compose, nginx reverse proxy, GitHub Actions CI/CD to EC2

## Roles

| Role | Permissions |
|------|-------------|
| OWNER | Full access: CRUD apartments, manage users, view audit logs, delete anything |
| ADMIN | Read/create apartments, manage tickets, upload bills, no delete |
| TENANT | View own apartment, create/upload tickets, upload bill proofs |

## Default Users

| Username | Password | Role |
|----------|----------|------|
| owner | from `DEFAULT_PASSWORD` env var | OWNER |
| admin | from `DEFAULT_PASSWORD` env var | ADMIN |
| tenant | from `DEFAULT_PASSWORD` env var | TENANT |
| tenant2 | from `DEFAULT_PASSWORD` env var | TENANT (created by the demo seed data, only when `app.seed.enabled=true`) |

**IMPORTANT:** Passwords are NOT hardcoded in source. They come from the `DEFAULT_PASSWORD` environment variable (with a local-dev fallback to the root `.env` file). The application fails fast at startup if neither is set. Never commit hardcoded passwords.

Backend internals (auth flow, authorization patterns, full API surface, file storage, test setup, conventions, quirks) are in `backend/AGENTS.md`.

## Key Backend Files

### Models
- `backend/src/main/java/com/apartmentmanager/model/Apartment.java` - Apartment entity with title, description, location, price, rooms, area, photoPaths, metadata, presentation
- `backend/src/main/java/com/apartmentmanager/model/BillPayment.java` - Bill entity with originalFileName, storedFileName, contentType, billType, documentType, extractedAmount, extractedCurrency, ocrConfidence, ocrFailed
- `backend/src/main/java/com/apartmentmanager/model/OcrKeywords.java` - OCR keywords entity with language, amountKeywords, languageKeywords, paymentKeywords, defaultCurrency
- `backend/src/main/java/com/apartmentmanager/model/User.java` - User entity with username, password, role, email, apartment reference

### Services
- `backend/src/main/java/com/apartmentmanager/service/BillPaymentService.java` - Bill upload, OCR analysis, amount extraction. All uploads go through `uploadFromBytes()` (byte[]); the controller passes `file.getBytes()`, and `EmailFetchService` passes attachment bytes
- `backend/src/main/java/com/apartmentmanager/service/OcrService.java` - Dual-path OCR: PDF text extraction via PDFBox → regex, fallback to Tesseract. Extracts amount and currency (currency from symbols/language keywords). The uploader chooses the document type (see Key Patterns); `paymentKeywords` is seeded and editable but unused
- `backend/src/main/java/com/apartmentmanager/service/EmailFetchService.java` - IMAP polling for Gmail, downloads attachments, saves as bills to first apartment
- `backend/src/main/java/com/apartmentmanager/service/UserService.java` - User CRUD, `@PostConstruct` creates default users only when DB is empty
- `backend/src/main/java/com/apartmentmanager/service/SeedDataService.java` - Seeds OCR keywords (amount, language, payment) for RO/DE/EN
- `backend/src/main/java/com/apartmentmanager/service/AuditService.java` - Audit logging

### Controllers
- `backend/src/main/java/com/apartmentmanager/controller/BillPaymentController.java` - Bill CRUD, upload with documentType param, OCR analyze, update amount
- `backend/src/main/java/com/apartmentmanager/controller/OcrKeywordsController.java` - GET/PUT/POST OCR keywords
- `backend/src/main/java/com/apartmentmanager/controller/AuthController.java` - Login, /me, verify-password

### Security
- `backend/src/main/java/com/apartmentmanager/security/JwtAuthFilter.java` - JWT token validation
- `backend/src/main/java/com/apartmentmanager/security/SecurityConfig.java` - Spring Security config

## Key Frontend Files

- `frontend/src/PaidBills.js` - Bill upload/view with document type selector, bill/proof labels (red/blue), per-month balance, summary section
- `frontend/src/ApartmentDetail.js` - Single apartment view, PaidBills in left column, Contacts in right column
- `frontend/src/ApartmentList.js` - Apartment dashboard with summary cards
- `frontend/src/locales/en.json` (and de/it/fr/ro) - All i18n keys

## Key Patterns

- Backend uses `Map<String, String>` request bodies for most endpoints, not typed DTOs
- OCR runs async on upload via `CompletableFuture.orTimeout(3, MINUTES)`; on failure → `ocrFailed=true`
- OWNER and ADMIN can see and edit OCR-extracted amounts on any billing document; a TENANT only on billing documents of their assigned apartment
- Document types: "bill" (red label) and "proof" (blue label). The uploader picks it in the `PaidBills.js` selector and it's sent as the `documentType` upload param (default "bill"); documents fetched by email are always "bill"
- Bill list shows per-month balance (green=overpaid, red=unpaid)
- Summary section below bill list: Total Bills, Total Paid, Balance
- Photos are downscaled client-side (max 1920px longest side, JPEG q0.85) before upload via `frontend/src/imageUtils.js` (`prepareImage`); HEIC/HEIF from iPhones are converted in-browser via `heic2any`. Applied to apartment, inspection-row, and ticket photo uploads. PDF/protocol/bill document uploads are NOT resized.

## Environment Variables

### Required for Production (EC2)

| SSM Parameter | Description |
|---------------|-------------|
| `/apartment-manager/DB_USERNAME` | PostgreSQL username |
| `/apartment-manager/DB_PASSWORD` | PostgreSQL password |
| `/apartment-manager/JWT_SECRET` | JWT signing secret |
| `/apartment-manager/DEFAULT_PASSWORD` | Password for all default users |

### Optional (Email Fetch)

| SSM Parameter | Description |
|---------------|-------------|
| `/apartment-manager/EMAIL_FETCH_ENABLED` | Set to `true` to enable |
| `/apartment-manager/EMAIL_FETCH_ADDRESS` | Gmail address |
| `/apartment-manager/EMAIL_FETCH_PASSWORD` | Gmail App Password |

## Deployment

- **CI/CD:** GitHub Actions on push to `main`
- **deploy.sh:** Fetches secrets from SSM, writes `.env`, runs `docker compose up -d --build`, truncates `users` table, restarts backend
- **User reset:** Every deployment truncates the users table and re-creates default users with current `DEFAULT_PASSWORD`
- **Docker:** `backend/Dockerfile` installs tesseract-ocr + lang packs (deu, eng)

## API Endpoints

The full endpoint list with who can call each one is in `backend/AGENTS.md` ("API surface").

## Planned Features

- **AI bill matching:** Use Amazon Bedrock (Nova Micro) to match bills to apartments based on bill content (~$0.00002/bill)
- Bill-to-apartment matching based on address/tenant name extraction from OCR text

## Testing

- Tests use H2 in-memory database (no external dependencies)
- Test password constant: `TEST_PASSWORD` comes from the `DEFAULT_PASSWORD` env var; the Gradle `test` task sets it to a random value
- Run: `./gradlew test` (from `backend/`)
- E2E tests (`frontend/`) never hardcode the password; they require `DEFAULT_PASSWORD` env var and fail fast if unset. Run: `DEFAULT_PASSWORD=... npm run test:e2e` (from `frontend/`)
- `./gradlew build` / `./gradlew check` also run the e2e suite (root `check` depends on `e2e`); `./gradlew e2e` runs it alone
- The Gradle `e2e` task boots the packaged backend from `backend/build/libs/*.jar` (via `frontend/scripts/start-backend.cjs`, using the Java 21 toolchain) so the suite never spawns a nested Gradle build
- Skip e2e with `./gradlew build -PskipE2e` (or `-x e2e`); `-PskipE2e=false` forces it on. Put `skipE2e=true` in `~/.gradle/gradle.properties` to keep IDE builds fast
- E2E tests run on CI via `.github/workflows/deploy.yaml` (needs the `DEFAULT_PASSWORD` repository secret). CI runs `npm run test:e2e` directly, not `./gradlew e2e`, because the runner needs `playwright install --with-deps`

## Important Notes

- Never commit hardcoded passwords in source code
- Never commit/push without explicit user approval
- OCR keywords are stored in `ocr_keywords` table, editable by OWNER via Manage section
- `OcrKeywords` entity has 5 fields: language, amountKeywords, languageKeywords, paymentKeywords, defaultCurrency

## Agent skills

### Issue tracker

Issues live as local markdown files under `.scratch/<feature>/`. See `docs/agents/issue-tracker.md`.

### Triage labels

Default five-role vocabulary (needs-triage, needs-info, ready-for-agent, ready-for-human, wontfix), recorded as a `Status:` line in each issue file. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: `GLOSSARY.md` + `docs/adr/` at the repo root (created lazily). See `docs/agents/domain.md`.

Use the `GLOSSARY.md` terms (billing document, proof of payment, assigned apartment, apartment document, etc.) in code, docs and tickets, in place of the terms it lists under _Avoid_.
