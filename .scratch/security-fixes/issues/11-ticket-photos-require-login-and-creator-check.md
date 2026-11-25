# 11: Ticket photos require login and the creator check

**What to build:** Ticket photos are no longer public. Owner and admin see every ticket photo. A tenant sees photos only on tickets they created, the same rule as for viewing the ticket itself. Photos on the tickets page keep showing, because the page now loads them with the login using the shared helper from ticket 10.

**Blocked by:** 10 (adds the shared frontend helper).

**Status:** resolved

Source: split from `01-public-file-routes.md`. See it for research details.

- [ ] Without a login, fetching a ticket photo gets 401/403.
- [ ] A tenant fetching a photo on a ticket created by someone else gets 403; on their own ticket, 200.
- [ ] Owner and admin can fetch any ticket photo (200).
- [ ] A name that isn't a ticket photo (e.g. a billing document's name) gets 404.
- [ ] The ticket photo link is no longer public in the security configuration. The existing test that says it is public is updated.
- [ ] On the tickets page, both the photo thumbnail and its link use the shared helper and still work.
- [ ] Tests written first, failing on the current code. Existing e2e coverage of ticket photos, if any, still passes.

## Comments

- 2026-11-19: Done. Ticket photos need a login, must be on a ticket (else 404), and a tenant sees only photos on tickets they created (else 403). The tickets page loads thumbnails and opens photos through `authFiles.js`. Covered by `FileAccessTest` (tests written first; full backend suite and e2e suite green). Not committed yet.
