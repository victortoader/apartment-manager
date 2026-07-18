# Apartment Manager

A full-stack apartment management portal with JWT-based role-based access control.

## Tech Stack

- **Backend:** Spring Boot 4.1.0, Java 21, Gradle
- **Frontend:** React 18, React Router
- **Database:** H2 (local dev) / PostgreSQL 16 (Docker/production)
- **Auth:** JWT (jjwt 0.12.x), BCrypt, Spring Security 7.1
- **Deployment:** Docker Compose, nginx reverse proxy

## Roles

| Role | Permissions |
|------|-------------|
| **OWNER** | Full access: CRUD apartments, manage users, view audit logs, delete anything |
| **ADMIN** | Read/create apartments, manage tickets, upload bills, no delete |
| **TENANT** | View own apartment, create/upload tickets, upload bill proofs |

## Default Users

| Username | Password | Role |
|----------|----------|------|
| owner | owner | OWNER |
| admin | admin | ADMIN |
| tenant | tenant | TENANT |
| tenant2 | tenant2 | TENANT |

## Prerequisites

- Java 21+
- Node.js 20+
- Docker + Docker Compose (for PostgreSQL / production)
- Gradle (or use included `./gradlew`)

## Quick Start - Local Development (H2)

No database setup needed. H2 runs in-memory.

```bash
# Run backend
./gradlew bootRun

# In a separate terminal, run frontend
cd frontend
npm install
npm start
```

- Frontend: http://localhost:3000
- Backend API: http://localhost:8080/api

## Run with PostgreSQL (Local)

```bash
# 1. Start PostgreSQL in Docker
docker compose up -d db

# 2. Start the backend with PostgreSQL profile
./gradlew bootRun --args='--spring.profiles.active=postgres'

# 3. In a separate terminal, run frontend
cd frontend
npm install
npm start
```

Or use the convenience script:
```bash
chmod +x start-postgres.sh
./start-postgres.sh
```

## Build and Run with Docker

```bash
# Build and start all services (backend + frontend + PostgreSQL)
docker compose up -d --build

# View logs
docker compose logs -f backend

# Stop all services
docker compose down

# Stop and remove volumes (resets database)
docker compose down -v
```

- Frontend: http://localhost:3000
- Backend API: http://localhost:8080/api

## Run Tests

```bash
./gradlew test
```

All tests use H2 in-memory database (no external dependencies needed).

## API Endpoints

### Auth

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/login` | Public | Login, returns JWT |
| GET | `/api/auth/me` | Bearer | Current user info |
| POST | `/api/auth/verify-password` | Bearer | Verify password |

### Apartments

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/apartments` | All | List apartments (filtered for tenants) |
| GET | `/api/apartments/summary` | All | Apartments with recent bills + ticket counts |
| GET | `/api/apartments/{id}` | All | Single apartment |
| POST | `/api/apartments` | Owner/Admin | Create apartment |
| DELETE | `/api/apartments/{id}` | Owner | Delete apartment |
| POST | `/api/apartments/{id}/photos` | All | Upload photo |
| GET | `/api/apartments/{id}/presentation` | Public | Presentation page data |
| PUT | `/api/apartments/{id}/presentation` | Owner | Update presentation |
| PUT | `/api/apartments/{id}/details` | Owner | Update price/description |

### Tickets

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/tickets` | Owner/Admin | All tickets |
| GET | `/api/tickets/unread/count` | Owner/Admin | Unread count |
| POST | `/api/apartments/{id}/tickets` | All | Create ticket |
| PATCH | `/api/tickets/{id}` | Owner/Admin | Update status |
| POST | `/api/tickets/{id}/photos` | All | Upload photo |

### Bills

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/apartments/{id}/bills` | All | List bills |
| POST | `/api/apartments/{id}/bills` | Owner/Admin/Tenant | Upload bill |
| DELETE | `/api/bills/{id}` | Owner | Delete bill |

### Contacts

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/apartments/{id}/contacts` | All | List contacts |
| POST | `/api/apartments/{id}/contacts` | Owner | Create contact |
| PUT | `/api/contacts/{id}` | Owner | Update contact |
| DELETE | `/api/contacts/{id}` | Owner | Delete contact |

### Notes

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/apartments/{id}/notes` | Owner/Admin | List notes |
| POST | `/api/apartments/{id}/notes` | Owner/Admin | Create note |
| PUT | `/api/notes/{id}` | Owner/Admin | Update note |
| DELETE | `/api/notes/{id}` | Owner/Admin | Delete note |

### User Management

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/users` | Owner | List all users |
| POST | `/api/users` | Owner | Create user |
| PUT | `/api/users/{id}/apartment` | Owner | Assign apartment |
| DELETE | `/api/users/{id}` | Owner | Delete user |

### Audit Log

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/audit` | Owner | All audit logs |
| GET | `/api/audit?username=X` | Owner | Filter by user |

## Environment Variables

| Variable | Default (H2) | Docker/PostgreSQL | Description |
|----------|--------------|-------------------|-------------|
| `DB_URL` | `jdbc:h2:mem:apartments` | `jdbc:postgresql://db:5432/apartment-management-db` | Database URL |
| `DB_DRIVER` | `org.h2.Driver` | `org.postgresql.Driver` | JDBC driver |
| `DB_USER` | `sa` | `apartment_user` | Database username |
| `DB_PASS` | (empty) | `changeme` | Database password |
| `DB_DIALECT` | `org.hibernate.dialect.H2Dialect` | `org.hibernate.dialect.PostgreSQLDialect` | Hibernate dialect |
| `SPRING_PROFILES_ACTIVE` | (none) | `postgres` | Active Spring profile |
| `app.seed.enabled` | `true` | `true` | Seed test data on startup |

## Project Structure

```
demo/
├── src/main/java/com/example/demo/
│   ├── model/          # JPA entities (Apartment, User, Ticket, etc.)
│   ├── controller/     # REST controllers
│   ├── service/        # Business logic + seed data
│   ├── repository/     # Spring Data JPA repositories
│   └── security/       # JWT filter, SecurityConfig
├── src/test/           # Integration tests (H2)
├── frontend/
│   ├── src/
│   │   ├── App.js              # Routes
│   │   ├── AuthContext.js      # Auth state + JWT
│   │   ├── ApartmentList.js    # Apartment dashboard
│   │   ├── ApartmentDetail.js  # Single apartment view
│   │   ├── Tickets.js          # Ticket management
│   │   ├── Presentation.js     # Public presentation page
│   │   ├── PaidBills.js        # Bill upload/view
│   │   ├── UserManagement.js   # User admin (owner)
│   │   └── AuditLog.js         # Audit log (owner)
│   └── nginx.conf      # Production nginx config
├── docker-compose.yml
├── Dockerfile.backend
├── start-postgres.sh
└── build.gradle
```
