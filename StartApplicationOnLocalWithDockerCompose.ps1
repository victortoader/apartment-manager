Write-Host "Cleaning up existing containers, images, and database volume..."
docker compose down --rmi all --remove-orphans -v 2>$null

Write-Host "Starting PostgreSQL..."
docker compose up -d db

Write-Host "Waiting for PostgreSQL to be ready..."
Start-Sleep -Seconds 3

Write-Host "Ensuring uploads directory exists..."
if (-not (Test-Path "uploads")) { New-Item -ItemType Directory -Path "uploads" | Out-Null }

Write-Host "Starting backend..."
$backend = Start-Process -NoNewWindow -PassThru -FilePath "cmd" -ArgumentList "/c .\gradlew.bat bootRun --args='--spring.profiles.active=postgres'"

Write-Host "Waiting for backend to be ready..."
$maxWait = 60
$waited = 0
while ($waited -lt $maxWait) {
    Start-Sleep -Seconds 2
    $waited += 2
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/login" -Method POST -Body '{}' -ContentType "application/json" -ErrorAction Stop
        Write-Host "Backend is ready."
        break
    } catch {
        if ($_.Exception.Response.StatusCode -eq 400) {
            Write-Host "Backend is ready."
            break
        }
    }
}

if ($waited -ge $maxWait) {
    Write-Host "Warning: Backend may not be fully ready, starting frontend anyway..."
}

Write-Host "Starting frontend..."
$frontend = Start-Process -NoNewWindow -PassThru -FilePath "cmd" -ArgumentList "/c npm start" -WorkingDirectory "frontend"

Write-Host ""
Write-Host "App running at:"
Write-Host "  Frontend: http://localhost:3000"
Write-Host "  Backend:  http://localhost:8080/api"
Write-Host "  Database: localhost:5432 (PostgreSQL)"
Write-Host ""
Write-Host "Press Ctrl+C to stop."

try {
    while ($true) { Start-Sleep -Seconds 1 }
}
finally {
    Write-Host "Stopping..."
    Stop-Process -Id $backend.Id -ErrorAction SilentlyContinue
    Stop-Process -Id $frontend.Id -ErrorAction SilentlyContinue
    Write-Host "Stopping PostgreSQL..."
    docker compose stop db
}
