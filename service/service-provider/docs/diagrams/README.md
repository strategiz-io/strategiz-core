---
id: diagrams
title: API Documentation Diagrams
sidebar_label: Diagrams
---

# API Documentation Diagrams

This folder contains draw.io diagram files for API documentation. Each API should have:
- Flow diagram showing the process flow
- Component diagram showing the architecture

## File Naming Convention

```
[api-name-slug]-flow.drawio       # Flow diagram
[api-name-slug]-components.drawio  # Component diagram
[api-name-slug]-flow.png           # Exported PNG of flow diagram
[api-name-slug]-components.png     # Exported PNG of component diagram
```

## How to Edit Diagrams

### Using draw.io Desktop App
1. Download draw.io desktop from https://github.com/jgraph/drawio-desktop/releases
2. Open the `.drawio` file
3. Make your changes
4. Save the file
5. Export as PNG (File > Export As > PNG)
   - Use same filename but with `.png` extension
   - Border: 10
   - Transparent background: No
   - Resolution: 300 DPI

### Using draw.io Web
1. Go to https://app.diagrams.net/
2. Open existing diagram (File > Open From > Device)
3. Select the `.drawio` file
4. Make your changes
5. Save (File > Save As)
6. Export as PNG (File > Export As > PNG)

## Exporting Diagrams to PNG

When you modify a diagram, always export a PNG version:

```bash
# Manual export process:
# 1. Open diagram in draw.io
# 2. File > Export As > PNG
# 3. Settings:
#    - Zoom: 100%
#    - Border: 10
#    - Background: White
#    - Resolution: 300 DPI
# 4. Save with same name but .png extension
```

## Current Diagrams

| API | Flow Diagram | Component Diagram |
|-----|--------------|-------------------|
| Provider Connection API | `provider-connection-api-flow.drawio` | `provider-connection-api-components.drawio` |

## Integration with MDX Documentation

The MDX documentation files reference these diagrams in two ways:

1. **External PNG Reference** (for display):
   ```markdown
   ![Provider Connection Flow](diagrams/provider-connection-api-flow.png)
   ```

2. **Link to Editable File**:
   ```markdown
   > **[📊 Open in draw.io](diagrams/provider-connection-api-flow.drawio)**
   ```

3. **Embedded Code** (in collapsible section):

       <details>
       <summary>View Embedded Diagram Code</summary>

       ```drawio
       <!-- diagram XML here -->
       ```

       </details>

## Tips for Creating Good Diagrams

### Flow Diagrams
- Use swim lanes for different layers/actors
- Show decision points clearly with diamonds
- Use consistent colors for similar components
- Include start and end points
- Label all arrows with actions

### Component Diagrams
- Group components by layer (Frontend, API, Service, Business, Data, External)
- Use different shapes for different types:
  - Rectangles: Services/Components
  - Cylinders: Databases/Storage
  - Clouds: External systems
  - Diamonds: Decision points
- Show data flow with arrows
- Use consistent color coding per layer

### Color Scheme
We use a consistent color scheme across all diagrams:
- **Frontend**: Light Blue (#e6f2ff)
- **API Gateway**: Light Yellow (#fff2cc)
- **Service Layer**: Light Red (#f8cecc)
- **Business Layer**: Light Purple (#e1d5e7)
- **Data Layer**: Light Green (#d5e8d4)
- **External Systems**: Light Gray (#f0f0f0)

## Automation Note

Future enhancement: We could automate PNG export using draw.io CLI:
```bash
# Example command (requires draw.io desktop app)
drawio --export --format png --output diagram.png diagram.drawio
```

For now, manual export is required when diagrams are updated.