# 03: Update the agent docs to the new access rules

**What to build:** Once the security fixes have landed, an agent reading the docs learns the new file-access and tenant-access rules instead of the old ones.

**Blocked by:** 02, and `.scratch/security-fixes/issues/` 02–05 and 07–11.

**Status:** resolved

Source: `.scratch/security-fixes/issues/06-agents-md-drift.md`

- [x] The file storage and authorization parts of `backend/AGENTS.md` describe the rules from `.scratch/security-fixes/spec.md`, checked against the landed code. Those rules cover:
  - who can reach what;
  - tenants limited to their assigned apartment;
  - 403 on denial;
  - only apartment photos staying public;
  - each file route serving only its own kind.
- [x] The "security-fixes will change this" note is removed.
- [x] The root `AGENTS.md` has one line about file access under Key Patterns.
- [x] No code changes.

## Comments

- 2026-11-25: Done. All blockers had landed (agents-md-drift 02 and security-fixes 02–05, 07–11 resolved). Checked every file route in the code: apartment photos are the only public files and serve only apartment photos; apartment documents and billing documents download with a tenant check; ticket photos only for the tenant who created the ticket; applicant documents OWNER/ADMIN; the single inspection-photo route is gone (zip download only, OWNER/ADMIN); every route returns 404 for another kind of file and resolves names via `PhotoStorageService.resolveInside`. Updated the `backend/AGENTS.md` API table, replaced the "Pending changes" note with the access rules, rewrote File storage, and added one File access line under Key Patterns in the root `AGENTS.md`. No code changes.
