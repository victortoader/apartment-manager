# Frontend Agent Guide

## Tech Stack
- React 18.2 (JavaScript — no TypeScript despite it being a devDependency)
- React Router v6, react-i18next (5 languages: en/de/it/fr/ro)
- Create React App (react-scripts 5.0.1)
- No state management library — React Context + useState
- No CSS preprocessor — single monolithic `App.css` (2269 lines)

## Project Structure
```
frontend/src/
├── App.js              # Routes + AuthProvider wrapper
├── AuthContext.js       # JWT auth state, login/logout, authHeader()
├── Login.js             # Login page
├── ApartmentList.js     # Dashboard — apartment cards, summary, manage mode, metadata
├── ApartmentDetail.js   # Detail — photos, protocols, notes, contacts, bills, tickets
├── PaidBills.js         # Sub-component embedded in ApartmentDetail
├── Tickets.js           # Support ticket system with photo upload
├── UserManagement.js    # Admin user CRUD + apartment assignment
├── AuditLog.js          # Audit trail with auto-refresh (OWNER only)
├── Presentation.js      # Public-facing apartment page (unauthenticated visitors)
├── i18n.js              # i18next init (localStorage detection, en fallback)
├── App.css              # All styles (2269 lines, no modules)
└── locales/             # en.json, de.json, it.json, fr.json, ro.json
```

## Routes
| Path | Component | Protected | Role Notes |
|------|-----------|-----------|------------|
| `/login` | Login | No | |
| `/presentations/apartments/:id` | Presentation | No | Public, editable by OWNER |
| `/` | ApartmentList | Yes | Tenant sees only their apt |
| `/apartments/:id` | ApartmentDetail | Yes | Tenant limited to own apt |
| `/users` | UserManagement | Yes | OWNER only (enforced in component) |
| `/tickets` | Tickets | Yes | OWNER/ADMIN see all, Tenant sees own |
| `/audit` | AuditLog | Yes | OWNER only (enforced in component) |

No role-based route guards — role checks happen inside each component.

## Auth Flow

1. `AuthContext` stores `user`, `token` (from localStorage), `loading`
2. On mount: if token exists, calls `GET /api/auth/me` to validate → sets `user`
3. `login(username, password)` → `POST /api/auth/login` → stores token, sets user
4. `authHeader()` → `{ Authorization: 'Bearer <token>' }` — used by all API calls
5. `isAuthenticated` → `!!token && !!user` (both must be truthy)
6. `ProtectedRoute` in App.js: shows `t('loading')` while loading, redirects to `/login` if not authed

User object shape: `{ id, username, role, apartmentId }` — `apartmentId` only for TENANT.

## API Communication Pattern

Every component (except Presentation.js) defines:
```javascript
const API = process.env.REACT_APP_API_URL || '';
```

Standard fetch pattern:
```javascript
const res = await fetch(`${API}/api/endpoint`, { headers: authHeader() });
const data = await res.json();
```

POST with JSON:
```javascript
const res = await fetch(`${API}/api/endpoint`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json', ...authHeader() },
  body: JSON.stringify(payload)
});
```

File upload (no Content-Type header — browser sets multipart boundary):
```javascript
const formData = new FormData();
formData.append('file', file);
await fetch(`${API}/api/endpoint`, {
  method: 'POST',
  headers: authHeader(),
  body: formData
});
```

Dev proxy: `package.json` has `"proxy": "http://localhost:8080"`.
Production: nginx routes `/api/` to backend container.

## Role Permissions (UI Enforced)

| Action | OWNER | ADMIN | TENANT |
|--------|-------|-------|--------|
| View apartments | All | All | Own only |
| Create apartment | Yes | Yes | No |
| Delete apartment | Yes | No | No |
| Enter manage mode | Yes (password gate) | No | No |
| Upload photos (manage mode) | Yes | Yes | No |
| Delete bills | Yes | No | No |
| Upload bills | No | No | Yes |
| Manage notes | Yes | Yes | No (can't even see) |
| Manage contacts | Yes | No | No (can read) |
| Upload protocols | Yes | Yes | No |
| Create tickets | No explicit form | No explicit form | Yes (own apt) |
| Change ticket status | Yes | Yes | No |
| Manage users | Yes | No | No |
| View audit log | Yes | No | No |
| Edit presentation | Yes (on Presentation page) | No | No |
| Edit metadata | Yes | No | No |

## Manage Mode (ApartmentList)

OWNER-only, password-gated:
1. OWNER clicks "Manage" → modal appears
2. Enters password → `POST /api/auth/verify-password`
3. On success: `manageMode = true` → shows add apartment form, photo upload buttons, delete buttons, metadata editor
4. Click "Done" → exits manage mode

## Data Refresh Pattern

After every mutation (create/update/delete), components re-fetch the full list. No optimistic updates.

## Navigation Inconsistency

- `ApartmentList`: `window.location.href = ...` (full page reload)
- `ApartmentDetail`: `useNavigate()` (SPA navigation)
- `Presentation`: `<Link>` components (SPA navigation)
- `Tickets`: `href="/tickets?apartmentId=${id}"` (full page reload via anchor)

## Presentation.js Quirk

This component does NOT use the `API` constant. It uses bare relative URLs:
```javascript
const res = await fetch(`/api/apartments/${id}/presentation`);
```
And reads the token directly from localStorage instead of using `authHeader()`:
```javascript
const token = localStorage.getItem('token');
headers: { 'Authorization': `Bearer ${token}` }
```

## LanguageSwitcher Duplication

The `LANGUAGES` array and `LanguageSwitcher` component are copy-pasted into 4 files: `ApartmentList.js`, `Tickets.js`, `UserManagement.js`, `AuditLog.js`. Not extracted to a shared module.

## Component Details

### ApartmentList (Dashboard)
- Fetches: `GET /api/apartments`, `GET /api/apartments/summary`, `GET /api/tickets/unread`
- Summary map: `summaries` array indexed by `id` into lookup object for O(1) access
- Photo: first photo only via `/api/apartments/photos/${apt.photoPaths[0]}`, fallback to `/placeholder.svg`
- Metadata editor: inline, owner-only, uses `stopPropagation` to prevent row navigation
- Unread tickets section: ADMIN only, shows list inline on dashboard

### ApartmentDetail
- Fetches: apartment, protocols, contacts, notes (all on mount)
- Embeds `<PaidBills apartmentId={id} />` inline
- Document type selector: `<select>` with `HANDOVER_PROTOCOL`, `BILLS`, `PHOTOS`, `OTHER`
- Protocol upload accepts: `.pdf,.doc,.docx,.jpeg,.jpg,.png`
- On 403/404 from apartment fetch → navigates to `/`
- Link to tickets: `href="/tickets?apartmentId=${id}"`

### PaidBills (Sub-component)
- Props: `{ apartmentId }`
- Bill types sent to API as English strings: "Monthly Maintenance Fee", "Electricity Bill", "Internet Subscription", "Other Payments"
- Bills grouped by month using `date.getFullYear()-date.getMonth()` as key
- File link uses `storedFileName` (not `originalFileName`): `/api/bills/${bill.storedFileName}`

### Tickets
- OWNER/ADMIN: fetches `GET /api/tickets` (all)
- TENANT: fetches `GET /api/apartments/:user.apartmentId/tickets`
- Supports `?apartmentId=` query param filter
- Creation: first POST ticket JSON, then loops through selected files uploading each to `POST /api/tickets/:ticketId/photos`
- Status colors: NEW `#3b82f6`, IN_PROGRESS `#f59e0b`, DONE `#22c55e`, REJECTED `#ef4444`
- Photo limit: 5 per ticket (client-side enforced)
- Tenant can only add photos to own-created tickets

### UserManagement
- Fetches users + apartments
- Role badges: `.role-owner` (blue), `.role-admin` (green), `.role-tenant` (amber)
- Apartment assignment dropdown: only for TENANT-role users
- Delete guard: OWNER users cannot be deleted (button hidden)

### AuditLog
- Auto-refresh: `setInterval(fetchLogs, 5000)` when enabled
- Filter dropdown uses `uniqueUsernames` derived from current logs (not from the separately fetched `allUsers`)
- Timestamp parsing handles both ISO strings and Java `[year, month, day, hour, min, sec]` arrays (month 1-indexed)
- Action badge class by prefix: `LOGIN`→`action-login`, `CREATED`→`action-create`, `UPDATED`→`action-update`, `DELETED`→`action-delete`, `UPLOADED`→`action-upload`, `ASSIGNED`→`action-assign`

### Presentation
- NOT protected — unauthenticated visitors see the page
- Edit controls shown only if `isOwner`
- Photo gallery: carousel with prev/next, counter `1/N`, thumbnail strip
- Save: two sequential API calls — first saves presentation text as `text/plain`, then saves price+description as JSON
- Uses `pres-*` CSS class prefix, separate design system from admin UI

## CSS Conventions (App.css)

- Primary blue: `#1a73e8`
- Accent purple: `#667eea` (manage button, presentation, ticket borders)
- Background: `#f0f2f5`
- Cards: white, `border-radius: 12px`, `box-shadow: 0 2px 8px rgba(0,0,0,0.1)`
- Font: `-apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif`
- Button classes: `.btn-primary`, `.btn-back`, `.btn-upload`, `.btn-delete`, `.btn-manage`, `.btn-presentation`
- Size modifiers: `.small`, `.large`, `.tiny`
- Component prefixes: `.pres-*` (presentation), `.paid-bills-*` (bills), `.row-*` (apartment list rows)
- Responsive: `@media (max-width: 768px)` and `@media (max-width: 600px)`

## i18n

- Languages loaded statically (no lazy loading)
- Detection: localStorage → navigator, fallback: `en`
- `escapeValue: false` (React handles XSS)
- Adding a translation: add key to all 5 locale files, use `t('key.subkey')` in component

## Known Quirks / Things to Watch

1. **TypeScript is a devDependency but unused** — no `.tsx` files, no `tsconfig.json`
2. **No lint or test scripts** in package.json — only `start` and `build`
3. **`handlePhotoUpload` in ApartmentList has no error handling** — doesn't check `res.ok`
4. **`window.confirm()` for all delete actions** — no custom confirmation modal
5. **Bill type is English strings** — "Monthly Maintenance Fee" sent to API, not machine-readable enum
6. **`allUsers` fetched in AuditLog but not used for filter dropdown** — uses `uniqueUsernames` from logs instead
7. **Single CSS file** — 2269 lines, no CSS modules, no scoping
8. **One inline style** in `ApartmentDetail.js:381` on textarea
9. **`.npmrc` copied in Dockerfile** — may have private registry reference
