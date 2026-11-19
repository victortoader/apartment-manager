# 01 - Lock down public file routes

Status: ready-for-agent

Spec: `../spec.md`

## Problem

`backend/src/main/java/com/apartmentmanager/security/SecurityConfig.java:40-43` makes four file routes public. Every one of them calls `PhotoStorageService.load` (`service/PhotoStorageService.java:44-46`), which returns any file in the shared uploads folder. So billing documents, applicant documents and handover protocols can be downloaded without logging in, through any of these routes:

| Route | Handler |
|---|---|
| `GET /api/apartments/photos/{fileName}` | `ApartmentController.getPhoto` (`:163-178`) |
| `GET /api/apartments/protocols/{fileName}` | `ApartmentController.getProtocolFile` (`:213-231`) |
| `GET /api/inspections/photos/{fileName}` | `InspectionController.getPhoto` (`:144-159`) |
| `GET /api/tickets/photos/{fileName}` | `TicketController.getPhoto` (`:161-176`) |

## What to do

1. **Apartment photos stay public**, but the route serves only a name listed in some apartment's `photoPaths`. Any other name gets a 404.
2. **Apartment documents** (`/api/apartments/protocols/{fileName}`) need a login. The name must match a `HandoverProtocol` record, otherwise 404. Generated inspection PDFs also have a `HandoverProtocol` record (`InspectionService.java:331-334`). OWNER and ADMIN can download any apartment document; a tenant only those on their assigned apartment, otherwise 403.
3. **Ticket photos** (`/api/tickets/photos/{fileName}`) need a login. The name must be in some ticket's `photoPaths` (table `ticket_photos`), otherwise 404. OWNER and ADMIN can see all of them; a tenant only on tickets they created (same rule as `TicketController.getTicket`, `:113-116`), otherwise 403.
4. **Delete `GET /api/inspections/photos/{fileName}`.** Nothing in the frontend uses it; inspection photos are downloaded as a zip through `/api/inspections/{id}/photos`. Keep the DELETE endpoint on the same path (used by `InspectionForm.js:233`).
5. **Reject path tricks:** `PhotoStorageService.load` must refuse any name whose resolved path is outside the uploads folder (e.g. an encoded `..`).
6. **Update `SecurityConfig`:** only `/api/apartments/photos/**` stays `permitAll`.
7. **Frontend, in this same ticket** so both sides deploy together: add a shared helper that fetches a file with `authHeader()` and returns a blob URL, following the pattern in `PaidBills.js:134-137`. Use it for:
   - ticket photos in `frontend/src/Tickets.js:262-263` (both the `<a href>` and the `<img src>`)
   - the protocol download link in `frontend/src/ApartmentDetail.js:300`
   - Revoke blob URLs when they're no longer needed.

   Leave apartment photo `<img>` tags as they are; that route stays public.

## Tests (write first)

- Without a login, fetching a billing document's stored name via `/api/apartments/photos/{name}` → 404.
- Without a login, `/api/apartments/protocols/{name}` and `/api/tickets/photos/{name}` → 401/403.
- A tenant fetching an apartment document of another apartment → 403. A tenant on their assigned apartment → 200.
- A tenant fetching a photo on a ticket created by someone else → 403. Their own ticket → 200.
- A billing document's name requested via the ticket-photo route → 404.
- `GET /api/inspections/photos/{name}` no longer serves files.
- A name containing an encoded `..` → rejected, not served.
- Update tests that currently say these routes are public: `PhotoTest.java:78-91` (`getPhoto_isPublicEndpoint`, still valid for apartment photos), `ProtocolTest.java:198-206` (`getProtocolFile_isPublicEndpoint`) and `SecurityTest.java:61-69`.
- Frontend: if there's e2e coverage of ticket photos or the protocol download, check that it still passes.

## Out of scope

Image validation for ticket photo uploads; the stored-file module refactor.
