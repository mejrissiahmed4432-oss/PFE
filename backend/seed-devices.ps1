$equipments = @(
    @{
        equipmentName = 'Dell Latitude 5420'
        brand = 'Dell'
        model = 'L5420'
        serialNumber = 'DELL-LT-001'
        category = 'DEVICE'
        type = 'Laptop'
        qte = 1
        status = 'Available'
        specifications = @{
            CPU = 'Intel i5-1135G7'
            RAM = '16GB'
            Storage = '512GB SSD'
        }
    },
    @{
        equipmentName = 'HP EliteBook 840'
        brand = 'HP'
        model = 'EB840G8'
        serialNumber = 'HP-LT-002'
        category = 'DEVICE'
        type = 'Laptop'
        qte = 1
        status = 'Available'
        specifications = @{
            CPU = 'Intel i7-1165G7'
            RAM = '32GB'
            Storage = '1TB NVMe'
        }
    },
    @{
        equipmentName = 'Workstation Tower'
        brand = 'Custom'
        model = 'T7920'
        serialNumber = 'WS-TW-003'
        category = 'DEVICE'
        type = 'System Unit'
        qte = 1
        status = 'Available'
        specifications = @{
            CPU = 'AMD Ryzen 9'
            RAM = '64GB'
            Storage = '2TB SSD'
        }
    }
)

foreach ($eq in $equipments) {
    $json = $eq | ConvertTo-Json -Depth 10 -Compress
    try {
        Invoke-RestMethod -Uri "http://localhost:8000/api/equipment" -Method Post -ContentType "application/json" -Body $json
        Write-Host "Added $($eq.equipmentName)"
    } catch {
        Write-Host "Failed to add $($eq.equipmentName)"
    }
}
