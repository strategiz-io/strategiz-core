#!/bin/bash

# Phase 1.2: Rename StrategyBot to BotDeployment
# This script renames all StrategyBot files to BotDeployment using git mv to preserve history

set -e

DATA_STRATEGY="/Users/cuztomizer/Documents/GitHub/strategiz-core/data/data-strategy/src/main/java"

echo "====== Phase 1.2: Rename StrategyBot to BotDeployment ======"
echo ""

# Step 1: Rename all Java files using git mv
echo "Step 1: Renaming Java files..."
find "$DATA_STRATEGY" -name "*StrategyBot*.java" -type f | while read file; do
    dir=$(dirname "$file")
    basename=$(basename "$file")
    newname=$(echo "$basename" | sed 's/StrategyBot/BotDeployment/g')

    if [ "$basename" != "$newname" ]; then
        echo "Renaming: $basename → $newname"
        git mv "$file" "$dir/$newname"
    fi
done

echo ""
echo "Step 2: Updating class names, imports, and type references in renamed files..."

# Step 2: Update class names and imports in the renamed files
find "$DATA_STRATEGY" -name "*BotDeployment*.java" -type f -exec sed -i '' \
    -e 's/public class StrategyBot/public class BotDeployment/g' \
    -e 's/public interface StrategyBot/public interface BotDeployment/g' \
    -e 's/extends StrategyBot/extends BotDeployment/g' \
    -e 's/implements StrategyBot/implements BotDeployment/g' \
    -e 's/import.*StrategyBot/import io.strategiz.data.strategy.entity.BotDeployment/g' \
    -e 's/StrategyBot\.class/BotDeployment.class/g' \
    -e 's/StrategyBot /BotDeployment /g' \
    -e 's/StrategyBot>/BotDeployment>/g' \
    -e 's/<StrategyBot>/<BotDeployment>/g' \
    -e 's/(StrategyBot /(BotDeployment /g' \
    -e 's/, StrategyBot /, BotDeployment /g' \
    {} \;

echo ""
echo "Step 3: Updating collection annotation..."
# Update @Collection annotation from "strategyBots" to "botDeployments"
find "$DATA_STRATEGY" -name "BotDeployment.java" -type f -exec sed -i '' \
    -e 's/@Collection("strategyBots")/@Collection("botDeployments")/g' \
    {} \;

echo ""
echo "Step 4: Updating subcollection name in repository..."
# Update getSubcollectionName() return value
find "$DATA_STRATEGY" -name "BotDeploymentSubcollectionRepository.java" -type f -exec sed -i '' \
    -e 's/return "strategyBots"/return "botDeployments"/g' \
    {} \;

echo ""
echo "Step 5: Updating constructor names..."
# Update constructor names
find "$DATA_STRATEGY" -name "*BotDeployment*.java" -type f -exec sed -i '' \
    -e 's/public StrategyBot(/public BotDeployment(/g' \
    {} \;

echo ""
echo "====== Phase 1.2 Complete ======"
echo "Files renamed: 11"
echo "Next step: Fix any remaining references and compile"
