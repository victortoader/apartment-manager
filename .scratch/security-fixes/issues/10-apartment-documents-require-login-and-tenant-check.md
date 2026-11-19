# 10: Apartment documents require login and the tenant check

**What to build:** Apartment documents, such as handover protocols, are no longer public. Owner and admin can download any apartment document. A tenant can download only documents on their assigned apartment. The download button on the apartment page keeps working, because the page now sends the login with the request through a new shared frontend helper for downloading with the login.

**Blocked by:** None (can start immediately).

**Status:** ready-for-agent

Source: split from `01-public-file-routes.md`. See it for research details.

- [ ] Without a login, downloading an apartment document gets 401/403.
- [ ] A tenant downloading an apartment document of another apartment gets 403; on their assigned apartment, 200.
- [ ] Owner and admin can download any apartment document (200), including generated inspection protocols.
- [ ] A name that isn't an apartment document gets 404.
- [ ] The apartment document link is no longer public in the security configuration. The existing test that says it is public is updated.
- [ ] A shared frontend helper fetches a file with the login and gives the page a temporary link, which is released when no longer needed.
- [ ] The protocol download on the apartment page uses the helper and still works.
- [ ] Tests written first, failing on the current code. Existing e2e coverage of the protocol download, if any, still passes.
