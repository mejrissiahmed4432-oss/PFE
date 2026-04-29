$osData = @(
    @{
        name = "Windows 10"
        version = "22H2"
        edition = "Professional"
        architecture = "x64"
        licenseType = "Volume"
        licenseKey = "XXXXX-XXXXX-XXXXX-XXXXX-XXXXX"
        totalLicenses = 50
        usedLicenses = 12
        size = "5.4 GB"
        requiredRam = 4
        requiredStorage = 64
        status = "Active"
    },
    @{
        name = "Windows 11"
        version = "23H2"
        edition = "Enterprise"
        architecture = "x64"
        licenseType = "Volume"
        licenseKey = "YYYYY-YYYYY-YYYYY-YYYYY-YYYYY"
        totalLicenses = 30
        usedLicenses = 5
        size = "6.2 GB"
        requiredRam = 8
        requiredStorage = 64
        status = "Active"
    },
    @{
        name = "Ubuntu Desktop"
        version = "24.04 LTS"
        edition = "Noble Numbat"
        architecture = "x64"
        licenseType = "Free / Open Source"
        totalLicenses = 999
        usedLicenses = 25
        size = "4.8 GB"
        requiredRam = 4
        requiredStorage = 25
        status = "Active"
    },
    @{
        name = "Windows Server 2022"
        version = "21H2"
        edition = "Standard"
        architecture = "x64"
        licenseType = "Retail"
        licenseKey = "ZZZZZ-ZZZZZ-ZZZZZ-ZZZZZ-ZZZZZ"
        totalLicenses = 10
        usedLicenses = 2
        size = "5.2 GB"
        requiredRam = 16
        requiredStorage = 128
        status = "Active"
    }
)

foreach ($os in $osData) {
    $json = $os | ConvertTo-Json -Compress
    try {
        Invoke-RestMethod -Uri "http://localhost:8081/api/os" -Method Post -ContentType "application/json" -Body $json
        Write-Host "Added $($os.name)" -ForegroundColor Green
    } catch {
        Write-Host "Failed to add $($os.name): $($_.Exception.Message)" -ForegroundColor Red
    }
}
