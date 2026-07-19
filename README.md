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

- Java 26+
- Node.js 22+
- Docker + Docker Compose (for PostgreSQL / production)
- Gradle (or use included `./gradlew`)
- AWS CLI configured (for EC2 deployment)

## Quick Start - Local Development (H2)

No database setup needed. H2 runs in-memory.

```bash
# Run backend (terminal 1)
./gradlew bootRun

# Run frontend (terminal 2)
cd frontend
npm install
npm start
```

- Frontend: http://localhost:3000
- Backend API: http://localhost:8080/api

## Run with PostgreSQL (Local)

```bash
# 1. Create a .env file in the project root (see .env.example)
# 2. Start PostgreSQL in Docker
docker compose up -d db

# 3. Start the backend with PostgreSQL profile
./gradlew bootRun --args='--spring.profiles.active=postgres'

# 4. Run frontend (separate terminal)
cd frontend
npm start
```

## Run with Docker Compose

```bash
# Build and start all services
docker compose up -d --build

# View logs
docker compose logs -f backend

# Stop all services
docker compose down

# Stop and remove volumes (resets database)
docker compose down -v
```

- Frontend + API: http://localhost:80

A `docker-compose.override.yml` is automatically loaded locally to use `nginx/default.local.conf` (plain HTTP, no SSL). On EC2, only `docker-compose.yml` is used with the production nginx config.

## Run Tests

```bash
./gradlew test
```

All tests use H2 in-memory database (no external dependencies needed).

## Deploy to EC2

Deployment is automated via GitHub Actions. Pushing to `main` triggers the CI/CD pipeline which runs tests, builds the frontend, and deploys to EC2.

### One-time AWS setup

**1. Create SSM parameters on the EC2 instance:**

```bash
aws ssm put-parameter --name "/apartment-manager/DB_USERNAME" --type String --value "apartment_user"
aws ssm put-parameter --name "/apartment-manager/DB_PASSWORD" --type SecureString --value "<your-strong-password>"
aws ssm put-parameter --name "/apartment-manager/JWT_SECRET" --type SecureString --value "<your-base64-secret>"
```

Generate a JWT secret: `openssl rand -base64 48`

**2. Attach an IAM role to the EC2 instance** with SSM read access:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "ssm:GetParameter",
      "Resource": "arn:aws:ssm:*:*:parameter/apartment-manager/*"
    }
  ]
}
```

**3. GitHub Actions secrets** (in repo Settings → Secrets):

| Secret | Description |
|--------|-------------|
| `EC2_SSH_KEY` | Private SSH key for the EC2 instance |
| `EC2_HOST` | EC2 public IP or hostname |
| `EC2_USER` | SSH username (e.g. `ubuntu`) |

### How it works

1. GitHub Actions runs backend tests + builds the frontend
2. SSHs into EC2 and runs `scripts/deploy.sh`
3. `deploy.sh` pulls the latest code, fetches secrets from SSM Parameter Store, then runs `docker compose up -d --build`
4. Docker Compose rebuilds and restarts all services

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

### Local Development (H2)

No environment variables needed. Defaults are in `application.properties`.

### Local Development (PostgreSQL)

Set these in a `.env` file in the project root (gitignored):

| Variable | Example | Description |
|----------|---------|-------------|
| `DB_USERNAME` | `apartment_user` | PostgreSQL username |
| `DB_PASSWORD` | `changeme` | PostgreSQL password |
| `JWT_SECRET` | base64 string | JWT signing secret (min 32 bytes decoded) |

### Production (EC2)

Secrets are stored in AWS SSM Parameter Store and fetched at deploy time by `deploy.sh`:

| SSM Parameter | Description |
|---------------|-------------|
| `/apartment-manager/DB_USERNAME` | PostgreSQL username |
| `/apartment-manager/DB_PASSWORD` | PostgreSQL password |
| `/apartment-manager/JWT_SECRET` | JWT signing secret |

## Project Structure

```
demo/
├── src/main/java/com/apartmentmanager/
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
│   └── Dockerfile
├── scripts/
│   └── deploy.sh       # EC2 deployment script (fetches secrets from SSM)
├── nginx/
│   └── default.conf    # Production nginx config
├── docker-compose.yml
├── Dockerfile.backend
├── .env                # Local dev secrets (gitignored)
├── .env.example        # Template for .env
└── build.gradle
```
