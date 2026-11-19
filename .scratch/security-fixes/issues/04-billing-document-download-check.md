# 04 - Restrict billing document downloads to the tenant's assigned apartment

Status: resolved

Spec: `../spec.md`

## Problem

`backend/src/main/java/com/apartmentmanager/controller/BillPaymentController.java:76-96`: `GET /api/bills/{fileName}` requires a login but doesn't check the apartment, so any tenant can download any apartment's billing documents.

## What to do

- Look up the billing document by its stored file name. If there's no match, return 404, and never serve a file that isn't a billing document.
- OWNER and ADMIN can download any billing document.
- A TENANT can download only those on their assigned apartment; otherwise **403**.

Side note found during research: `BillPaymentService` reads the upload folder from the JVM system property `app.upload.dir` (`BillPaymentService.java:39-40`), not from the Spring property. So in tests, bills are written to `./uploads` instead of the test's temp folder. If this gets in the way of the tests, inject the Spring `app.upload.dir` value instead. Otherwise leave it alone.

## Tests (write first)

- A tenant assigned to apartment A downloads a billing document of apartment B → 403.
- A tenant assigned to apartment A downloads a billing document of apartment A → 200.
- Downloading a name that isn't a billing document (e.g. an apartment photo's name) → 404.
- An admin downloads any billing document → 200.

Frontend: no change. `PaidBills.js:134-137` already sends the token.

## Comments

- 2026-11-19: Done. `GET /api/bills/{fileName}` serves only registered billing documents (else 404); a tenant gets 403 outside their assigned apartment. Covered by `FileAccessTest` (tests written first; full backend suite and e2e suite green). Not committed yet.
