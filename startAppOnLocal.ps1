Write-Host "Starting backend..."
$backend = Start-Process -NoNewWindow -PassThru -FilePath "cmd" -ArgumentList "/c .\gradlew.bat bootRun"

Write-Host "Starting frontend..."
$frontend = Start-Process -NoNewWindow -PassThru -FilePath "cmd" -ArgumentList "/c npm start" -WorkingDirectory "frontend"

Write-Host ""
Write-Host "App running at:"
Write-Host "  Frontend: http://localhost:3000"
Write-Host "  Backend:  http://localhost:8080/api"
Write-Host ""
Write-Host "Press Ctrl+C to stop."

try {
    while ($true) { Start-Sleep -Seconds 1 }
}
finally {
    Write-Host "Stopping..."
    Stop-Process -Id $backend.Id -ErrorAction SilentlyContinue
    Stop-Process -Id $frontend.Id -ErrorAction SilentlyContinue
}
