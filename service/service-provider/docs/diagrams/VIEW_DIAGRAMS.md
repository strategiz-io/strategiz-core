---
id: view-diagrams
title: How to View and Edit Diagrams
sidebar_label: View Diagrams
---

# How to View and Edit Diagrams

## 🚨 Quick Fix: Install VS Code Extension

Run this command in your terminal:
```bash
code --install-extension hediet.vscode-drawio
```

Then:
1. Restart VS Code
2. Click on any `.drawio` file
3. It will open in a visual diagram editor right in VS Code!

## Option 1: VS Code with Draw.io Extension (Recommended)

### Install the Extension
```bash
# Install the draw.io extension for VS Code
code --install-extension hediet.vscode-drawio

# Also recommended: Better markdown preview
code --install-extension shd101wyy.markdown-preview-enhanced
```

### How to Use
1. After installation, restart VS Code
2. Open any `.drawio` file
3. VS Code will show a visual diagram editor
4. You can edit, save, and export directly from VS Code

### Features
- Edit diagrams directly in VS Code
- Live preview while editing
- Export to PNG/SVG/PDF
- Integrated with your workspace

## Option 2: Draw.io Desktop App

### Install
1. Download from: https://github.com/jgraph/drawio-desktop/releases
2. Choose your platform (Mac/Windows/Linux)
3. Install the application

### How to Use
1. Open Draw.io Desktop
2. File → Open → Select the `.drawio` file
3. Edit and save
4. File → Export As → PNG for documentation

## Option 3: Web Browser

### Quick Open
1. Go to https://app.diagrams.net/
2. Click "Open Existing Diagram"
3. Select "Device" 
4. Choose the `.drawio` file from your computer

### Direct Links to Our Diagrams
Open these in your browser:
- [Edit Flow Diagram](https://app.diagrams.net/#Uhttps%3A%2F%2Fraw.githubusercontent.com%2Fyour-repo%2Fstrategiz-core%2Fmain%2Fservice%2Fservice-provider%2Fdocs%2Fdiagrams%2Fprovider-connection-api-flow.drawio)
- [Edit Component Diagram](https://app.diagrams.net/#Uhttps%3A%2F%2Fraw.githubusercontent.com%2Fyour-repo%2Fstrategiz-core%2Fmain%2Fservice%2Fservice-provider%2Fdocs%2Fdiagrams%2Fprovider-connection-api-components.drawio)

## Option 4: Generate PNG Exports

If you just want to view the diagrams as images:

### Using Draw.io Desktop/Web
1. Open the `.drawio` file
2. File → Export As → PNG
3. Settings:
   - Zoom: 100%
   - Border: 10
   - Background: White
   - Quality: High
4. Save in same folder with `.png` extension

### Using Command Line (requires draw.io desktop)
```bash
# Export all diagrams to PNG
for file in *.drawio; do
    drawio --export --format png --output "${file%.drawio}.png" "$file"
done
```

## Current Diagrams in This Folder

| Diagram | Purpose | View | Edit |
|---------|---------|------|------|
| provider-connection-api-flow.drawio | Shows API request flow | [View PNG](provider-connection-api-flow.png) | Open in VS Code |
| provider-connection-api-components.drawio | Shows component architecture | [View PNG](provider-connection-api-components.png) | Open in VS Code |

## VS Code Workspace Settings

Add this to your `.vscode/settings.json` for better diagram support:

```json
{
  "drawio.theme": "atlas",
  "drawio.exportFormat": "png",
  "drawio.exportQuality": 100,
  "files.associations": {
    "*.drawio": "xml",
    "*.drawio.svg": "xml"
  },
  "workbench.editorAssociations": {
    "*.drawio": "hediet.vscode-drawio-text"
  }
}
```

## Troubleshooting

### Can't see diagrams in VS Code?
1. Make sure extension is installed: `code --list-extensions | grep drawio`
2. Restart VS Code after installation
3. Right-click on `.drawio` file → "Open With..." → "Draw.io Editor"

### Diagrams not showing in markdown preview?
- PNG exports need to be created first
- Use the export instructions above
- Make sure PNG files are in the same folder

### Want to edit without installing anything?
- Use https://app.diagrams.net/ 
- It's free and works in any browser
- Just upload the file and edit

## Tips

1. **Always save both formats**: Keep `.drawio` for editing and `.png` for viewing
2. **Use consistent colors**: Follow our color scheme guide
3. **Version control**: Both `.drawio` and `.png` files should be committed
4. **Regular exports**: Export to PNG whenever you change a diagram