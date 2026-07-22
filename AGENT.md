# Backend Agent Guide

## Tech Stack
- Java 21, Spring Boot 4.1.0, Gradle
- Spring Data JPA + Hibernate, Spring Security
- H2 (dev/test), PostgreSQL 16 (production)
- jjwt 0.12.6, BCrypt passwords
- Jackson 3.x (`tools.jackson.databind`) in tests

## Project Structure
```
src/main/java/com/apartmentmanager/
├── DemoApplication.java
├── controller/
│   ├── AuthController.java              # POST /api/auth/login, GET /api/auth/me, POST /api/auth/verify-password
│   ├── ApartmentController.java         # CRUD, photos, protocols, presentations, metadata, details
│   ├── BillPaymentController.java       # Bill upload/download/delete
│   ├── ContactController.java           # Contact CRUD per apartment
│   ├── NoteController.java              # Note CRUD per apartment
│   ├── TicketController.java            # Ticket CRUD, photos, status, unread
│   ├── UserManagementController.java    # User CRUD, apartment assignment
│   ├── AuditController.java             # GET /api/audit with optional ?username= filter
│   ├── HelloController.java             # GET /hello → "Hello, World!"
│   ├── GlobalExceptionHandler.java      # @ControllerAdvice
│   ├── ApartmentSummaryDto.java         # Java record for /summary endpoint
│   └── PresentationDto.java             # Java record for public presentation
├── model/                               # JPA entities, no Lombok, manual getters/setters
├── repository/                          # JpaRepository interfaces + custom @Query for N+1 fixes
├── service/                             # @Service classes, constructor injection
└── security/                            # JWT filter chain, BCrypt, CORS
```

## Auth Flow (Exact)

1. `POST /api/auth/login` with `{ username, password }` → returns `{ token, id, username, role, apartmentId? }`
2. Every subsequent request sends `Authorization: Bearer <token>` header
3. `JwtAuthFilter` (extends `OncePerRequestFilter`):
   - `OPTIONS` requests bypass the filter entirely
   - Extracts token from header, validates via `JwtUtil`
   - Sets `SecurityContextHolder` with authority `ROLE_<role>` (e.g., `ROLE_OWNER`)
   - Invalid/missing token → no auth set → Spring returns 403
4. `JwtUtil`: HMAC key from `app.jwt.secret` (Base64-decoded), claims: `sub`=username, custom `role`=role name, 24h expiration
5. Failed login → 401 `{ error: "Invalid credentials" }` + audit log with IP address

## Two Authorization Patterns

**Pattern 1 — `@PreAuthorize`** (for OWNER and OWNER|ADMIN endpoints):
```java
@PreAuthorize("hasRole('OWNER')")              // ApartmentController.delete, all notes/contacts/user mgmt/audit
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")  // ApartmentController.create, ticket status/unread
```

**Pattern 2 — Manual tenant check** (for TENANT-scoped access):
```java
User user = userRepository.findByUsername(auth.getName()).orElseThrow();
if (user.getRole() == Role.TENANT) {
    if (user.getApartment() == null || !user.getApartment().getId().equals(id)) {
        return ResponseEntity.status(403).build();  // or Map.of("error", "Access denied")
    }
}
```
⚠️ **Inconsistency**: Tenant 403 responses are sometimes empty `ResponseEntity.status(403).build()` (bills, photos, protocols) and sometimes `Map.of("error", "Access denied")` (tickets).

## Every Controller Does This
```java
User user = userRepository.findByUsername(auth.getName()).orElseThrow();
```
This throws `NoSuchElementException` if the JWT-valid user no longer exists in DB → becomes 500 via `GlobalExceptionHandler`. No custom exception handling for this case.

## Complete API Surface

| Method | Path | Auth | Notes |
|--------|------|------|-------|
| POST | `/api/auth/login` | Public | Returns JWT + user info |
| GET | `/api/auth/me` | Auth | Current user info |
| POST | `/api/auth/verify-password` | Auth | Password check for manage mode |
| GET | `/api/apartments` | Auth | Tenant sees only their apt |
| GET | `/api/apartments/summary` | Auth | Summary DTO (recentBills, openTickets) |
| GET | `/api/apartments/{id}` | Auth | Tenant can only see own |
| POST | `/api/apartments` | OWNER/ADMIN | Create apartment |
| DELETE | `/api/apartments/{id}` | OWNER | Manual cascade in service |
| POST | `/{id}/photos` | Auth | Tenant check |
| GET | `/photos/{fileName}` | **Public** | Serve photo |
| GET | `/{id}/protocols` | Auth | Tenant check |
| POST | `/{id}/protocols` | Auth | Tenant check |
| GET | `/protocols/{fileName}` | **Public** | Serve protocol |
| DELETE | `/protocols/{id}` | OWNER | |
| GET | `/{id}/presentation` | **Public** | |
| PUT | `/{id}/presentation` | OWNER | Body: raw `text/plain` |
| PUT | `/{id}/details` | OWNER | Body: `Map<String, Object>` with price/description |
| PUT | `/{id}/metadata` | OWNER | Body: `List<String>` |
| POST | `/api/apartments/{id}/bills` | Auth | Tenant check |
| GET | `/api/apartments/{id}/bills` | Auth | Tenant check |
| GET | `/api/bills/{fileName}` | **Public** | |
| DELETE | `/api/bills/{id}` | OWNER | |
| POST | `/api/apartments/{id}/tickets` | Auth | Tenant check |
| GET | `/api/apartments/{id}/tickets` | Auth | Tenant check |
| GET | `/api/tickets` | OWNER/ADMIN | Marks all as read |
| GET | `/api/tickets/unread/count` | OWNER/ADMIN | Returns `{ count }` |
| GET | `/api/tickets/unread` | OWNER/ADMIN | Returns list of ticket objects |
| POST | `/api/tickets/{id}/read` | OWNER/ADMIN | |
| GET | `/api/tickets/{id}` | Auth | Tenant sees only own-created |
| PATCH | `/api/tickets/{id}` | OWNER/ADMIN | Body: `{ status }` — string value |
| POST | `/api/tickets/{id}/photos` | Auth | Tenant limited to 5 photos |
| GET | `/api/tickets/photos/{fileName}` | **Public** | |
| GET/POST/PUT/DELETE | `/api/apartments/{id}/notes` | OWNER/ADMIN | Full CRUD |
| GET/POST/PUT/DELETE | `/api/apartments/{id}/contacts` | OWNER/ADMIN for CUD, Auth for GET | |
| GET/POST/DELETE | `/api/users` | OWNER | |
| PUT | `/api/users/{id}/apartment` | OWNER | Body: `{ apartmentId }` |
| GET | `/api/audit` | OWNER | Optional `?username=` filter |

## Request Body Conventions

Most endpoints use `@RequestBody Map<String, String>` instead of typed DTOs:
```java
public ResponseEntity<?> createNote(@PathVariable Long id, @RequestBody Map<String, String> body, Authentication auth) {
    String content = body.get("content");
```

Exceptions:
- `ApartmentController.create()` → `@Valid @RequestBody Apartment`
- `ApartmentController.updateDetails()` → `Map<String, Object>`
- `ApartmentController.updateMetadata()` → `List<String>`
- Presentation update → raw `@RequestBody String content`
- File uploads → `@RequestParam("file") MultipartFile`

Validation is manual inline (not Spring `@Valid`):
```java
if (title == null || title.isBlank()) {
    return ResponseEntity.badRequest().body(Map.of("error", "Title is required"));
}
```

## Audit Logging

Every mutating endpoint calls:
```java
auditService.log(user.getUsername(), user.getRole().name(), "ACTION_NAME", "Human detail", null);
```
IP address is always `null` except in `AuthController` (login/logout). Action names: `LOGIN`, `LOGIN_FAILED`, `APARTMENT_CREATED`, `APARTMENT_DELETED`, `APARTMENT_PHOTO_UPLOADED`, `APARTMENT_DETAILS_UPDATED`, `APARTMENT_METADATA_UPDATED`, `PRESENTATION_UPDATED`, `TICKET_CREATED`, `TICKET_STATUS_UPDATED`, `TICKET_PHOTO_UPLOADED`, `BILL_UPLOADED`, `BILL_DELETED`, `NOTE_CREATED`, `NOTE_UPDATED`, `NOTE_DELETED`, `CONTACT_CREATED`, `CONTACT_UPDATED`, `CONTACT_DELETED`, `USER_CREATED`, `USER_DELETED`, `APARTMENT_ASSIGNED`, `PROTOCOL_UPLOADED`.

## Database

- Dev: H2 in-memory `jdbc:h2:mem:apartments`, `ddl-auto=update`
- Prod: PostgreSQL `apartment-management-db`, profile `postgres`
- Schema auto-generated by Hibernate
- Default users created by `UserService.init()` (`@PostConstruct`): owner/owner, admin/admin, tenant/tenant
- Seed data (`SeedDataService`): 2 apartments ("Sunny Studio" 850EUR, "Garden Flat" 1200EUR), 2 tenants, 5 tickets, 3 contacts/apt, 4 notes, 10 placeholder PNGs/apt (procedurally generated), long HTML presentations

## File Storage

- All files stored in `uploads/` directory (configurable via `app.upload.dir`)
- Naming: `UUID + originalExtension`
- Photos: `PhotoStorageService` (used by `HandoverProtocolService` too)
- Bills: `BillPaymentService` (manages its own path via `System.getProperty`)
- Max upload: 10MB
- Docker maps `uploads/` as named volume

## Error Handling

`GlobalExceptionHandler`:
- `AccessDeniedException` → 403 `{ error: "Access denied" }`
- `RuntimeException` → 500 `{ error: <message> }`

No custom exceptions defined. Everything uses `RuntimeException`. No handler for `MethodArgumentNotValidException` or `HttpMessageNotReadableException` (e.g., invalid `TicketStatus` in PATCH → uncaught `IllegalArgumentException` → 500).

## Running Locally

```bash
./gradlew bootRun                                    # H2 in-memory
./gradlew bootRun --args='--spring.profiles.active=postgres'  # PostgreSQL
docker compose up db                                 # Start just the database
docker compose up --build                            # Full stack (nginx :80/:443)
```

## Testing

```bash
./gradlew test
```

### Base Class: `AbstractIntegrationTest`
- `@SpringBootTest(MOCK)` + `@Transactional` (rolls back after each test)
- `@TempDir static Path tempDir` → injected via `@DynamicPropertySource` as `app.upload.dir`
- `MockMvc` with `springSecurity()` filter applied
- Helper: `bearer(username)` generates real JWT
- Helper: `createApartment(title)` saves and returns an apartment
- Users "owner", "admin", "tenant" seeded by `UserService.init()`
- Pattern: `entityManager.flush()` + `entityManager.clear()` before HTTP requests

### Test Structure
Tests use `@Nested` classes:
```java
class TicketTest extends AbstractIntegrationTest {
    @Nested class CreateTicket { ... }
    @Nested class GetTickets { ... }
    @Nested class UpdateTicketStatus { ... }
}
```

### JSON Library in Tests
Uses `tools.jackson.databind.ObjectMapper` (Jackson 3.x, the new package), not `com.fasterxml.jackson.databind`.

## Code Conventions

- **No Lombok** — all getters/setters written manually
- Constructor injection everywhere (no `@Autowired` on fields)
- `@JsonIgnore` on passwords and back-references
- `@JsonProperty("tenant")` on computed `Apartment.getTenant()` method
- `@ElementCollection` for `photoPaths` and `metadata` (separate tables)
- Repositories with custom `@Query` for `LEFT JOIN FETCH` to solve N+1 (ApartmentRepository, TicketRepository, BillPaymentRepository)
- `deleteByApartment(Apartment)` derived delete methods on repos
- Services throw `RuntimeException` with descriptive messages

## Known Quirks / Things to Watch

1. **TicketStatus values**: `NEW`, `IN_PROGRESS`, `DONE`, `REJECTED` — NOT `RESOLVED` (the enum name is `TicketStatus` but values differ from what you might expect)
2. **DocumentType**: `HANDOVER_PROTOCOL`, `BILLS`, `PHOTOS`, `OTHER`
3. **No typed input DTOs** — almost everything is `Map<String, String>`, which means no automatic validation and manual key extraction
4. **Inconsistent 403 responses** — empty body vs `{ error: "Access denied" }` across controllers
5. **`orElseThrow()` without custom exception** — if a JWT-valid user is deleted from DB mid-session, it's a 500
6. **Apartment delete cascade** is manual in `ApartmentService.delete()` — not JPA cascade (it deletes tickets, bills, protocols, nulls tenants, clears contacts/photos)
7. **Bill type** is a free-form string (e.g., "Monthly Maintenance Fee"), not an enum — matches what the frontend sends
8. **Seed data generates raw PNG bytes** using `Deflater` compression without any image library
9. **application-postgres.properties** requires env vars `DB_USERNAME` and `DB_PASSWORD`
