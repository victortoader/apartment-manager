# 06 - Fix stale statements in AGENTS.md and merge AGENT.md

Status: ready-for-agent

Spec: `../spec.md`

## Problem

`AGENTS.md` says two things that are no longer true:

1. It says `BillPaymentService` "has `upload()` (MultipartFile) and `uploadFromBytes()` (byte[]) methods". `upload()` no longer exists.
2. It says proofs are detected from payment keywords (in the `OcrService` entry and under Key Patterns). Nothing reads `paymentKeywords`; proofs are detected some other way (check `OcrService` for how, e.g. negative amounts).

## What to do

- Check both statements against the code, then correct them so `AGENTS.md` describes what the code actually does.
- Don't change any code. Whether `paymentKeywords` *should* be used is out of scope.
- While you're there, add a pointer to `GLOSSARY.md` for domain terms, if the Agent skills section doesn't already cover it.
- Once ticket 01 is done, the file-access rules could also get a line under Key Patterns, but only if 01 has landed.

## Merge AGENT.md and remove it

The repo root also has `AGENT.md` (no "s"), an older backend-only guide last changed in commit `02fa733` (2026-08-03). No standard tool loads that file name and nothing links to it, so agents never see it. Some of its content isn't in `AGENTS.md`:

- Auth Flow, Two Authorization Patterns, Every Controller Does This
- Complete API Surface, Request Body Conventions, Audit Logging
- Database, File Storage, Error Handling
- Running Locally, Testing (`AbstractIntegrationTest`, test structure, JSON library), Code Conventions, Known Quirks

What to do:

1. Check each section against the current code. Drop what's no longer true; don't copy stale content.
2. Move the backend-specific sections that are still true into a new `backend/AGENTS.md` (loaded when agents work in `backend/`). Keep the root `AGENTS.md` for project-wide context, and don't duplicate content between the two.
3. The File Storage and authorization-pattern sections describe the access checks that tickets 01–05 change. If those tickets have landed, describe the new rules; if not, describe current behaviour and note that `.scratch/security-fixes/` changes it.
4. Delete `AGENT.md`.
5. Use the `GLOSSARY.md` terms (billing document, assigned apartment, apartment document, etc.) in the merged text.
