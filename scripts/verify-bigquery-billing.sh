#!/bin/bash
#
# BigQuery Billing Verification Script
# Checks if billing export data has arrived and shows sample costs
#
# Usage: ./scripts/verify-bigquery-billing.sh
#

set -e

echo "=================================================="
echo "BigQuery Billing Export Verification"
echo "=================================================="
echo ""

PROJECT_ID="strategiz-io"
DATASET="billing_export"

echo "📊 Step 1: Checking for billing export tables..."
echo ""

# List tables in billing_export dataset
TABLES=$(bq ls --max_results=10 ${PROJECT_ID}:${DATASET} 2>&1)

if echo "$TABLES" | grep -q "gcp_billing_export"; then
    echo "✅ Found billing export table(s)!"
    echo ""
    echo "$TABLES"
    echo ""

    # Get the table name
    TABLE_NAME=$(echo "$TABLES" | grep "gcp_billing_export" | awk '{print $1}' | head -1)
    echo "📋 Using table: $TABLE_NAME"
    echo ""

    echo "📊 Step 2: Checking row count..."
    echo ""

    ROW_COUNT=$(bq query --use_legacy_sql=false --format=csv \
        "SELECT COUNT(*) as row_count FROM \`${PROJECT_ID}.${DATASET}.${TABLE_NAME}\`" \
        | tail -1)

    if [ "$ROW_COUNT" -gt 0 ]; then
        echo "✅ Found $ROW_COUNT rows of billing data!"
        echo ""

        echo "📊 Step 3: Sample costs by service (last 7 days)..."
        echo ""

        bq query --use_legacy_sql=false --format=prettyjson \
            "SELECT
                service.description AS service_name,
                ROUND(SUM(cost), 2) AS total_cost_usd
            FROM \`${PROJECT_ID}.${DATASET}.${TABLE_NAME}\`
            WHERE DATE(_PARTITIONTIME) >= DATE_SUB(CURRENT_DATE(), INTERVAL 7 DAY)
            GROUP BY service_name
            ORDER BY total_cost_usd DESC
            LIMIT 10" \
            | jq -r '.[] | "\(.service_name): $\(.total_cost_usd)"' \
            || echo "Note: Install jq for formatted output (brew install jq)"

        echo ""
        echo "✅ SUCCESS! BigQuery billing data is available!"
        echo ""
        echo "Next steps:"
        echo "1. Your backend will automatically query this data"
        echo "2. Console dashboard will show real costs at:"
        echo "   - https://api.strategiz.io/v1/console/costs/summary"
        echo "   - https://api.strategiz.io/v1/console/costs/by-service"
        echo "   - https://api.strategiz.io/v1/console/costs/daily?days=7"
        echo ""

    else
        echo "⚠️  Table exists but has no data yet"
        echo ""
        echo "This is normal! BigQuery export typically takes 24-48 hours for first data."
        echo "Check again tomorrow or later today."
        echo ""
    fi

else
    echo "⏳ No billing export tables found yet"
    echo ""
    echo "Current dataset contents:"
    echo "$TABLES"
    echo ""
    echo "This is normal if BigQuery export was just enabled."
    echo "BigQuery creates the table automatically once billing data is ready."
    echo ""
    echo "Expected timeline:"
    echo "- BigQuery export enabled: Jan 3, 2026"
    echo "- First data expected: Jan 4-5, 2026 (24-48 hours)"
    echo ""
    echo "Check again in a few hours!"
    echo ""
fi

echo "=================================================="
echo "Verification Complete"
echo "=================================================="
