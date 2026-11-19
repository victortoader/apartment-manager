# 03 - Restrict amount correction to the tenant's assigned apartment

Status: resolved

Spec: `../spec.md`

## Problem

`backend/src/main/java/com/apartmentmanager/controller/BillPaymentController.java:119`: `PUT /api/bills/{id}/amount` has no tenant check, so any tenant can change the amount on any apartment's billing document.

## What to do

OWNER and ADMIN can correct the amount on any billing document. A TENANT can correct it only on billing documents of their assigned apartment. Anything else returns **403** and the amount stays unchanged.

Tenants keep the right to correct OCR amounts on their own apartment's billing documents. Don't remove it.

Use the same inline check style as `getBills` (`:38-45`), comparing the billing document's apartment with the tenant's assigned apartment.

## Tests (write first)

- A tenant assigned to apartment A corrects an amount on a billing document of apartment B → 403, and the stored amount is unchanged.
- A tenant assigned to apartment A corrects an amount on a billing document of apartment A → 200, and the amount is updated.
- An admin corrects any billing document's amount → 200.

## Comments

- 2026-11-19: Done. `PUT /api/bills/{id}/amount` returns 403 (amount unchanged) when a tenant corrects a billing document outside their assigned apartment. Covered by `BillingDocumentAmountCorrectionTest` (5 tests, incl. owner and unassigned tenant after review); full backend suite green. Not committed yet.
