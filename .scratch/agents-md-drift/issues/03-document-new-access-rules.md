# 03: Update the agent docs to the new access rules

**What to build:** Once the security fixes have landed, an agent reading the docs learns the new file-access and tenant-access rules instead of the old ones.

**Blocked by:** 02, and `.scratch/security-fixes/issues/` 02–05 and 07–11.

**Status:** ready-for-agent

Source: `.scratch/security-fixes/issues/06-agents-md-drift.md`

- [ ] The file storage and authorization parts of `backend/AGENTS.md` describe the rules from `.scratch/security-fixes/spec.md`, checked against the landed code. Those rules cover:
  - who can reach what;
  - tenants limited to their assigned apartment;
  - 403 on denial;
  - only apartment photos staying public;
  - each file route serving only its own kind.
- [ ] The "security-fixes will change this" note is removed.
- [ ] The root `AGENTS.md` has one line about file access under Key Patterns.
- [ ] No code changes.
