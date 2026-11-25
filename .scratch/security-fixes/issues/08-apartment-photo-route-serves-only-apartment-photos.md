# 08: The apartment photo link serves only apartment photos

**What to build:** Apartment photos stay public, so the public presentation pages keep working. The apartment photo link no longer hands out other files, such as billing documents, applicant documents or handover protocols, to anyone who knows their stored name.

**Blocked by:** None (can start immediately).

**Status:** resolved

Source: split from `01-public-file-routes.md`. See it for research details.

- [ ] Without a login, a photo listed on some apartment is served (200).
- [ ] Without a login, a billing document's stored name requested through the apartment photo link gets 404.
- [ ] Any name not listed as an apartment photo gets 404.
- [ ] Existing tests that say the apartment photo link is public still pass.
- [ ] Tests written first, failing on the current code.

## Comments

- 2026-11-19: Done. The apartment photo route serves only names in some apartment's photos (else 404); it stays public. Covered by `FileAccessTest` (tests written first; full backend suite and e2e suite green). Not committed yet.
