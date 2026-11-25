# Backend Agent Guide

Backend-specific context. Project-wide context (tech stack, roles, default users, env vars, deployment, key files) is in the root `AGENTS.md`; domain terms are in `GLOSSARY.md`.

## Package layout

```
backend/src/main/java/com/apartmentmanager/
├── controller/   # REST controllers + GlobalExceptionHandler, ApartmentSummaryDto, PresentationDto (records)
├── model/        # JPA entities, Lombok @Getter/@Setter/@NoArgsConstructor
├── repository/   # JpaRepository interfaces + custom @Query (LEFT JOIN FETCH against N+1)
├── service/      # @Service classes
├── audit/        # @Audited annotation + AuditAspect
├── security/     # JwtAuthFilter, JwtUtil, SecurityConfig, CustomUserDetailsService
└── util/         # DefaultPassword (see root AGENTS.md, "Default Users")
```

## Auth flow

1. `POST /api/auth/login` with `{ username, password }` returns `{ token, id, username, role, apartmentId? }`. A failed login returns 401 `{ error: "Invalid credentials" }`.
2. Every later request sends `Authorization: Bearer <token>`.
3. `JwtAuthFilter` lets `OPTIONS` through, validates the token with `JwtUtil` and sets authority `ROLE_<role>`. A missing or invalid token on a protected route gets 403 from Spring.
4. `JwtUtil`: HMAC key from `app.jwt.secret` (Base64), claims `sub` = username and `role`, expiry `app.jwt.expiration-ms` (24h).

## Authorization: two patterns

**1. `@PreAuthorize`** for role-only rules, e.g. `hasRole('OWNER')` (deletes, user management, audit, contact create/update/delete, OCR keyword edits) and `hasAnyRole('OWNER','ADMIN')` (apartment create, notes, inspections, ticket status/unread, applications list).

**2. Inline tenant check** for anything scoped to a tenant's **assigned apartment**. OWNER and ADMIN pass; a TENANT passes only on their assigned apartment:

```java
User user = userRepository.findByUsername(auth.getName()).orElseThrow();
if (user.getRole() == Role.TENANT) {
    if (user.getApartment() == null || !user.getApartment().getId().equals(apartmentId)) {
        return ResponseEntity.status(403).build();
    }
}
```

When the path has a child id (e.g. a billing document id) rather than the apartment id, compare against the child's apartment (see `BillPaymentController.updateBillAmount`). Tickets use a different rule: a tenant sees only tickets they created.

## API surface

Paths are under `/api`. "Tenant check" = pattern 2 above.

| Area | Endpoint | Access |
|---|---|---|
| Auth | `POST /auth/login` | Public |
| | `GET /auth/me`, `POST /auth/verify-password` | Logged in |
| Apartments | `GET /apartments`, `GET /apartments/summary` | Logged in (tenant sees only their assigned apartment) |
| | `GET /apartments/{id}` | Logged in, tenant check (a denied tenant gets 500, not 403) |
| | `POST /apartments` | OWNER/ADMIN |
| | `DELETE /apartments/{id}`, `PUT /{id}/metadata`, photo delete / set main | OWNER |
| | `PUT /{id}/details`, `PUT /{id}/presentation` | OWNER/ADMIN |
| | `GET /{id}/presentation`, `GET /apartments/presentation-list` | **Public** |
| Apartment photos | `POST /apartments/{id}/photos` | Tenant check |
| | `GET /apartments/photos/{fileName}` | **Public**, apartment photos only |
| Apartment documents | `GET /apartments/{id}/protocols` | Tenant check (a denied tenant gets an empty list, not 403) |
| | `POST /apartments/{id}/protocols` | Tenant check |
| | `GET /apartments/protocols/{fileName}` | Tenant check (on the apartment document's apartment) |
| | `DELETE /apartments/protocols/{id}` | OWNER |
| Billing documents | `GET /apartments/{id}/bills` | Tenant check |
| | `POST /apartments/{id}/bills` (params `file`, `billType`, `documentType`) | Tenant check |
| | `GET /bills/{fileName}` | Tenant check (on the billing document's apartment) |
| | `PUT /bills/{id}/amount` (body `{ amount, currency }`) | Tenant check (on the billing document's apartment) |
| | `POST /bills/{id}/analyze` (re-runs OCR) | OWNER/ADMIN |
| | `DELETE /bills/{id}` | OWNER |
| Tickets | `GET`/`POST /apartments/{id}/tickets` | Tenant check |
| | `GET /tickets/{id}` | Tenant sees only tickets they created |
| | `POST /tickets/{id}/photos` | Tenant only on tickets they created, max 5 photos |
| | `GET /tickets/photos/{fileName}` | Tenant only on tickets they created |
| | `GET /tickets` (also marks every ticket read for the caller), `/tickets/unread`, `/tickets/unread/count`, `POST /tickets/{id}/read`, `PATCH /tickets/{id}` (body `{ status }`) | OWNER/ADMIN |
| Contacts | `GET /apartments/{id}/contacts` | Tenant check |
| | `POST /apartments/{id}/contacts`, `PUT`/`DELETE /contacts/{id}` | OWNER |
| Notes | all `/apartments/{id}/notes`, `/notes/{id}` | OWNER/ADMIN |
| Inspections | all `/inspections/**` and `/apartments/{id}/inspections` | OWNER/ADMIN (`DELETE /inspections/{id}` OWNER) |
| | `GET /inspections/{id}/photos` (all photos as a zip) | OWNER/ADMIN. There is no single inspection photo download route |
| Applications | `POST /applications` | **Public** |
| | `GET /applications`, `/applications/counts` | OWNER/ADMIN |
| | `GET /applications/files/{fileName}` | OWNER/ADMIN |
| Users | `GET`/`POST /users`, `DELETE /users/{id}`, `PUT /users/{id}/apartment` (body `{ apartmentId }`), `PUT /users/{id}/email` (body `{ email }`) | OWNER |
| Audit | `GET /audit?username=` | OWNER |
| OCR keywords | `GET /ocr-keywords` | Logged in |
| | `PUT /ocr-keywords/{id}`, `POST /ocr-keywords` | OWNER |

**Access rules** (from `.scratch/security-fixes/spec.md`):

- OWNER and ADMIN reach everything, on every apartment.
- A TENANT reaches only what belongs to their **assigned apartment**, based on the current assignment only. A previous apartment gives no access.
- Ticket photos follow the ticket rule: a tenant sees them only on tickets they created.
- Applicant documents: OWNER and ADMIN only.
- Only apartment photos are public, because the public presentation pages need them. Every other file route needs a login.
- A denied tenant gets **403**. The exceptions are listed in the table above and under Known quirks.

## Request bodies and validation

- Exceptions to the `Map` request-body pattern (root `AGENTS.md`, Key Patterns): `ApartmentController.create` (`@Valid @RequestBody Apartment`), `updateMetadata` (`List<String>`), presentation update (raw `String`), file uploads (`@RequestParam("file") MultipartFile`).
- Validation is manual and inline, e.g. `return ResponseEntity.badRequest().body(Map.of("error", "Title is required"));`.

## Audit logging

Mutating endpoints are annotated with `@Audited(action = "...", message = "...")`. `AuditAspect` writes the entry; the message can reference path variables and body fields (e.g. `{body['name']}`, `#{id}`). Only `AuthController` (login, failed login) calls `AuditService.log` directly and records the IP address; aspect entries have no IP.

## Database

- Dev/test: H2 in-memory (`jdbc:h2:mem:apartments`), `ddl-auto=update`.
- Production: PostgreSQL via profile `postgres` (`application-postgres.properties`, `DB_USERNAME`/`DB_PASSWORD`).
- Schema is generated by Hibernate; there are no migrations.
- `UserService.init()` creates owner/admin/tenant when the users table is empty. `SeedDataService` (only when `app.seed.enabled=true` and there are no apartments) seeds two apartments, `tenant2`, tickets, contacts, notes, placeholder PNGs (raw bytes compressed with `java.util.zip.Deflater`, no image library) and the OCR keywords.

## File storage

- Every stored file (apartment photos, apartment documents, ticket and inspection photos, applicant documents, billing documents) goes into one shared `uploads/` folder, named `UUID + original extension`. Max upload 10MB. Docker mounts it as a named volume.
- `PhotoStorageService` reads the Spring property `app.upload.dir`. `BillPaymentService` and `SeedDataService` read the JVM system property `app.upload.dir` instead, so in tests billing documents land in `./uploads`, not the test temp folder.
- Every file route serves only its own kind of file. It first looks the name up in that kind's records (e.g. `ApartmentService.isApartmentPhoto`, `BillPaymentService.findByStoredFileName`, `TicketService.findByPhotoPath`) and returns 404 for a name of another kind, before running any access check.
- Names are resolved with `PhotoStorageService.resolveInside`, which refuses any name that escapes the uploads folder. A new file route must do both: check the kind, then call `resolveInside`.

## Error handling

`GlobalExceptionHandler`: `AccessDeniedException` gives 403 `{ error: "Access denied" }`, and any `RuntimeException` gives 500 `{ error: <message> }`. There are no custom exceptions, and services throw `RuntimeException` with a message. An invalid enum value in a body (e.g. ticket status) ends up as 500.

## Running locally

From the repo root: `./gradlew :backend:bootRun` (H2), or with `--args='--spring.profiles.active=postgres'` after `docker compose up db`. `docker compose up --build` runs the full stack behind nginx (:80/:443).

## Testing

- `./gradlew :backend:test` (password handling: root `AGENTS.md`, Testing).
- Extend `AbstractIntegrationTest`: `@SpringBootTest(MOCK)` + `@Transactional` (rolls back per test), MockMvc with Spring Security, a `@TempDir` set as `app.upload.dir`.
- Helpers: `bearer(username)` (real JWT), `createApartment(title)`. Users owner/admin/tenant exist. Assign a tenant with `tenant.setApartment(apt)` + save, then `entityManager.flush()` and `clear()` before HTTP calls.
- Tests are mostly flat classes. Use `@Nested` groups (as in `TicketTest`, `ApartmentTest`) when one class covers several endpoints.
- JSON in tests is Jackson 3 (`tools.jackson.databind.ObjectMapper`), not `com.fasterxml`.
- Every access check gets at least one test where a tenant is denied (a different assigned apartment) and one where they're allowed.

## Code conventions

- Controllers and services use `@RequiredArgsConstructor` with `private final` fields (constructor injection). Field `@Autowired` is fine in tests only.
- `@JsonIgnore` on passwords and back-references. `Apartment.getTenant()` is exposed via `@JsonProperty("tenant")`.
- `@ElementCollection` for `photoPaths` and `metadata`.
- Derived `deleteByApartment(Apartment)` on repositories. Apartment delete cascades manually in `ApartmentService.delete()` (tickets, billing documents, apartment documents, tenants unassigned, contacts and photos cleared), not via JPA cascade.

## Known quirks

1. `TicketStatus` is `NEW`, `IN_PROGRESS`, `DONE`, `REJECTED` (no `RESOLVED`).
2. Apartment document kinds (`DocumentType`): `HANDOVER_PROTOCOL`, `BILLS`, `PHOTOS`, `OTHER`. A `BILLS` apartment document is not a billing document.
3. A billing document's bill type is a free-form string (e.g. "Monthly Maintenance Fee"), and its document type is the string `"bill"` or `"proof"`.
4. Tenant 403 bodies are inconsistent: empty in most controllers, `{ error: "Access denied" }` in tickets.
5. `findByUsername(...).orElseThrow()` gives a 500 if a still-valid token's user was deleted.
