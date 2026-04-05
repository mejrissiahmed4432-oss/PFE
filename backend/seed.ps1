$categories = @(
    @{ name = 'DEVICE'; icon = 'monitor'; types = @('Laptop', 'System Unit', 'Desktop', 'Smartphone', 'Tablet') },
    @{ name = 'PERIPHERAL'; icon = 'mouse'; types = @('Monitor', 'Keyboard', 'Mouse', 'Printer', 'Scanner', 'Headset', 'Webcam', 'HDMI Cable', 'USB Cable', 'Charger', 'Adapter', 'Docking Station', 'USB Hub') },
    @{ name = 'NETWORK'; icon = 'wifi'; types = @('Router', 'Switch', 'Access Point', 'Firewall') },
    @{ name = 'STORAGE'; icon = 'hard-drive'; types = @('SSD', 'HDD', 'NVMe', 'USB Flash', 'Ext. HDD') },
    @{ name = 'COMPONENT'; icon = 'cpu'; types = @('RAM', 'CPU', 'GPU', 'Motherboard', 'NIC') }
)

foreach ($cat in $categories) {
    $json = $cat | ConvertTo-Json -Depth 10 -Compress
    try {
        Invoke-RestMethod -Uri "http://localhost:8000/api/equipment-categories" -Method Post -ContentType "application/json" -Body $json
        Write-Host "Added $($cat.name)"
    } catch {
        Write-Host "Failed to add $($cat.name). Trying 8080..."
        Invoke-RestMethod -Uri "http://localhost:8080/api/equipment-categories" -Method Post -ContentType "application/json" -Body $json
        Write-Host "Added $($cat.name) on 8080"
    }
}
