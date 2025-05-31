$pomFiles = Get-ChildItem -Path . -Filter "pom.xml" -Recurse

foreach ($file in $pomFiles) {
    $content = Get-Content -Path $file.FullName -Raw
    
    # Remove <name>...</name> tags
    $newContent = $content -replace '<name>[^<]*</name>', ''
    
    # Remove <n>...</n> tags (the malformed ones)
    $newContent = $newContent -replace '<n>[^<]*</n>', ''
    
    # Write the updated content back
    Set-Content -Path $file.FullName -Value $newContent -NoNewline
    
    Write-Host "Processed $($file.FullName)"
}

Write-Host "Completed removing name tags from all pom.xml files"
