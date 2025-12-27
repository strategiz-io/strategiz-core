# Marketplace Integration Testing Guide

## Overview
This guide will help you test the complete marketplace integration including:
- Marketplace API endpoints
- Frontend marketplace UI
- Strategy publishing flow
- Purchase/checkout flow

## Prerequisites
- Access to https://strategiz.io (production frontend)
- User account with authentication
- At least one strategy created in Strategy Labs

---

## Test 1: Marketplace API Endpoints

### 1.1 List Public Strategies (Unauthenticated)
```bash
# This endpoint should work without authentication for public strategies
curl -s "https://strategiz-api-43628135674.us-east1.run.app/v1/marketplace/strategies?limit=10" | jq .
```

**Expected Response:**
```json
{
  "strategies": []
}
```
or if strategies exist:
```json
{
  "strategies": [
    {
      "id": "...",
      "name": "RSI Momentum Strategy",
      "description": "...",
      "creatorName": "John Doe",
      "pricingModel": "FREE",
      "deploymentCount": 5,
      "performance": {
        "winRate": 65.5,
        "totalReturn": 23.4,
        "lastUpdated": "2025-01-15T10:00:00Z"
      }
    }
  ]
}
```

### 1.2 Test Filtering and Sorting
```bash
# Filter by category
curl -s "https://strategiz-api-43628135674.us-east1.run.app/v1/marketplace/strategies?category=momentum&limit=5" | jq .

# Filter by featured
curl -s "https://strategiz-api-43628135674.us-east1.run.app/v1/marketplace/strategies?featured=true&limit=5" | jq .

# Sort by popular
curl -s "https://strategiz-api-43628135674.us-east1.run.app/v1/marketplace/strategies?sortBy=popular&limit=5" | jq .

# Sort by newest
curl -s "https://strategiz-api-43628135674.us-east1.run.app/v1/marketplace/strategies?sortBy=newest&limit=5" | jq .
```

### 1.3 Get Specific Strategy (Requires Authentication)
You'll need to get an auth token first from the frontend (check browser DevTools > Application > Local Storage > auth token).

```bash
# Replace YOUR_AUTH_TOKEN and STRATEGY_ID
curl -s -H "Authorization: Bearer YOUR_AUTH_TOKEN" \
  "https://strategiz-api-43628135674.us-east1.run.app/v1/marketplace/strategies/STRATEGY_ID" | jq .
```

**Expected Response:**
```json
{
  "strategy": {
    "id": "...",
    "name": "Strategy Name",
    "description": "...",
    "code": "...",
    "creatorId": "...",
    "creatorName": "...",
    "pricingModel": "ONE_TIME",
    "price": 99.00,
    "currency": "USD"
  }
}
```

---

## Test 2: Frontend Marketplace UI

### 2.1 View Marketplace
1. Navigate to https://strategiz.io
2. Log in with your account
3. Click on "Marketplace" in the navigation menu

**What to Check:**
- ✅ Marketplace screen loads without errors
- ✅ Strategies are displayed in a grid or list
- ✅ Each strategy card shows:
  - Strategy name
  - Creator name
  - Pricing badge (color-coded: FREE=green, ONE_TIME=blue, SUBSCRIPTION=purple)
  - Performance metrics (if available)
  - Deployment count ("X deployed")
  - "Performance updated X days ago" timestamp

### 2.2 Test Pricing Badges
**Expected Badge Colors:**
- **FREE**: Green badge
- **ONE_TIME** (e.g., $99): Blue badge
- **SUBSCRIPTION** (e.g., $19/mo): Purple badge

### 2.3 Test Performance Display
Look for strategies with performance data and verify:
- Win rate, total return, profit factor are displayed
- "Last updated: X days ago" shows correctly
- Hover over deployment count shows tooltip

### 2.4 Test Search and Filters
- Search for strategy by name
- Filter by category (if available)
- Sort by: Popular, Newest, Price, Rating

---

## Test 3: Strategy Publishing Flow

To test the complete publishing flow, you'll need to create and publish a strategy:

### 3.1 Create a Test Strategy
1. Go to **Strategy Labs**
2. Create a new strategy with this simple code:
```python
def initialize(context):
    context.symbol = 'AAPL'

def handle_data(context, data):
    current_price = data.current(context.symbol, 'price')

    if current_price > 150:
        order_target_percent(context.symbol, 1.0)
    else:
        order_target_percent(context.symbol, 0.0)
```
3. Name it "Test Marketplace Strategy"
4. **Save** the strategy

### 3.2 Run Backtest
1. Click **Backtest** button
2. Configure:
   - Start date: 6 months ago
   - End date: Today
   - Initial capital: $10,000
3. Run the backtest
4. Wait for results to complete

**This step is CRITICAL** - You must have backtest results before publishing!

### 3.3 Publish Strategy
Currently, the UI for publishing from "My Strategies" isn't implemented yet (it's in the plan). For now, you can publish via API:

```bash
# Get your auth token from browser DevTools
# Replace YOUR_AUTH_TOKEN and STRATEGY_ID

curl -X POST \
  -H "Authorization: Bearer YOUR_AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "isPublished": true,
    "pricing": {
      "pricingType": "FREE",
      "currency": "USD"
    },
    "category": "momentum",
    "description": "A simple AAPL momentum strategy for testing"
  }' \
  "https://strategiz-api-43628135674.us-east1.run.app/v1/strategies/STRATEGY_ID"
```

For paid strategies:
```bash
# ONE_TIME pricing
curl -X POST \
  -H "Authorization: Bearer YOUR_AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "isPublished": true,
    "pricing": {
      "pricingType": "ONE_TIME",
      "oneTimePrice": 99.00,
      "currency": "USD"
    },
    "category": "momentum",
    "description": "Premium AAPL momentum strategy"
  }' \
  "https://strategiz-api-43628135674.us-east1.run.app/v1/strategies/STRATEGY_ID"

# SUBSCRIPTION pricing
curl -X POST \
  -H "Authorization: Bearer YOUR_AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "isPublished": true,
    "pricing": {
      "pricingType": "SUBSCRIPTION",
      "monthlyPrice": 19.00,
      "currency": "USD"
    },
    "category": "momentum",
    "description": "Monthly AAPL momentum strategy subscription"
  }' \
  "https://strategiz-api-43628135674.us-east1.run.app/v1/strategies/STRATEGY_ID"
```

### 3.4 Verify Published Strategy
1. Refresh the Marketplace page
2. Your strategy should now appear
3. Check that all fields are displayed correctly:
   - Name, description
   - Pricing badge matches what you set
   - Performance metrics from backtest are shown
   - Deployment count shows 0

---

## Test 4: Purchase Flow

### 4.1 Purchase FREE Strategy
1. In the Marketplace, find a FREE strategy
2. Click on the strategy card to view details
3. Click "Add to My Strategies" or "Purchase" button
4. Strategy should be immediately added to your account
5. Verify in "My Strategies" that it appears with "Purchased" badge

**API Test:**
```bash
curl -X POST \
  -H "Authorization: Bearer YOUR_AUTH_TOKEN" \
  "https://strategiz-api-43628135674.us-east1.run.app/v1/marketplace/strategies/STRATEGY_ID/purchase"
```

**Expected Response:**
```json
{
  "success": true,
  "purchaseId": "...",
  "strategyId": "...",
  "message": "Strategy added successfully"
}
```

### 4.2 Purchase ONE_TIME Strategy (Stripe Checkout)
1. Find a ONE_TIME priced strategy
2. Click "Purchase for $X" button
3. Should redirect to Stripe checkout page
4. Use Stripe test card: `4242 4242 4242 4242`
   - Expiry: Any future date
   - CVC: Any 3 digits
   - ZIP: Any 5 digits
5. Complete payment
6. Should redirect back to: `/marketplace/purchase-success?session_id=...`
7. Verify strategy appears in "My Strategies"

**API Test (Create Checkout):**
```bash
curl -X POST \
  -H "Authorization: Bearer YOUR_AUTH_TOKEN" \
  "https://strategiz-api-43628135674.us-east1.run.app/v1/marketplace/strategies/STRATEGY_ID/checkout"
```

**Expected Response:**
```json
{
  "sessionId": "cs_test_...",
  "checkoutUrl": "https://checkout.stripe.com/c/pay/..."
}
```

### 4.3 View Purchased Strategies
```bash
curl -s -H "Authorization: Bearer YOUR_AUTH_TOKEN" \
  "https://strategiz-api-43628135674.us-east1.run.app/v1/marketplace/strategies/user/purchases" | jq .
```

**Expected Response:**
```json
{
  "strategies": [
    {
      "id": "...",
      "name": "Purchased Strategy",
      "purchasedAt": "2025-01-15T10:00:00Z",
      "pricingModel": "FREE"
    }
  ]
}
```

---

## Test 5: Error Cases

### 5.1 Test Unauthenticated Access
```bash
# Should fail for authenticated endpoints
curl -s "https://strategiz-api-43628135674.us-east1.run.app/v1/marketplace/strategies/user/purchases" | jq .
```

**Expected:**
```json
{
  "code": "NOT_AUTHENTICATED",
  "message": "authorization"
}
```

### 5.2 Test Invalid Strategy ID
```bash
curl -s -H "Authorization: Bearer YOUR_AUTH_TOKEN" \
  "https://strategiz-api-43628135674.us-east1.run.app/v1/marketplace/strategies/invalid-id" | jq .
```

**Expected:**
```json
{
  "error": "Strategy not found"
}
```

### 5.3 Test Purchasing Already Owned Strategy
1. Try to purchase a strategy you already own
2. Should show appropriate error message

---

## Test 6: Data Integrity

### 6.1 Verify Creator Information
For each strategy in the marketplace, verify:
- Creator name is shown (not "Unknown")
- Creator email is not exposed publicly
- Creator photo URL loads correctly (if available)

### 6.2 Verify Performance Timestamps
- Check that "Last updated" dates are accurate
- Performance data should match the last backtest run
- Relative time formatting should work ("2 days ago", "3 months ago")

### 6.3 Verify Deployment Count
1. Deploy a strategy as an alert or bot
2. Check if deployment count increments on the marketplace card
3. Stop the deployment
4. Check if deployment count decrements

---

## Known Issues / Limitations

1. **Publishing UI Not Implemented**: Currently need to use API to publish strategies
2. **Console Modules Disabled**: Admin console features temporarily disabled for Cloud Build
3. **Performance Update**: Manual refresh not implemented yet - need to re-run backtest

---

## Success Criteria

✅ Marketplace page loads and displays strategies
✅ Pricing badges show correct colors
✅ Performance metrics display correctly
✅ Deployment count shows on cards
✅ FREE strategy purchase works
✅ Stripe checkout flow works for ONE_TIME purchases
✅ Purchased strategies appear in "My Strategies"
✅ Creator information displays correctly
✅ Timestamps show relative time formatting

---

## Troubleshooting

### Issue: Marketplace shows no strategies
- **Solution**: Publish at least one strategy with backtest results

### Issue: "date-fns" error in browser console
- **Solution**: Already fixed and redeployed - clear browser cache

### Issue: Stripe checkout fails
- **Solution**: Check that Stripe keys are configured in Vault

### Issue: Performance data not showing
- **Solution**: Ensure strategy has completed backtest before publishing

### Issue: Authentication errors
- **Solution**: Log out and log back in to refresh token

---

## API Endpoints Summary

| Method | Endpoint | Auth Required | Description |
|--------|----------|---------------|-------------|
| GET | `/v1/marketplace/strategies` | No | List public strategies |
| GET | `/v1/marketplace/strategies/{id}` | Yes | Get strategy details |
| POST | `/v1/marketplace/strategies` | Yes | Create strategy |
| PUT | `/v1/marketplace/strategies/{id}` | Yes | Update strategy |
| DELETE | `/v1/marketplace/strategies/{id}` | Yes | Delete strategy |
| POST | `/v1/marketplace/strategies/{id}/purchase` | Yes | Purchase FREE strategy |
| POST | `/v1/marketplace/strategies/{id}/checkout` | Yes | Create Stripe checkout |
| POST | `/v1/marketplace/strategies/{id}/apply` | Yes | Apply strategy to portfolio |
| GET | `/v1/marketplace/strategies/user/purchases` | Yes | Get user's purchases |
| GET | `/v1/marketplace/strategies/user/strategies` | Yes | Get user's created strategies |

---

## Next Steps

After successful testing:
1. Implement "My Strategies" screen with publishing UI
2. Add "Update Performance" button for manual backtest refresh
3. Implement subscription management UI
4. Add strategy reviews and ratings
5. Implement fork functionality for purchased strategies
