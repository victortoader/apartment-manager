# 02 - Restrict contacts to the tenant's assigned apartment

Status: resolved

Spec: `../spec.md`

## Problem

`backend/src/main/java/com/apartmentmanager/controller/ContactController.java:22`: `getContacts` has no tenant check, so any tenant can read any apartment's contacts.

## What to do

OWNER and ADMIN can read the contacts of any apartment. A TENANT can read only the contacts of their assigned apartment. Any other apartment returns **403**.

Use the same inline check as `BillPaymentController.getBills` (`:38-45`): look up the user, and if they're a TENANT whose assigned apartment is null or a different apartment, return 403. The method will need `Authentication` and to return a `ResponseEntity`.

## Tests (write first)

- A tenant assigned to apartment A gets the contacts of apartment B → 403.
- A tenant assigned to apartment A gets the contacts of apartment A → 200 with the contacts.
- A tenant with no assigned apartment → 403.
- An admin gets the contacts of any apartment → 200.

## Comments

- 2026-11-19: Done. `ContactController.getContacts` now returns 403 to a tenant asking for an apartment other than their assigned one. Covered by `ContactTest` (5 tests); full backend suite green. Not committed yet.
