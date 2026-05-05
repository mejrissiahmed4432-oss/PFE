$path = 'c:\Users\Asus\Desktop\Nouveau dossier\PFE\frontend\src\app\os-management\os-management.component.html'
$content = Get-Content $path -Raw
$content = $content -replace '</div>>', '</div>'
Set-Content -Path $path -Value $content -NoNewline
Write-Host "Fixed stray angle bracket."
