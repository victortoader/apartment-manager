# 09: Remove the unused public inspection-photo download

**What to build:** Single inspection photos can no longer be downloaded through their public link. Nothing in the frontend uses it; inspection photos are still downloaded as a zip from the inspection itself. Deleting an inspection photo keeps working.

**Blocked by:** None (can start immediately).

**Status:** resolved

Source: split from `01-public-file-routes.md`. See it for research details.

- [ ] Fetching a single inspection photo by name no longer serves the file.
- [ ] The link is no longer listed as public in the security configuration.
- [ ] Deleting an inspection photo still works.
- [ ] Downloading an inspection's photos as a zip still works.
- [ ] Tests written first, failing on the current code.

## Comments

- 2026-11-19: Done. Removed `GET /api/inspections/photos/{fileName}` and its permitAll; DELETE on the same path and the zip download are unchanged. Covered by `FileAccessTest` (tests written first; full backend suite and e2e suite green). Not committed yet.
