# Frontend Agent Guide

Frontend-specific context. Project-wide context (tech stack, roles, key files, image downscaling, e2e testing) is in the root `AGENTS.md`; backend routes and access rules are in `backend/AGENTS.md`; domain terms are in `GLOSSARY.md`.

## Stack notes

- Create React App (`react-scripts` 5.0.1), plain JavaScript. `typescript` is a devDependency but unused (no `.ts`/`.tsx`, no `tsconfig.json`).
- No state library: React Context (`AuthContext`) + `useState`.
- One global stylesheet, `src/App.css` (~3700 lines, no CSS modules).
- Scripts: `start`, `build`, `test:e2e` (Playwright, `playwright.config.cjs`). No lint or unit-test script.

## Source layout

```
frontend/src/
├── App.js                   # Routes + ProtectedRoute + AuthProvider
├── AuthContext.js           # user, token, login/logout, authHeader(), unread ticket count
├── authFiles.js             # Loading files that need a login (see "Protected files")
├── imageUtils.js            # prepareImage(): client-side downscale + HEIC conversion
├── Login.js
├── ApartmentList.js         # Dashboard: apartment rows, summary, manage mode, metadata, OCR keywords
├── ApartmentDetail.js       # One apartment: apartment documents, notes, contacts, billing documents, tickets
├── PaidBills.js             # Billing documents, embedded in ApartmentDetail
├── ManagePhotos.js          # Apartment photo upload/delete/set main
├── Tickets.js
├── InspectionForm.js        # Handover inspection (OWNER/ADMIN), uses SignaturePad
├── SignaturePad.js          # Canvas signature input
├── ApartmentApplications.js # Applications for one apartment (OWNER/ADMIN)
├── UserManagement.js
├── AuditLog.js
├── PresentationList.js      # Public list of apartments, opens ApplyModal
├── Presentation.js          # Public page for one apartment
├── ApplyModal.js            # Public application form
├── i18n.js
└── locales/                 # en, de, it, fr, ro
```

## Routes

| Path | Component | Login | Notes |
|---|---|---|---|
| `/login` | Login | No | |
| `/presentations` | PresentationList | No | Public |
| `/presentations/apartments/:id` | Presentation | No | Public; edit controls for OWNER/ADMIN |
| `/` | ApartmentList | Yes | Tenant sees only their assigned apartment |
| `/apartments/:id` | ApartmentDetail | Yes | Navigates to `/` on 403/404 |
| `/apartments/:id/photos` | ManagePhotos | Yes | |
| `/apartments/:id/inspections/new`, `/apartments/:id/inspections/:inspectionId` | InspectionForm | Yes | Redirects non OWNER/ADMIN to `/` |
| `/applications/:id` | ApartmentApplications | Yes | |
| `/users` | UserManagement | Yes | OWNER (link shown only to OWNER) |
| `/tickets` | Tickets | Yes | Supports `?apartmentId=` filter |
| `/audit` | AuditLog | Yes | OWNER (link shown only to OWNER) |

There are no role-based route guards. `ProtectedRoute` only checks login (shows `t('loading')`, else redirects to `/login`); role checks live inside each component, and the backend is the real enforcement.

## Auth

- `AuthContext` keeps `token` in `localStorage`, validates it on mount with `GET /api/auth/me`, and clears it if that fails.
- `login()` stores the token and sets `user` = `{ id, username, role, apartmentId }` (`apartmentId` only for TENANT).
- `authHeader()` returns `{ Authorization: 'Bearer <token>' }`. `isAuthenticated` = `!!token && !!user`.
- The context also exposes `unreadTickets` and `fetchUnreadCount()` for the tickets badge.

## API calls

- Most components define `const API = process.env.REACT_APP_API_URL || '';` and call `fetch(`${API}/api/...`, { headers: authHeader() })`.
- JSON bodies add `'Content-Type': 'application/json'`. File uploads send `FormData` with only `authHeader()` (the browser sets the multipart boundary).
- Dev: `package.json` `"proxy": "http://localhost:8080"`. Production: nginx routes `/api/` to the backend.
- After a mutation, components re-fetch the list rather than updating optimistically.

## Protected files

Only apartment photos are public (`/api/apartments/photos/...`), so a plain `<img src>` works only for them. Every other file (billing documents, apartment documents, ticket photos, applicant documents) needs the `Authorization` header, which `<img src>` and `<a href>` don't send. Use `authFiles.js`:

- `downloadAuthFile(url, headers, fileName)` to save a file (e.g. apartment documents in `ApartmentDetail`).
- `AuthImage` / `AuthImageButton` for protected images (ticket photos in `Tickets`).
- `fetchFileUrl(url, headers)` for a temporary `blob:` URL.

`PaidBills.openBillFile` and the applicant-document downloads in `ApartmentDetail`/`ApartmentApplications` do the same fetch-to-blob inline instead of using the helper.

## UI permissions

These only hide controls; the backend enforces access (see `backend/AGENTS.md`).

| Action | OWNER | ADMIN | TENANT |
|---|---|---|---|
| Manage mode on the dashboard (password gate via `POST /api/auth/verify-password`) | Yes | No | No |
| Add apartment (in manage mode) | Yes | — | No |
| Delete apartment, contacts CRUD, delete billing documents, edit metadata | Yes | No | No |
| Upload apartment documents, see/manage notes, see applications, inspections | Yes | Yes | No |
| Upload billing documents | Yes | Yes | Yes |
| Create tickets | No | No | Yes (assigned apartment) |
| Add ticket photos | Yes | Yes | Only on tickets they created; max 5 per ticket |
| Change ticket status | Yes | Yes | No |
| Edit presentation (on the Presentation page) | Yes | Yes | No |
| Users, audit log | Yes | No | No |

## Component notes

- **ApartmentList:** fetches `/api/apartments`, `/api/apartments/summary`, `/api/tickets/unread`; for OWNER also OCR keywords and application counts. Unread-tickets section shows for ADMIN only. Row photo is the first apartment photo, else `/placeholder.svg`. Row clicks use `stopPropagation` on inner buttons.
- **ApartmentDetail:** apartment document kinds in the selector: `HANDOVER_PROTOCOL`, `BILLS`, `PHOTOS`, `OTHER` (a `BILLS` apartment document is not a billing document). Upload accepts `.pdf,.doc,.docx,.jpeg,.jpg,.png`.
- **PaidBills:** props `{ apartmentId }`. Bill types are sent as English strings ("Monthly Maintenance Fee", "Electricity Bill", "Internet Subscription", "Other Payments") and mapped back to i18n keys. Document type is `bill` or `proof`. Grouped by month with key `` `${getFullYear()}-${getMonth()}` ``.
- **Tickets:** OWNER/ADMIN load `GET /api/tickets`; TENANT loads `GET /api/apartments/:apartmentId/tickets`. Creating a ticket posts the JSON first, then uploads each photo. Status colors: NEW `#3b82f6`, IN_PROGRESS `#f59e0b`, DONE `#22c55e`, REJECTED `#ef4444`.
- **UserManagement:** role badges `.role-owner`/`.role-admin`/`.role-tenant`; apartment assignment only for TENANT users; OWNER users have no delete button.
- **AuditLog:** auto-refresh every 5s; timestamps may be ISO strings or Java `[y, M, d, h, m, s]` arrays (month 1-based); badge class by action name (`LOGIN*`, `*CREATED*`, `*UPDATED*`, `*DELETED*`, `*UPLOADED*`, `*ASSIGNED*`).
- **Presentation:** carousel with thumbnails; save is two calls, `PUT /presentation` (`text/plain`) then `PUT /details` (JSON: price, description, status, availableFrom). Uses the `pres-*` CSS prefix.

## CSS conventions

- Primary blue `#1a73e8`, accent purple `#667eea`, background `#f0f2f5`.
- Cards: white, `border-radius: 12px`. Font stack starts with `-apple-system, BlinkMacSystemFont`.
- Buttons: `.btn-primary`, `.btn-back`, `.btn-upload`, `.btn-delete`, `.btn-manage`, `.btn-presentation`; size modifiers `.small`, `.large`, `.tiny`.
- Prefixes: `.pres-*` (presentation), `.paid-bills-*` (billing documents), `.row-*` (dashboard rows).
- Breakpoints: `max-width: 768px` and `600px`.

## i18n

- All five locales are bundled statically. Detection: `localStorage`, then `navigator`; fallback `en`; `escapeValue: false`.
- A new string needs its key in all five locale files.

## Known quirks

1. `Presentation.js` doesn't use the `API` constant (bare `/api/...` URLs) and reads the token from `localStorage` instead of `authHeader()`.
2. Navigation is mixed: `ApartmentList` and several links use `window.location.href` / `<a href>` (full reload); `ApartmentDetail` uses `useNavigate()`; `Presentation` uses `<Link>`.
3. `LANGUAGES` and `LanguageSwitcher` are copy-pasted in `ApartmentList`, `Tickets`, `UserManagement`, `AuditLog`, `Login` and `PresentationList`.
4. `AuditLog` fetches `allUsers` but the filter dropdown uses usernames found in the loaded logs.
5. Deletes use `window.confirm()`, no custom modal.
6. A few inline `style={{...}}` props exist (e.g. the notes textarea in `ApartmentDetail`), despite the single stylesheet.
