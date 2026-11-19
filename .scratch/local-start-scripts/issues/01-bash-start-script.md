# 01 - Replace the PowerShell start scripts with one bash script

Status: ready-for-agent

## Problem

Local start-up is spread over three overlapping scripts at the repo root:

- `startAppOnLocal.ps1`: starts the backend (H2) and the frontend, and stops both on Ctrl+C.
- `StartApplicationOnLocalWithDockerCompose.ps1`: starts Postgres in Docker, then the backend (profile `postgres`) and the frontend, and stops all of them on Ctrl+C. It also:
  - runs `docker compose down --rmi all --remove-orphans -v` on every start, which wipes the database volume and every image, so local data never survives a restart;
  - always enables Java Flight Recorder (`GRADLE_OPTS=-XX:StartFlightRecording=filename=backend-recording.jfr,...`), which leaves an untracked `backend-recording.jfr` behind;
  - waits a fixed 3 seconds for Postgres.
- `start-postgres.sh`: starts Postgres and the backend, with no frontend.

The PowerShell scripts only work on Windows, and the same start/wait/stop logic is repeated across them.

## What to do

Create **`scripts/start-local.sh`** (next to the existing deploy/backup bash scripts). It must work in Git Bash on Windows as well as on macOS and Linux. Run it from the repo root; if the script `cd`s itself, run it from anywhere.

Behaviour:

| Flag | Effect |
|---|---|
| (none) | Backend on H2 (`./gradlew :backend:bootRun`) + frontend (`npm start` in `frontend/`) |
| `--postgres` | Start the `db` service from `docker-compose.yml` first, wait until it's ready, then run the backend with `--args='--spring.profiles.active=postgres'` |
| `--fresh` | Only with `--postgres`: run `docker compose down -v --remove-orphans` first to wipe the local database. Don't remove images (`--rmi all` forced a full re-pull for no benefit) |
| `--profile` | Start the backend with Java Flight Recorder (`GRADLE_OPTS=-XX:StartFlightRecording=filename=backend-recording.jfr,disk=true,settings=default`). Off by default |
| `-h` / `--help` | Print usage |

Details:

1. **Postgres readiness:** replace the fixed sleep with a loop of `docker compose exec -T db pg_isready` (with a timeout, ~30s, and a clear error if it never becomes ready).
2. **Backend readiness:** before starting the frontend, poll the backend as the PowerShell script did (`POST /api/auth/login` with `{}`: any HTTP response, including 400, means it's up), with a ~60s timeout. If it times out, print a warning and start the frontend anyway.
3. Ensure `backend/uploads` exists (the Docker script did this).
4. Clear `JAVA_TOOL_OPTIONS` for the backend process, as the Docker script did.
5. **Ctrl+C stops everything:** `trap` on INT/TERM/EXIT kills the backend and frontend process groups (Gradle and npm start child processes, so kill the whole group or use `pkill -P` as needed; check it on Git Bash for Windows). With `--postgres`, also run `docker compose stop db`. Don't wipe data on stop.
6. Print the URLs at start (frontend :3000, backend :8080/api, plus Postgres :5432 with `--postgres`).
7. `set -euo pipefail`, and make the file executable (`git update-index --chmod=+x`), since it's committed from Windows.

Clean-up:

- Delete `startAppOnLocal.ps1`, `StartApplicationOnLocalWithDockerCompose.ps1` and `start-postgres.sh`.
- Add `*.jfr` to `.gitignore`.
- Mention `scripts/start-local.sh` and its flags in `AGENTS.md` (or `backend/AGENTS.md` if security-fixes ticket 06 has created it) under a "Running locally" note.

Don't touch `gradlew.bat` or `backend/gradlew.bat`. They're the Gradle wrapper and stay.

## How to verify

Run each mode in Git Bash on Windows:

- `scripts/start-local.sh`: the app loads at http://localhost:3000 and login works. Ctrl+C leaves no `java` or `node` process running from it.
- `scripts/start-local.sh --postgres`: create something, stop, start again, and the data is still there.
- `scripts/start-local.sh --postgres --fresh`: the data is gone.
- `scripts/start-local.sh --profile`: `backend-recording.jfr` is written and `git status` doesn't show it.
- `scripts/start-local.sh --fresh` without `--postgres`: a clear error, or ignored with a warning.
