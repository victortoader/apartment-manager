# 01: Correct the two stale statements in AGENTS.md

**What to build:** An agent reading the root `AGENTS.md` gets an accurate picture of billing documents. The description of the billing service no longer mentions an upload method that doesn't exist. The description of how a proof of payment is detected matches what the code actually does. The file also points agents to `GLOSSARY.md` for domain terms.

**Blocked by:** None (can start immediately).

**Status:** resolved

Source: `.scratch/security-fixes/issues/06-agents-md-drift.md`

- [ ] The billing service entry lists only the upload methods that exist in the code.
- [ ] Both places that describe proof detection (the OCR service entry and Key Patterns) state the real mechanism, checked against the code.
- [ ] `AGENTS.md` points to `GLOSSARY.md` for domain terms, unless the Agent skills section already does.
- [ ] No code changes. Whether payment keywords *should* be used is out of scope.

## Comments

- 2026-11-19: Done. Fixed the `upload()` line and both proof-detection statements: document type is chosen by the uploader, nothing detects it, and `paymentKeywords` is unused. Added a "use GLOSSARY.md terms" line under Domain docs. Not committed yet.
