# 02: Move the still-true backend guide into backend/AGENTS.md and delete AGENT.md

**What to build:** An agent working in the backend automatically gets an accurate backend guide. The old `AGENT.md` at the repo root (no "s") is never loaded by any tool, and parts of it are out of date. Its still-true content moves into `backend/AGENTS.md` and the old file is removed.

**Blocked by:** None (can start immediately).

**Status:** ready-for-agent

Source: `.scratch/security-fixes/issues/06-agents-md-drift.md`

- [ ] Every `AGENT.md` section has been checked against the current code. The sections are Tech Stack, Project Structure, Auth Flow, Two Authorization Patterns, Every Controller Does This, Complete API Surface, Request Body Conventions, Audit Logging, Database, File Storage, Error Handling, Running Locally, Testing, Code Conventions and Known Quirks.
- [ ] Anything no longer true is dropped, not copied.
- [ ] Still-true backend content lives in `backend/AGENTS.md`.
- [ ] The root `AGENTS.md` keeps project-wide context only, and nothing is duplicated between the two files.
- [ ] File storage and authorization are described as they work today, with a note that `.scratch/security-fixes/` changes them.
- [ ] The merged text uses `GLOSSARY.md` terms (billing document, assigned apartment, apartment document, etc.).
- [ ] `AGENT.md` is deleted.
- [ ] No code changes.
