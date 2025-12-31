#!/bin/bash

# Script to rename StrategyAlert to AlertDeployment
# Preserves git history using git mv

set -e  # Exit on error

echo "=========================================="
echo "Renaming StrategyAlert → AlertDeployment"
echo "=========================================="

BASE_DIR="/Users/cuztomizer/Documents/GitHub/strategiz-core"
DATA_STRATEGY="$BASE_DIR/data/data-strategy/src/main/java/io/strategiz/data/strategy"

# Step 1: Rename all Java files using git mv
echo ""
echo "Step 1: Renaming files with git mv..."
echo "--------------------------------------"

cd "$BASE_DIR"

# Find all StrategyAlert files and rename them
find "$DATA_STRATEGY" -name "*StrategyAlert*.java" -type f | while read file; do
    dir=$(dirname "$file")
    basename=$(basename "$file")
    newname=$(echo "$basename" | sed 's/StrategyAlert/AlertDeployment/g')

    if [ "$basename" != "$newname" ]; then
        echo "Renaming: $basename → $newname"
        git mv "$file" "$dir/$newname"
    fi
done

echo ""
echo "Step 2: Updating class names and imports..."
echo "--------------------------------------------"

# Update class names, imports, and references in all Java files
find "$DATA_STRATEGY" -name "*.java" -type f -exec sed -i '' \
    -e 's/class StrategyAlert /class AlertDeployment /g' \
    -e 's/StrategyAlert(/AlertDeployment(/g' \
    -e 's/extends StrategyAlert/extends AlertDeployment/g' \
    -e 's/StrategyAlertBaseRepository/AlertDeploymentBaseRepository/g' \
    -e 's/StrategyAlertSubcollectionRepository/AlertDeploymentSubcollectionRepository/g' \
    -e 's/StrategyAlertHistoryBaseRepository/AlertDeploymentHistoryBaseRepository/g' \
    -e 's/CreateStrategyAlertRepository/CreateAlertDeploymentRepository/g' \
    -e 's/ReadStrategyAlertRepository/ReadAlertDeploymentRepository/g' \
    -e 's/UpdateStrategyAlertRepository/UpdateAlertDeploymentRepository/g' \
    -e 's/DeleteStrategyAlertRepository/DeleteAlertDeploymentRepository/g' \
    -e 's/CreateStrategyAlertHistoryRepository/CreateAlertDeploymentHistoryRepository/g' \
    -e 's/ReadStrategyAlertHistoryRepository/ReadAlertDeploymentHistoryRepository/g' \
    -e 's/UpdateStrategyAlertHistoryRepository/UpdateAlertDeploymentHistoryRepository/g' \
    -e 's/DeleteStrategyAlertHistoryRepository/DeleteAlertDeploymentHistoryRepository/g' \
    -e 's/import.*StrategyAlert/import io.strategiz.data.strategy.entity.AlertDeployment/g' \
    -e 's/"strategyAlerts"/"alertDeployments"/g' \
    {} \;

echo ""
echo "Step 3: Updating imports in all affected files..."
echo "--------------------------------------------------"

# Update imports across the entire codebase
find "$BASE_DIR/data" "$BASE_DIR/business" "$BASE_DIR/service" -name "*.java" -type f -exec sed -i '' \
    -e 's/import io\.strategiz\.data\.strategy\.entity\.StrategyAlert;/import io.strategiz.data.strategy.entity.AlertDeployment;/g' \
    -e 's/import io\.strategiz\.data\.strategy\.repository\.StrategyAlert/import io.strategiz.data.strategy.repository.AlertDeployment/g' \
    -e 's/StrategyAlert alert/AlertDeployment alert/g' \
    -e 's/List<StrategyAlert>/List<AlertDeployment>/g' \
    -e 's/Optional<StrategyAlert>/Optional<AlertDeployment>/g' \
    -e 's/<StrategyAlert>/<AlertDeployment>/g' \
    {} \;

echo ""
echo "=========================================="
echo "Renaming complete!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. Review changes: git diff"
echo "2. Compile: mvn clean compile -pl data/data-strategy"
echo "3. Run tests: mvn test -pl data/data-strategy"
echo "4. Commit: git commit -m 'refactor: Rename StrategyAlert to AlertDeployment'"
