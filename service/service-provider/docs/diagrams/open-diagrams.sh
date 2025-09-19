#!/bin/bash

# Script to open draw.io diagrams in the browser
# This will open the diagrams in the draw.io web editor

echo "Opening draw.io diagrams in your browser..."
echo "----------------------------------------"

# Base URL for draw.io web app
DRAWIO_URL="https://app.diagrams.net"

# Get the current directory
CURRENT_DIR=$(pwd)

# Function to open a diagram
open_diagram() {
    local file=$1
    local filename=$(basename "$file")
    
    echo "Opening $filename..."
    
    # Create a URL to open the file in draw.io
    # Note: For local files, you'll need to use File -> Open from Device in the web app
    open "$DRAWIO_URL"
    
    echo "  → Use 'File -> Open from -> Device' and select:"
    echo "    $file"
    echo ""
}

# Find all .drawio files in current directory
for file in *.drawio; do
    if [ -f "$file" ]; then
        open_diagram "$CURRENT_DIR/$file"
    fi
done

echo "----------------------------------------"
echo "Alternative: Install Draw.io Desktop App"
echo "Download from: https://github.com/jgraph/drawio-desktop/releases"
echo ""
echo "Or install VS Code extension:"
echo "  code --install-extension hediet.vscode-drawio"
echo "----------------------------------------"