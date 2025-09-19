#!/bin/bash

# Script to update all entities to use @Collection annotation instead of getCollectionName()

echo "Updating all entities to use @Collection annotation..."

# Find all Java files that have getCollectionName() method
FILES=$(grep -r "getCollectionName()" --include="*.java" data/ | grep -v BaseRepository | grep -v BaseEntity | cut -d: -f1 | sort -u)

for FILE in $FILES; do
    echo "Processing: $FILE"
    
    # Extract the collection name from the getCollectionName() method
    COLLECTION_NAME=$(grep -A 1 "getCollectionName()" "$FILE" | grep "return" | sed -E 's/.*return "([^"]+)".*/\1/')
    
    if [ -z "$COLLECTION_NAME" ]; then
        echo "  Skipping - couldn't find collection name"
        continue
    fi
    
    echo "  Collection name: $COLLECTION_NAME"
    
    # Get the class name
    CLASS_NAME=$(grep "^public class" "$FILE" | sed -E 's/public class ([^ ]+).*/\1/')
    
    # Add the import for @Collection if not present
    if ! grep -q "import io.strategiz.data.base.annotation.Collection;" "$FILE"; then
        # Add import after the last import statement
        sed -i '' '/^import.*BaseEntity;/a\
import io.strategiz.data.base.annotation.Collection;' "$FILE"
    fi
    
    # Add @Collection annotation before the class declaration
    if ! grep -q "@Collection" "$FILE"; then
        sed -i '' "/^public class $CLASS_NAME/i\\
@Collection(\"$COLLECTION_NAME\")" "$FILE"
    fi
    
    # Remove the getCollectionName() method (including @Override if present)
    # This removes from @Override (if exists) or method declaration to closing brace
    sed -i '' '/@Override/{N;/getCollectionName/d;}' "$FILE"
    sed -i '' '/public String getCollectionName/,/^    \}/d' "$FILE"
    
    echo "  Updated!"
done

echo "Done! All entities updated."