# PowerShell script to update module names to match artifactIds
# This ensures consistency across all modules in the strategiz-core project

# Function to update a pom.xml file
function Update-PomXml {
    param (
        [string]$filePath
    )
    
    if (Test-Path $filePath) {
        Write-Host "Processing: $filePath"
        
        # Load the XML file
        [xml]$pomXml = Get-Content $filePath
        
        # Get the artifactId
        $artifactId = $pomXml.project.artifactId
        
        if ($artifactId) {
            # Set the name to match artifactId
            if ($pomXml.project.name) {
                $oldName = $pomXml.project.name
                $pomXml.project.name = $artifactId
                Write-Host "  Updated name from '$oldName' to '$artifactId'"
            } else {
                # Create name element if it doesn't exist
                $nameElement = $pomXml.CreateElement("name")
                $nameElement.InnerText = $artifactId
                $pomXml.project.AppendChild($nameElement) | Out-Null
                Write-Host "  Added name element with value '$artifactId'"
            }
            
            # Save the changes
            $pomXml.Save($filePath)
        } else {
            Write-Host "  No artifactId found in $filePath"
        }
    } else {
        Write-Host "File not found: $filePath"
    }
}

# Root directory
$rootDir = "c:\Users\cuzto\Documents\GitHub\strategiz-core"

# Find all pom.xml files
$pomFiles = Get-ChildItem -Path $rootDir -Filter "pom.xml" -Recurse

# Update each pom.xml file
foreach ($pom in $pomFiles) {
    Update-PomXml -filePath $pom.FullName
}

Write-Host "All module names have been updated to match their artifactIds."
