#!/bin/bash

# Script to fix remaining collection name returns in entities

echo "Fixing remaining entity collection names..."

# Fix WatchlistItemEntity
FILE="data/data-watchlist/src/main/java/io/strategiz/data/watchlist/entity/WatchlistItemEntity.java"
if [ -f "$FILE" ]; then
    echo "Fixing $FILE"
    # Remove the orphaned return statement
    sed -i '' '/^[[:space:]]*return "watchlist";$/d' "$FILE"
    sed -i '' '/^[[:space:]]*}[[:space:]]*$/N;s/}\n}/}/g' "$FILE"
fi

# Fix Strategy
FILE="data/data-strategy/src/main/java/io/strategiz/data/strategy/entity/Strategy.java"
if [ -f "$FILE" ]; then
    echo "Fixing $FILE"
    # Remove the orphaned return statement
    sed -i '' '/^[[:space:]]*return "strategies";$/d' "$FILE"
fi

# Fix UserPreferenceEntity
FILE="data/data-preferences/src/main/java/io/strategiz/data/preferences/entity/UserPreferenceEntity.java"
if [ -f "$FILE" ]; then
    echo "Fixing $FILE"
    # Remove the orphaned return statement
    sed -i '' '/^[[:space:]]*return "preferences";$/d' "$FILE"
    sed -i '' '/^[[:space:]]*}[[:space:]]*$/N;s/}\n}/}/g' "$FILE"
fi

# Also check and fix formatting for @Collection annotations
for file in data/*/src/main/java/io/strategiz/data/*/entity/*.java data/*/src/main/java/io/strategiz/data/*/model/*.java; do
    if [ -f "$file" ] && grep -q "@Collection(" "$file"; then
        # Fix spacing after @Collection annotation if needed
        sed -i '' 's/@Collection(\(.*\))public class/@Collection(\1)\
public class/g' "$file"
    fi
done

echo "Done fixing entity collection names!"