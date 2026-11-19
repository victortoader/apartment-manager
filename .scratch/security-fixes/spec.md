# Security fixes: file routes and tenant access checks

Status: ready-for-agent

## Problem Statement

Several endpoints let people reach data they shouldn't:

- Four file routes are public (`SecurityConfig.java:40-43`) and all read from one shared uploads folder by file name, with no check on which kind of file was asked for. Anyone holding a stored file name can download a billing document, an applicant's document or a handover protocol without logging in, for example via `/api/tickets/photos/<name>`. File names are random UUIDs, but they leak (e.g. `presentation-list` publicly returns photo names).
- Any tenant can read any apartment's contacts, correct the amount on any apartment's billing documents, and download any apartment's billing documents.
- Any logged-in user, tenants included, can download applicant documents.

## Solution

Close each hole with a targeted access check. The stored-file module refactor stays a separate, later effort.

Access rules (see `GLOSSARY.md` for terms):

- **OWNER and ADMIN** can reach everything, on every apartment.
- **TENANT** can reach only things belonging to their **assigned apartment**. Access follows the current assignment only: a tenant keeps no access to a previous apartment.
- Ticket photos follow the existing ticket rule: a tenant sees them only on tickets they created.
- Applicant documents: OWNER and ADMIN only.
- Only apartment photos stay public, because the public presentation pages need them.
- Every route serves only files of its own kind. A file name of another kind gets a 404.
- A denied tenant gets **403**.

## Tickets

- `issues/02-contacts-tenant-check.md`
- `issues/03-amount-correction-tenant-check.md`
- `issues/04-billing-document-download-check.md`
- `issues/05-applicant-document-download-check.md`
- `issues/07-refuse-paths-outside-uploads.md`
- `issues/08-apartment-photo-route-serves-only-apartment-photos.md`
- `issues/09-remove-public-inspection-photo-download.md`
- `issues/10-apartment-documents-require-login-and-tenant-check.md`
- `issues/11-ticket-photos-require-login-and-creator-check.md`

Tickets 02–05 and 07–10 are independent and can be done in any order. Ticket 11 is blocked by 10 (shared frontend download helper).

Replaced (wontfix):

- `issues/01-public-file-routes.md`: split into 07–11; kept for its research details.
- `issues/06-agents-md-drift.md`: split into `.scratch/agents-md-drift/issues/01-03`. The last of those waits for the security fixes, so the docs describe the new access rules.

## Testing Decisions

- Test-first: each ticket starts with tests that fail on the current code.
- Every check gets at least one test where a tenant is **denied** (their assigned apartment is a different one) and one where they are **allowed** (on their assigned apartment).
- Follow the existing pattern in `backend/src/test/java/com/apartmentmanager/`: extend `AbstractIntegrationTest`, use `bearer(username)` and `createApartment(...)`, and assign the seeded `tenant` user with `tenant.setApartment(apt)`. See `PhotoTest`, `ProtocolTest` and `TicketTest`.
- Run with `./gradlew test` from `backend/`.

## Out of Scope

- The public presentation list includes every apartment, whatever its status.
- Ticket photo upload accepts any file type, with no image check.
- Other existing denials that return 500 (`ApartmentController.getById`) or an empty list.
- Wiring `paymentKeywords` into proof detection.
- The stored-file module refactor (candidate 1 from the architecture review).
- Carrying access over when a tenant moves to a different apartment.
