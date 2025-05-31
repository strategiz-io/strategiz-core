# Script to fix <n> tags in all pom.xml files
Get-ChildItem -Path "C:\Users\cuzto\Documents\GitHub\strategiz-core" -Recurse -Filter "pom.xml" | ForEach-Object {
    $content = Get-Content -Path $_.FullName -Raw
    if ($content -match '<n>.*?</n>') {
        $content = $content -replace '<n>(.*?)</n>', '<name>$1</name>'
        Set-Content -Path $_.FullName -Value $content
        Write-Host "Fixed: $($_.FullName)"
    }
}

# Refresh Maven projects
Write-Host "All pom.xml files fixed. Now run 'mvn eclipse:eclipse' to update project configuration."
