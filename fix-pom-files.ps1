# PowerShell script to fix all POM files with correct groupId and parent references
Write-Host "Fixing all POM files with correct groupId and parent references..."

# Function to fix a POM file
function Fix-PomFile {
    param (
        [string]$filePath
    )
    
    Write-Host "Processing $filePath"
    
    # Read the content of the POM file
    $content = Get-Content -Path $filePath -Raw
    
    # Fix groupId
    $content = $content -replace '<groupId>io\.strategiz</groupId>', '<groupId>strategiz</groupId>'
    
    # Fix <n> tags to <name> tags
    $content = $content -replace '<n>', '<name>'
    
    # Add relativePath to parent if it doesn't exist
    if ($content -match '<parent>.*?</parent>' -and $content -notmatch '<relativePath>') {
        $content = $content -replace '(<version>.*?</version>)\s*</parent>', '$1' + "`r`n        <relativePath>../pom.xml</relativePath>`r`n    </parent>"
    }
    
    # Write the fixed content back to the file
    Set-Content -Path $filePath -Value $content
}

# Get all POM files in the project
$pomFiles = Get-ChildItem -Path . -Filter "pom.xml" -Recurse

# Process each POM file
foreach ($pomFile in $pomFiles) {
    Fix-PomFile -filePath $pomFile.FullName
}

Write-Host "All POM files have been fixed!"
