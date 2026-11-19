# 07: Refuse file names that escape the uploads folder

**What to build:** No file route can be tricked into serving a file outside the uploads folder. A request whose file name resolves outside it (for example an encoded `..`) is refused on every file route. No other route behaviour changes.

**Blocked by:** None (can start immediately).

**Status:** ready-for-agent

Source: split from `01-public-file-routes.md`. See it for research details.

- [ ] A file name containing an encoded `..` (or otherwise resolving outside the uploads folder) is refused, not served, on every file route.
- [ ] Normal file names on every route behave exactly as before.
- [ ] Test written first, failing on the current code.
