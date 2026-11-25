# 01: Correct the two stale statements in AGENTS.md

**What to build:** An agent reading the root `AGENTS.md` gets an accurate picture of billing documents. The description of the billing service no longer mentions an upload method that doesn't exist. The description of how a proof of payment is detected matches what the code actually does. The file also points agents to `GLOSSARY.md` for domain terms.

**Blocked by:** None (can start immediately).

**Status:** resolved

Source: `.scratch/security-fixes/issues/06-agents-md-drift.md`

- [x] The billing service entry lists only the upload methods that exist in the code.
- [x] Both places that describe proof detection (the OCR service entry and Key Patterns) state the real mechanism, checked against the code.
- [x] `AGENTS.md` points to `GLOSSARY.md` for domain terms, unless the Agent skills section already does.
- [x] No code changes. Whether payment keywords *should* be used is out of scope.

## Comments

2026-11-25 — Resolved. Findings checked against the code:
- `BillPaymentService` has only `uploadFromBytes()`; `BillPaymentController` and `EmailFetchService` both call it. No `upload(MultipartFile)` exists.
- Proof is never auto-detected. `documentType` comes from the upload request param (default `bill`); `EmailFetchService` hardcodes `bill`. Nothing reads `paymentKeywords`. OCR's minus-sign pattern only makes the amount negative.
- GLOSSARY pointer already present in the Agent skills section, so left as is.
- Also annotated the `OcrKeywords` note to say `paymentKeywords` is unused.
