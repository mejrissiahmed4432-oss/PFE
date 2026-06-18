# Charger les variables du .env dans l'environnement
Get-Content .env | Where-Object { $_ -notmatch "^#" -and $_ -ne "" } | ForEach-Object {
    $parts = $_ -split "=", 2
    [System.Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1].Trim(), "Process")
}

Write-Host "✅ Variables .env chargées" -ForegroundColor Green

# 2. Démarrer Config Server
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd backend/config-server; mvn spring-boot:run"
Write-Host "🚀 Config Server démarré (port 8888)" -ForegroundColor Cyan
Start-Sleep -Seconds 15


# 1. Démarrer Eureka
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd backend/eureka-server; mvn spring-boot:run"
Write-Host "🚀 Eureka Server démarré (port 8761)" -ForegroundColor Cyan
Start-Sleep -Seconds 15



# 3. Démarrer les microservices
$services = @("usermicroservice","employee-microservice","it-manager-microservice","stock-manager-microservice","technician-microservice","ai-service","api-gateway")
foreach ($svc in $services) {
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd backend/$svc; mvn spring-boot:run"
    Write-Host "🚀 $svc démarré" -ForegroundColor Yellow
    Start-Sleep -Seconds 4
}

Write-Host "✅ Tous les services sont en cours de démarrage!" -ForegroundColor Green
