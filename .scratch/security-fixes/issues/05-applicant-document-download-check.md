# 05 - Restrict applicant document downloads to owner and admin

Status: resolved

Spec: `../spec.md`

## Problem

`backend/src/main/java/com/apartmentmanager/controller/ApplicationController.java:75-95`: `GET /api/applications/files/{fileName}` requires a login but has no role check, so any tenant can download applicants' documents (likely ID papers or payslips).

## What to do

- Only OWNER and ADMIN may download (`@PreAuthorize("hasAnyRole('OWNER','ADMIN')")`). A TENANT gets **403**.
- The name must belong to an application's documents; otherwise return 404, and never serve other kinds of file through this route.
- The public `POST /api/applications` (submitting an application) stays public and unchanged.

## Tests (write first)

- A tenant downloads an applicant document → 403.
- An admin or the owner downloads an applicant document → 200.
- Downloading a name that isn't an applicant document → 404.
- Without a login → 401/403.

Frontend: no change. `ApartmentApplications.js:142-145` and `ApartmentDetail.js:150-153` already send the token, and only owner/admin pages show applications.

## Comments

- 2026-11-19: Done. `GET /api/applications/files/{fileName}` is OWNER/ADMIN only and serves only names listed on an application (else 404). Covered by `FileAccessTest` (tests written first; full backend suite and e2e suite green). Not committed yet.
