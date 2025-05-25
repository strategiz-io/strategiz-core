# PowerShell script to fix all POM files with correct groupId and parent references
Write-Host "Fixing all POM files with correct groupId and parent references..."

# Function to check if a POM file needs fixing
function Needs-Fixing {
    param (
        [string]$filePath
    )
    
    $content = Get-Content -Path $filePath -Raw
    return ($content -match 'io\.strategiz' -or $content -match '<n>' -or ($content -match '<parent>' -and $content -match '\n' -and $content -notmatch '<relativePath>'))
}

# Function to fix a POM file
function Fix-PomFile {
    param (
        [string]$filePath
    )
    
    Write-Host "Fixing $filePath"
    
    # Read the content of the POM file
    $content = Get-Content -Path $filePath -Raw
    
    # Create a temporary file
    $tempFile = "$filePath.temp"
    
    # Fix groupId
    $content = $content -replace 'io\.strategiz', 'strategiz'
    
    # Fix <n> tags to <name> tags
    $content = $content -replace '<n>', '<name>'
    
    # Fix the parent section to ensure proper formatting
    if ($content -match '<parent>' -and $content -notmatch '<relativePath>') {
        $content = $content -replace '(<version>[^<]+</version>)\s*</parent>', '$1' + [Environment]::NewLine + '        <relativePath>../pom.xml</relativePath>' + [Environment]::NewLine + '    </parent>'
    }
    
    # Remove any literal \n characters that might have been introduced
    $content = $content -replace '\\n', [Environment]::NewLine
    
    # Write the fixed content to the temporary file
    Set-Content -Path $tempFile -Value $content
    
    # Replace the original file with the fixed one
    Move-Item -Path $tempFile -Destination $filePath -Force
}

# Get all POM files in the project
$pomFiles = Get-ChildItem -Path . -Filter "pom.xml" -Recurse

# Process each POM file
foreach ($pomFile in $pomFiles) {
    if (Needs-Fixing -filePath $pomFile.FullName) {
        Fix-PomFile -filePath $pomFile.FullName
    }
}

Write-Host "All POM files have been fixed!"
