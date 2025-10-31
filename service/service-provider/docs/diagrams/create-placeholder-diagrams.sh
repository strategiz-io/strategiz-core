#!/bin/bash

# Script to create placeholder diagram PNGs
# These are temporary placeholders until real draw.io exports are created

DIAGRAMS_DIR="/Users/cuztomizer/Documents/GitHub/strategiz-core/service/service-provider/docs/diagrams"

# Function to create a placeholder SVG
create_placeholder_svg() {
    local filename=$1
    local title=$2
    local width=${3:-800}
    local height=${4:-600}

    cat > "${filename}" << EOF
<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}">
  <defs>
    <style>
      .title { font-family: Arial, sans-serif; font-size: 24px; font-weight: bold; fill: #333; }
      .subtitle { font-family: Arial, sans-serif; font-size: 16px; fill: #666; }
      .box { fill: #e3f2fd; stroke: #1976d2; stroke-width: 2; }
      .label { font-family: Arial, sans-serif; font-size: 14px; fill: #333; }
    </style>
  </defs>

  <!-- Background -->
  <rect width="${width}" height="${height}" fill="#fafafa"/>

  <!-- Border -->
  <rect x="10" y="10" width="$((width-20))" height="$((height-20))" fill="none" stroke="#ddd" stroke-width="2"/>

  <!-- Title -->
  <text x="${width}/2" y="50" text-anchor="middle" class="title">${title}</text>
  <text x="${width}/2" y="80" text-anchor="middle" class="subtitle">Open this file in draw.io to edit</text>

  <!-- Placeholder content -->
  <rect x="100" y="150" width="200" height="100" class="box" rx="5"/>
  <text x="200" y="205" text-anchor="middle" class="label">Component 1</text>

  <rect x="400" y="150" width="200" height="100" class="box" rx="5"/>
  <text x="500" y="205" text-anchor="middle" class="label">Component 2</text>

  <!-- Arrow -->
  <line x1="300" y1="200" x2="400" y2="200" stroke="#1976d2" stroke-width="2" marker-end="url(#arrowhead)"/>
  <defs>
    <marker id="arrowhead" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto">
      <polygon points="0 0, 10 3.5, 0 7" fill="#1976d2" />
    </marker>
  </defs>

  <!-- Footer note -->
  <text x="${width}/2" y="$((height-30))" text-anchor="middle" class="subtitle">
    This is a placeholder. Export from draw.io as PNG with embedded data to see the actual diagram.
  </text>
</svg>
EOF
}

# Create placeholder PNGs using ImageMagick or base64 encoding
# If ImageMagick is not available, we'll use a different approach

cd "$DIAGRAMS_DIR"

echo "Creating placeholder diagrams..."

# Provider Connection API
create_placeholder_svg "provider-connection-api-components.svg" "Provider Connection - Component Diagram" 1200 800
create_placeholder_svg "provider-connection-api-flow.svg" "Provider Connection - Flow Diagram" 1200 600
create_placeholder_svg "provider-connection-api-sequence-apikey.svg" "Provider Connection - API Key Flow" 1000 800
create_placeholder_svg "provider-connection-api-sequence-oauth.svg" "Provider Connection - OAuth Flow" 1000 800
create_placeholder_svg "provider-connection-api-sequence-error.svg" "Provider Connection - Error Handling" 1000 800

# Provider Query API
create_placeholder_svg "provider-query-api-components.svg" "Provider Query - Component Diagram" 1200 800
create_placeholder_svg "provider-query-api-flow.svg" "Provider Query - Flow Diagram" 1200 600

# Provider Callback API
create_placeholder_svg "provider-callback-api-components.svg" "Provider Callback - Component Diagram" 1200 800
create_placeholder_svg "provider-callback-api-flow.svg" "Provider Callback - Flow Diagram" 1200 600

# Provider Update API
create_placeholder_svg "provider-update-api-components.svg" "Provider Update - Component Diagram" 1200 800
create_placeholder_svg "provider-update-api-flow.svg" "Provider Update - Flow Diagram" 1200 600

# Provider Delete API
create_placeholder_svg "provider-delete-api-components.svg" "Provider Delete - Component Diagram" 1200 800
create_placeholder_svg "provider-delete-api-flow.svg" "Provider Delete - Flow Diagram" 1200 600

echo "SVG placeholders created."

# Check if we have conversion tools available
if command -v rsvg-convert &> /dev/null; then
    echo "Converting SVGs to PNG using rsvg-convert..."
    for svg in *.svg; do
        png="${svg%.svg}.png"
        rsvg-convert -w 1200 "$svg" > "$png"
        echo "  Created $png"
    done
    rm *.svg
elif command -v convert &> /dev/null; then
    echo "Converting SVGs to PNG using ImageMagick..."
    for svg in *.svg; do
        png="${svg%.svg}.png"
        convert -background white -density 150 "$svg" "$png"
        echo "  Created $png"
    done
    rm *.svg
else
    echo "No SVG conversion tool found (rsvg-convert or ImageMagick)."
    echo "Renaming SVG files to PNG for now (Docusaurus can render SVGs)..."
    for svg in *.svg; do
        png="${svg%.svg}.png"
        mv "$svg" "$png"
        echo "  Renamed to $png"
    done
fi

echo "Done! Placeholder diagrams created in $DIAGRAMS_DIR"
echo ""
echo "To create proper diagrams:"
echo "1. Open the corresponding .drawio files in draw.io"
echo "2. Export as PNG with 'Include a copy of my diagram' enabled"
echo "3. Replace these placeholder PNGs with the exported versions"
