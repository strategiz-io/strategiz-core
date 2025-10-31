# Strategiz Core Diagrams

This directory contains editable Draw.io diagrams for Strategiz Core architecture and flows.

## 📁 Directory Structure

```
docs/diagrams/
├── architecture-overview.drawio.png
├── layered-architecture.drawio.png
├── provider-connection-flow.drawio.png
├── portfolio-aggregation-flow.drawio.png
├── authentication-flow.drawio.png
├── oauth-pattern.drawio.png
├── module-dependencies.drawio.png
└── deployment-architecture.drawio.png
```

## 🎨 Creating/Editing Diagrams

### Using Draw.io Desktop (Recommended)

1. **Download**: https://www.drawio.com/
2. **Open diagram**: File → Open → Select `.drawio.png` file
3. **Edit**: Make your changes
4. **Export**: File → Export as → PNG
   - ✅ Check "Include a copy of my diagram"
   - ✅ Check "Transparent Background" (optional)
   - Save as `.drawio.png`

### Brand Colors

- Primary: `#39FF14` (Neon Green)
- Secondary: `#00BFFF` (Neon Blue)
- Accent: `#BF00FF` (Neon Purple)
- Background: `#1a1a1a` (Dark)
- Text: `#FFFFFF` (White)

## 📝 Referencing in MDX

Diagrams are copied to the docs site during build. Reference them as:

```mdx
![Architecture Overview](diagrams/architecture-overview.drawio.png)
```

## 🔄 Workflow

1. **Create/Edit** diagrams in this directory
2. **Copy** to docs site: `cp *.drawio.png ../../../strategiz-docs/static/diagrams/strategiz-core/`
3. **Reference** in MDX files
4. **Commit** both source diagram and MDX changes

---

**Last Updated**: 2025-10-26
