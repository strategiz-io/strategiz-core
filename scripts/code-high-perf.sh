#!/bin/bash
# VS Code High Performance Launcher
# Optimized for 48GB RAM / 14 CPU cores
# Created for strategiz-core development

# ============================================
# MEMORY SETTINGS
# ============================================

# Node.js heap size for VS Code main process (8GB)
export NODE_OPTIONS="--max-old-space-size=8192"

# Extension Host memory (8GB)
export VSCODE_EXTHOST_MEMORY=8192

# ============================================
# JAVA EXTENSION SETTINGS
# ============================================

# Java Language Server heap (16GB) - great for large Maven projects
export JAVA_TOOL_OPTIONS="-Xmx16G -Xms4G -XX:+UseG1GC -XX:+UseStringDeduplication"

# ============================================
# PERFORMANCE OPTIMIZATIONS
# ============================================

# Increase file watcher limit
ulimit -n 65536 2>/dev/null

# ============================================
# LAUNCH VS CODE
# ============================================

echo "Launching VS Code with High Performance settings..."
echo "  - Node.js heap: 8GB"
echo "  - Extension Host: 8GB"
echo "  - Java Language Server: 16GB (max)"
echo "  - VS Code max memory: 20GB"
echo "  - File watchers: 65536"
echo ""

# Launch VS Code with the directory argument (or current dir)
# Find VS Code in common locations
if [ -f "/Applications/Visual Studio Code.app/Contents/Resources/app/bin/code" ]; then
    CODE_BIN="/Applications/Visual Studio Code.app/Contents/Resources/app/bin/code"
elif [ -f "$HOME/Downloads/Visual Studio Code.app/Contents/Resources/app/bin/code" ]; then
    CODE_BIN="$HOME/Downloads/Visual Studio Code.app/Contents/Resources/app/bin/code"
elif [ -f "$HOME/Applications/Visual Studio Code.app/Contents/Resources/app/bin/code" ]; then
    CODE_BIN="$HOME/Applications/Visual Studio Code.app/Contents/Resources/app/bin/code"
else
    echo "Error: VS Code not found. Please install it or update this script."
    exit 1
fi

if [ -n "$1" ]; then
    "$CODE_BIN" --max-memory=20480 "$@"
else
    "$CODE_BIN" --max-memory=20480 .
fi
