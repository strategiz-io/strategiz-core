# Strategiz Firestore Database ERD

Complete Entity-Relationship Diagram for the Strategiz platform Firestore database.

**Date**: December 2024
**Status**: Current production schema

---

## Table of Contents

1. [Overview](#overview)
2. [Collection Architecture](#collection-architecture)
3. [Top-Level Collections](#top-level-collections)
4. [User Subcollections](#user-subcollections)
5. [Strategy Subcollections](#strategy-subcollections)
6. [Entity Details](#entity-details)
7. [Key Relationships](#key-relationships)
8. [Query Patterns](#query-patterns)
9. [Architecture Decisions](#architecture-decisions)

---

## Overview

### Database Type
- **Platform**: Firebase Firestore (NoSQL document database)
- **ID Strategy**: Firebase auto-generated IDs (20-character alphanumeric)
- **Data Model**: Document-subcollection hierarchy

### Key Patterns
- **Denormalization**: Used for performance (e.g., `deploymentCount`, `subscriberCount`)
- **Subcollections**: User-scoped data under `users/{userId}/`
- **Top-Level**: Shared entities (strategies, symbols, sessions)
- **Soft Deletes**: `isActive: false` for all entities
- **Audit Trail**: `createdBy`, `createdAt`, `updatedBy`, `updatedAt` on all entities

---

## Collection Architecture

```
firestore/
├── users/                              # Top-level: User accounts
│   └── {userId}/
│       ├── authentication_methods/     # Subcollection: Auth methods (passkey, totp, etc.)
│       ├── preferences/                # Subcollection: User preferences
│       │   ├── ai                     # Document: AI model preferences
│       │   └── alerts                 # Document: Alert notification preferences
│       ├── subscription/              # Subcollection: Platform subscription
│       │   └── current                # Document: Current tier (Scout/Trader/Strategist)
│       ├── watchlist/                 # Subcollection: Market watchlist
│       ├── profile/                   # Subcollection: User profile
│       ├── activity/                  # Subcollection: Activity log
│       ├── portfolio/                 # Subcollection: Portfolio data
│       │   ├── {providerId}/          # Document: Provider-specific portfolio
│       │   └── summary                # Document: Aggregated portfolio
│       └── providers/                 # Subcollection: Connected providers
│
├── strategies/                         # Top-level: Trading strategies
│   └── {strategyId}/
│       ├── subscriptions/             # Subcollection: User subscriptions to this strategy
│       ├── comments/                  # Subcollection: User comments
│       ├── performance/               # Subcollection: Performance metrics
│       └── alertHistory/              # Subcollection: Alert execution history
│
├── strategyAlerts/                    # Top-level: Alert deployments (UNDER REVIEW)
├── strategyBots/                      # Top-level: Bot deployments (UNDER REVIEW)
├── strategyOwnershipTransfers/        # Top-level: Ownership transfer audit trail
├── sessions/                          # Top-level: Active user sessions
├── symbols/                           # Top-level: Market symbols catalog
├── featureFlags/                      # Top-level: Feature flag configuration
├── devices/                           # Top-level: Registered devices
├── marketDataCoverage/                # Top-level: Market data availability
└── system/                            # Top-level: System configuration
    └── quality_cache/                 # Subcollection: Code quality metrics cache
```

---

## Top-Level Collections

### 1. `users/` Collection

**Purpose**: User account information
**Type**: Top-level
**Document ID**: Auto-generated Firebase UID

**Fields**:
```typescript
{
  id: string;              // Firebase UID
  email: string;           // Primary email
  displayName: string;     // User's display name
  role: "USER" | "ADMIN";  // Access level
  isActive: boolean;       // Soft delete flag
  createdAt: Timestamp;
  updatedAt: Timestamp;
  createdBy: string;
  updatedBy: string;
}
```

**Relationships**:
- 1:N → `users/{userId}/authentication_methods/`
- 1:N → `users/{userId}/preferences/`
- 1:1 → `users/{userId}/subscription/current`
- 1:N → `users/{userId}/watchlist/`
- 1:1 → `users/{userId}/profile/`
- 1:N → `users/{userId}/activity/`
- 1:N → `users/{userId}/portfolio/`
- 1:N → `users/{userId}/providers/`
- 1:N → `strategies/` (as creator)
- 1:N → `strategies/` (as owner)
- 1:N → `strategies/{strategyId}/subscriptions/` (as subscriber)

---

### 2. `strategies/` Collection

**Purpose**: Trading strategy definitions
**Type**: Top-level
**Document ID**: Auto-generated

**Fields**:
```typescript
{
  id: string;
  name: string;
  description: string;

  // Owner subscription model
  creatorId: string;       // Original author (immutable)
  ownerId: string;         // Current rights holder (mutable)

  // Status fields (NEW - after cleanup)
  publishStatus: "DRAFT" | "PUBLISHED";
  publicStatus: "PRIVATE" | "SUBSCRIBERS_ONLY" | "PUBLIC";

  // LEGACY (TO BE REMOVED):
  // status: string;
  // isPublic: boolean;
  // publishedAt: Timestamp;
  // deploymentType: string;
  // deploymentStatus: string;
  // deployedAt: Timestamp;
  // deploymentId: string;

  // Denormalized metrics
  deploymentCount: number;  // How many users have deployed this
  subscriberCount: number;  // Active subscriptions

  // Code and configuration
  code: string;            // Python strategy code
  language: "Python" | "PineScript" | "Java";

  // Pricing
  pricing: {
    subscriptionPrice: number;  // Monthly subscription
    oneTimePrice: number;       // Full ownership purchase
  };

  // Performance metrics (denormalized)
  performance: {
    totalReturns: number;
    sharpeRatio: number;
    maxDrawdown: number;
    winRate: number;
  };

  // Audit
  isActive: boolean;
  createdAt: Timestamp;
  updatedAt: Timestamp;
  createdBy: string;
  updatedBy: string;
}
```

**Relationships**:
- N:1 → `users/` (creatorId)
- N:1 → `users/` (ownerId)
- 1:N → `strategies/{strategyId}/subscriptions/`
- 1:N → `strategies/{strategyId}/comments/`
- 1:N → `strategies/{strategyId}/performance/`
- 1:N → `strategies/{strategyId}/alertHistory/`
- 1:N → `strategyAlerts/` (strategy has many alert deployments)
- 1:N → `strategyBots/` (strategy has many bot deployments)
- 1:N → `strategyOwnershipTransfers/`

---

### 3. `strategyAlerts/` Collection ⚠️ UNDER REVIEW

**Purpose**: Alert deployments by users
**Type**: Top-level (CONSIDERING MOVE TO SUBCOLLECTION)
**Document ID**: Auto-generated

**Current Path**: `strategyAlerts/{alertId}`
**Proposed Path**: `users/{userId}/strategyAlerts/{alertId}` (SUBCOLLECTION)

**Fields**:
```typescript
{
  id: string;
  userId: string;          // Who deployed this alert
  strategyId: string;      // Which strategy

  // Owner subscription model
  subscriptionId: string;  // Which subscription allows this deployment
  strategyOwnerId: string; // Who owned strategy when deployed
  strategyCreatorId: string; // Original creator (attribution)

  // Alert configuration
  symbol: string;          // AAPL, BTC, etc.
  conditions: object;      // Alert trigger conditions
  channel: "EMAIL" | "SMS" | "PUSH";

  // Status
  status: "ACTIVE" | "PAUSED" | "STOPPED";
  lastTriggered: Timestamp;
  triggerCount: number;

  // LEGACY (TO BE REMOVED):
  // deploymentType: string;  // Always "ALERT" - redundant

  // Audit
  isActive: boolean;
  createdAt: Timestamp;
  updatedAt: Timestamp;
}
```

**Relationships**:
- N:1 → `users/` (userId)
- N:1 → `strategies/` (strategyId)
- N:1 → `strategies/{strategyId}/subscriptions/` (subscriptionId)

**Architecture Issue**:
- ❌ **Current**: Top-level collection allows cross-user queries but complicates ownership
- ✅ **Proposed**: Move to `users/{userId}/strategyAlerts/` subcollection
  - Better ownership model
  - Automatic cleanup when user deleted
  - Clearer security rules
  - Can still query by strategyId using collection group queries

---

### 4. `strategyBots/` Collection ⚠️ UNDER REVIEW

**Purpose**: Automated bot deployments
**Type**: Top-level (CONSIDERING MOVE TO SUBCOLLECTION)
**Document ID**: Auto-generated

**Current Path**: `strategyBots/{botId}`
**Proposed Path**: `users/{userId}/strategyBots/{botId}` (SUBCOLLECTION)

**Fields**:
```typescript
{
  id: string;
  userId: string;          // Who deployed this bot
  strategyId: string;      // Which strategy

  // Owner subscription model
  subscriptionId: string;  // Which subscription allows this deployment
  strategyOwnerId: string; // Who owned strategy when deployed
  strategyCreatorId: string; // Original creator (attribution)

  // Bot configuration
  symbol: string;          // AAPL, BTC, etc.
  environment: "PAPER" | "LIVE";  // NOT a type - it's an environment!
  providerId: string;      // Which broker (Alpaca, Robinhood, etc.)

  // Position management
  positionSize: number;
  stopLoss: number;
  takeProfit: number;

  // Status
  status: "RUNNING" | "PAUSED" | "STOPPED";
  lastExecution: Timestamp;
  executionCount: number;

  // Performance
  totalPnL: number;
  winRate: number;

  // Audit
  isActive: boolean;
  createdAt: Timestamp;
  updatedAt: Timestamp;
}
```

**Relationships**:
- N:1 → `users/` (userId)
- N:1 → `strategies/` (strategyId)
- N:1 → `strategies/{strategyId}/subscriptions/` (subscriptionId)
- N:1 → `users/{userId}/providers/` (providerId)

**Architecture Issue**:
- ❌ **Current**: Top-level collection, same issues as alerts
- ✅ **Proposed**: Move to `users/{userId}/strategyBots/` subcollection
- **Note**: `environment: "PAPER" | "LIVE"` is NOT a deployment type - it's a subfield indicating trading mode

---

### 5. `strategyOwnershipTransfers/` Collection

**Purpose**: Audit trail for strategy ownership transfers
**Type**: Top-level
**Document ID**: Auto-generated

**Fields**:
```typescript
{
  id: string;
  strategyId: string;
  strategyName: string;
  fromOwnerId: string;     // Previous owner
  toOwnerId: string;       // New owner
  purchasePrice: number;   // Amount paid (0 for gifts)
  transactionId: string;   // Stripe transaction ID
  subscribersTransferred: number;
  monthlyRevenueTransferred: number;
  transferredAt: Timestamp;

  // Audit
  isActive: boolean;
  createdAt: Timestamp;
  createdBy: string;
}
```

**Relationships**:
- N:1 → `strategies/` (strategyId)
- N:1 → `users/` (fromOwnerId)
- N:1 → `users/` (toOwnerId)

---

### 6. `sessions/` Collection

**Purpose**: Active user sessions
**Type**: Top-level
**Document ID**: Auto-generated session ID

**Fields**:
```typescript
{
  id: string;
  userId: string;
  deviceId: string;
  ipAddress: string;
  userAgent: string;
  acr: "0" | "1" | "2";    // Authentication Context Rating
  expiresAt: Timestamp;
  lastAccessedAt: Timestamp;

  // Audit
  isActive: boolean;
  createdAt: Timestamp;
}
```

**Relationships**:
- N:1 → `users/` (userId)
- N:1 → `devices/` (deviceId)

---

### 7. `symbols/` Collection

**Purpose**: Market symbols catalog
**Type**: Top-level
**Document ID**: Symbol ticker (AAPL, BTC, etc.)

**Fields**:
```typescript
{
  id: string;              // Symbol ticker (AAPL)
  name: string;            // Apple Inc.
  type: "STOCK" | "CRYPTO" | "ETF" | "FOREX";
  exchange: string;
  isActive: boolean;
  createdAt: Timestamp;
  updatedAt: Timestamp;
}
```

---

### 8. `featureFlags/` Collection

**Purpose**: Feature flag configuration
**Type**: Top-level
**Document ID**: Feature flag key

**Fields**:
```typescript
{
  id: string;              // Feature key
  name: string;            // Display name
  description: string;
  enabled: boolean;
  enabledForUsers: string[]; // User IDs with access
  rolloutPercentage: number; // 0-100

  // Audit
  isActive: boolean;
  createdAt: Timestamp;
  updatedAt: Timestamp;
}
```

---

### 9. `devices/` Collection

**Purpose**: Registered user devices
**Type**: Top-level
**Document ID**: Auto-generated

**Fields**:
```typescript
{
  id: string;
  userId: string;
  deviceName: string;
  deviceType: "WEB" | "MOBILE" | "DESKTOP";
  lastSeenAt: Timestamp;

  // Audit
  isActive: boolean;
  createdAt: Timestamp;
}
```

**Relationships**:
- N:1 → `users/` (userId)

---

### 10. `marketDataCoverage/` Collection

**Purpose**: Market data availability tracking
**Type**: Top-level
**Document ID**: Symbol ticker

**Fields**:
```typescript
{
  id: string;              // Symbol ticker
  earliestDate: Timestamp;
  latestDate: Timestamp;
  totalBars: number;
  provider: "ALPACA" | "YAHOO" | "COINGECKO";

  // Audit
  isActive: boolean;
  updatedAt: Timestamp;
}
```

---

### 11. `system/` Collection

**Purpose**: System-level configuration
**Type**: Top-level
**Document ID**: Configuration key

**Subcollections**:
- `system/quality_cache/history/` - Code quality metrics cache

---

## User Subcollections

### 1. `users/{userId}/authentication_methods/` Subcollection

**Purpose**: User's authentication methods
**Type**: Subcollection under users
**Document ID**: Auto-generated

**Fields**:
```typescript
{
  id: string;
  userId: string;          // Parent user
  type: "PASSWORD" | "TOTP" | "SMS" | "EMAIL_OTP" | "PASSKEY" | "OAUTH";
  provider: string;        // For OAuth: "google", "facebook", etc.
  isPrimary: boolean;
  metadata: object;        // Type-specific data

  // Audit
  isActive: boolean;
  createdAt: Timestamp;
  updatedAt: Timestamp;
}
```

**Path**: `users/{userId}/authentication_methods/{methodId}`

---

### 2. `users/{userId}/preferences/` Subcollection

**Purpose**: User preference documents
**Type**: Subcollection under users

#### Document: `users/{userId}/preferences/ai`

**Fields**:
```typescript
{
  preferredModel: string;  // "gemini-3-flash", "claude-opus-4-5", etc.
  temperature: number;
  maxTokens: number;

  // Audit
  updatedAt: Timestamp;
  updatedBy: string;
}
```

#### Document: `users/{userId}/preferences/alerts`

**Fields**:
```typescript
{
  emailEnabled: boolean;
  smsEnabled: boolean;
  pushEnabled: boolean;
  frequency: "IMMEDIATE" | "HOURLY" | "DAILY";

  // Audit
  updatedAt: Timestamp;
  updatedBy: string;
}
```

---

### 3. `users/{userId}/subscription/` Subcollection

**Purpose**: Platform subscription tier
**Type**: Subcollection under users

#### Document: `users/{userId}/subscription/current`

**Fields**:
```typescript
{
  tier: "SCOUT" | "TRADER" | "STRATEGIST";
  status: "ACTIVE" | "EXPIRED" | "CANCELLED";
  stripeSubscriptionId: string;
  stripeCustomerId: string;

  // Usage tracking
  dailyAIMessages: number;
  dailyAIMessagesUsed: number;
  lastResetAt: Timestamp;

  // Billing
  startedAt: Timestamp;
  expiresAt: Timestamp;
  cancelledAt: Timestamp;

  // Audit
  updatedAt: Timestamp;
}
```

**Subscription Tiers**:
- **Scout** (Free): 25 AI msgs/day, Gemini Flash + GPT-4o-mini
- **Trader** ($19/mo): 200 msgs/day, + Claude Haiku + GPT-4o
- **Strategist** ($49/mo): 500 msgs/day, ALL models (cost protected)

---

### 4. `users/{userId}/watchlist/` Subcollection

**Purpose**: User's market watchlist
**Type**: Subcollection under users
**Document ID**: Auto-generated

**Fields**:
```typescript
{
  id: string;
  userId: string;
  symbol: string;          // AAPL, BTC, etc.
  currentPrice: number;
  priceChange24h: number;
  addedAt: Timestamp;

  // Audit
  isActive: boolean;
  createdAt: Timestamp;
}
```

**Path**: `users/{userId}/watchlist/{itemId}`

---

### 5. `users/{userId}/profile/` Subcollection

**Purpose**: User profile information
**Type**: Subcollection under users

#### Document: `users/{userId}/profile/profile`

**Fields**:
```typescript
{
  userId: string;
  bio: string;
  location: string;
  website: string;
  imageUrl: string;

  // Social stats (denormalized)
  subscriberCount: number;  // Users subscribed to this user's strategies
  strategyCount: number;    // Strategies owned

  // Audit
  updatedAt: Timestamp;
}
```

---

### 6. `users/{userId}/activity/` Subcollection

**Purpose**: User activity log
**Type**: Subcollection under users
**Document ID**: Auto-generated

**Fields**:
```typescript
{
  id: string;
  userId: string;
  type: "STRATEGY_CREATED" | "STRATEGY_PUBLISHED" | "SUBSCRIPTION_PURCHASED" | "ALERT_TRIGGERED" | "BOT_EXECUTION";
  description: string;
  metadata: object;
  occurredAt: Timestamp;

  // Audit
  createdAt: Timestamp;
}
```

**Path**: `users/{userId}/activity/{activityId}`

---

### 7. `users/{userId}/portfolio/` Subcollection

**Purpose**: Portfolio data from connected providers
**Type**: Subcollection under users

#### Document: `users/{userId}/portfolio/{providerId}`

**Fields**:
```typescript
{
  userId: string;
  providerId: string;      // "alpaca", "robinhood", "coinbase", etc.
  totalValue: number;
  totalPnL: number;
  lastSyncedAt: Timestamp;

  // Audit
  updatedAt: Timestamp;
}
```

#### Document: `users/{userId}/portfolio/summary`

**Fields**:
```typescript
{
  userId: string;
  aggregatedValue: number;  // Sum across all providers
  aggregatedPnL: number;
  providers: string[];
  lastUpdatedAt: Timestamp;

  // Audit
  updatedAt: Timestamp;
}
```

---

### 8. `users/{userId}/providers/` Subcollection

**Purpose**: Connected broker/exchange providers
**Type**: Subcollection under users
**Document ID**: Provider ID

**Fields**:
```typescript
{
  id: string;              // "alpaca", "robinhood", etc.
  userId: string;
  providerName: string;
  status: "CONNECTED" | "DISCONNECTED" | "ERROR";
  lastSyncedAt: Timestamp;

  // Credentials (encrypted)
  accessToken: string;     // Encrypted
  refreshToken: string;    // Encrypted
  expiresAt: Timestamp;

  // Audit
  isActive: boolean;
  createdAt: Timestamp;
  updatedAt: Timestamp;
}
```

**Path**: `users/{userId}/providers/{providerId}`

---

## Strategy Subcollections

### 1. `strategies/{strategyId}/subscriptions/` Subcollection

**Purpose**: User subscriptions to a strategy
**Type**: Subcollection under strategies
**Document ID**: Auto-generated

**Fields**:
```typescript
{
  id: string;
  userId: string;          // Subscriber
  strategyId: string;      // Parent strategy
  ownerId: string;         // Current owner (receives payments)
  creatorId: string;       // Original creator (attribution)

  // Subscription details
  status: "ACTIVE" | "CANCELLED" | "EXPIRED";
  stripeSubscriptionId: string;
  pricePaid: number;       // Monthly price

  // Lifecycle
  startedAt: Timestamp;
  expiresAt: Timestamp;
  cancelledAt: Timestamp;

  // Audit
  isActive: boolean;
  createdAt: Timestamp;
  updatedAt: Timestamp;
}
```

**Path**: `strategies/{strategyId}/subscriptions/{subscriptionId}`

**Relationships**:
- N:1 → `users/` (userId - subscriber)
- N:1 → `users/` (ownerId - receives payments)
- N:1 → `strategies/` (strategyId)

---

### 2. `strategies/{strategyId}/comments/` Subcollection

**Purpose**: User comments on a strategy
**Type**: Subcollection under strategies
**Document ID**: Auto-generated

**Fields**:
```typescript
{
  id: string;
  strategyId: string;
  userId: string;          // Commenter
  content: string;
  likes: number;

  // Audit
  isActive: boolean;
  createdAt: Timestamp;
  updatedAt: Timestamp;
}
```

**Path**: `strategies/{strategyId}/comments/{commentId}`

---

### 3. `strategies/{strategyId}/performance/` Subcollection

**Purpose**: Historical performance metrics
**Type**: Subcollection under strategies
**Document ID**: Date (YYYY-MM-DD)

**Fields**:
```typescript
{
  id: string;              // Date (2024-12-30)
  strategyId: string;
  date: Timestamp;
  totalReturns: number;
  sharpeRatio: number;
  maxDrawdown: number;
  winRate: number;
  tradesExecuted: number;

  // Audit
  createdAt: Timestamp;
}
```

**Path**: `strategies/{strategyId}/performance/{date}`

---

### 4. `strategies/{strategyId}/alertHistory/` Subcollection

**Purpose**: Alert execution history
**Type**: Subcollection under strategies
**Document ID**: Auto-generated

**Fields**:
```typescript
{
  id: string;
  strategyId: string;
  alertId: string;
  userId: string;
  triggeredAt: Timestamp;
  symbol: string;
  signal: "BUY" | "SELL" | "HOLD";
  price: number;

  // Audit
  createdAt: Timestamp;
}
```

**Path**: `strategies/{strategyId}/alertHistory/{executionId}`

---

## Key Relationships

### User-Strategy Relationships

```
User (1) ----creates----> (N) Strategy [creatorId]
User (1) ----owns-------> (N) Strategy [ownerId]
User (N) <--subscribes--> (N) Strategy [via subscriptions subcollection]
User (1) ----deploys----> (N) StrategyAlert [userId]
User (1) ----deploys----> (N) StrategyBot [userId]
```

### Strategy Ownership Model

```
Strategy
├── creatorId (immutable) → User [Original author]
└── ownerId (mutable)     → User [Current rights holder]

When ownership transfers:
1. Strategy.ownerId changes
2. All subscriptions update ownerId field
3. Stripe payment routing updates
4. OwnershipTransfer record created
```

### Subscription Lifecycle

```
StrategySubscription
├── userId → User [Subscriber]
├── ownerId → User [Receives payments]
├── strategyId → Strategy [Which strategy]
└── Links to:
    ├── StrategyAlert [subscriptionId]
    └── StrategyBot [subscriptionId]

When subscription expires:
1. Subscription.status = "EXPIRED"
2. All linked alerts/bots stop automatically
```

---

## Query Patterns

### 1. Get User's Owned Strategies

```javascript
// Collection group query (top-level)
db.collection('strategies')
  .where('ownerId', '==', userId)
  .where('isActive', '==', true)
  .get();
```

### 2. Get User's Created Strategies

```javascript
db.collection('strategies')
  .where('creatorId', '==', userId)
  .where('isActive', '==', true)
  .get();
```

### 3. Get User's Subscribed Strategies

```javascript
// Collection group query across all strategies
db.collectionGroup('subscriptions')
  .where('userId', '==', userId)
  .where('status', '==', 'ACTIVE')
  .get();
```

### 4. Get All Deployments for a Strategy (CURRENT - Top-level)

```javascript
// Query top-level collections
const alerts = await db.collection('strategyAlerts')
  .where('strategyId', '==', strategyId)
  .where('isActive', '==', true)
  .get();

const bots = await db.collection('strategyBots')
  .where('strategyId', '==', strategyId)
  .where('isActive', '==', true)
  .get();
```

### 5. Get All Deployments for a Strategy (PROPOSED - Subcollections)

```javascript
// Collection group query across all users
const alerts = await db.collectionGroup('strategyAlerts')
  .where('strategyId', '==', strategyId)
  .where('isActive', '==', true)
  .get();

const bots = await db.collectionGroup('strategyBots')
  .where('strategyId', '==', strategyId)
  .where('isActive', '==', true)
  .get();
```

### 6. Get User's Active Deployments (PROPOSED - Subcollections)

```javascript
// Direct subcollection queries (faster, cheaper)
const alerts = await db.collection('users').doc(userId)
  .collection('strategyAlerts')
  .where('status', '==', 'ACTIVE')
  .get();

const bots = await db.collection('users').doc(userId)
  .collection('strategyBots')
  .where('status', '==', 'RUNNING')
  .get();
```

### 7. Update deploymentCount on Strategy (Denormalized)

```javascript
// When alert/bot is created
await strategyRef.update({
  deploymentCount: admin.firestore.FieldValue.increment(1)
});

// When alert/bot is stopped
await strategyRef.update({
  deploymentCount: admin.firestore.FieldValue.increment(-1)
});
```

---

## Architecture Decisions

### ⚠️ DECISION PENDING: Alerts/Bots Collection Structure

**Current Implementation**: Top-level collections
- `strategyAlerts/{alertId}`
- `strategyBots/{botId}`

**Proposed Implementation**: User subcollections
- `users/{userId}/strategyAlerts/{alertId}`
- `users/{userId}/strategyBots/{botId}`

#### Arguments for Subcollections (User's Preference)

**Pros**:
1. ✅ **Ownership clarity**: Alerts/bots clearly belong to users
2. ✅ **Automatic cleanup**: Delete user → automatically deletes all alerts/bots
3. ✅ **Security rules**: Simpler Firebase rules (`/users/{userId}/strategyAlerts/{alertId}` can only be accessed by `userId`)
4. ✅ **Logical grouping**: User's deployments are under their document
5. ✅ **Faster user queries**: Direct path to user's alerts/bots (no need to filter top-level collection)
6. ✅ **Cheaper queries**: Subcollection queries don't scan entire database
7. ✅ **Consistency**: Matches pattern of other user-scoped data (watchlist, preferences, etc.)

**Cons**:
1. ❌ **Collection group queries**: Need collection group query to find all deployments for a strategy (slightly more expensive)
2. ❌ **Migration complexity**: Must move existing data from top-level to subcollections

#### Arguments for Top-Level (Current)

**Pros**:
1. ✅ **Strategy queries**: Easy to find all deployments for a strategy
2. ✅ **Already implemented**: No migration needed

**Cons**:
1. ❌ **Ownership unclear**: Not obvious that alerts/bots belong to users
2. ❌ **Manual cleanup**: Must explicitly delete alerts/bots when user deleted
3. ❌ **Security complexity**: Must validate userId in rules
4. ❌ **Slower user queries**: Must filter top-level collection
5. ❌ **More expensive**: Every user query scans entire collection

#### Recommendation

**Move to subcollections** for the following reasons:

1. **Ownership model**: Alerts and bots are fundamentally user-owned resources, not shared entities like strategies
2. **Security**: Firebase security rules are simpler and more robust with subcollections
3. **Performance**: User-scoped queries (most common) are faster and cheaper
4. **Consistency**: Aligns with existing architecture (preferences, watchlist, profile all under users)
5. **Collection group queries**: The rare case of "find all deployments for strategy" can still be done efficiently with collection group queries

**Migration Path**:
1. Create new subcollection repositories
2. Write migration script to move data
3. Update all controllers to use new paths
4. Run migration in dev, then prod
5. Delete old top-level collections

---

### Module Structure Consideration

**Question**: Should alerts and bots have separate data modules?

**Current**: Both in `data-strategy` module
**Proposed**: Separate `data-alert` and `data-bot` modules

#### Arguments for Separation

**Pros**:
1. ✅ **Separation of concerns**: Alerts and bots have different lifecycles and purposes
2. ✅ **Independent evolution**: Can modify alert logic without affecting bots
3. ✅ **Clearer dependencies**: Business modules can depend only on what they need
4. ✅ **Testing**: Easier to test alert/bot logic in isolation

**Cons**:
1. ❌ **More modules**: Adds complexity to build process
2. ❌ **Code duplication**: Both share similar patterns (subscription linkage, deployment lifecycle)

#### Recommendation

**Keep in data-strategy for now** because:
1. Alerts and bots are both strategy deployment mechanisms
2. They share significant common patterns (subscription linkage, owner tracking)
3. Creating separate modules adds overhead without clear benefit
4. If they diverge significantly in the future, we can split them then

**However**, if alerts/bots become complex enough to warrant separate business logic modules (`business-alert`, `business-bot`), then separating the data modules would make sense.

---

### Status Field Cleanup

**DECISION**: Remove legacy status fields from Strategy entity

**Fields to Remove**:
- `status` (replaced by `publishStatus`)
- `isPublic` (replaced by `publicStatus`)
- `publishedAt` (inferred from `publishStatus == "PUBLISHED"`)
- `deploymentType` (doesn't belong on Strategy - it's per-alert/bot)
- `deploymentStatus` (doesn't belong on Strategy - use `deploymentCount`)
- `deployedAt` (doesn't belong on Strategy)
- `deploymentId` (doesn't belong on Strategy)

**Fields to Keep**:
- `publishStatus`: DRAFT | PUBLISHED (marketplace readiness)
- `publicStatus`: PRIVATE | SUBSCRIBERS_ONLY | PUBLIC (access control)
- `deploymentCount`: integer (denormalized count of active deployments)

**Migration**:
```javascript
// For each strategy document
const publishStatus = strategy.publishedAt ? 'PUBLISHED' : 'DRAFT';
const publicStatus = strategy.isPublic ? 'PUBLIC' : 'PRIVATE';

await strategyRef.update({
  publishStatus,
  publicStatus,
  deploymentCount: 0, // Will be recalculated

  // Remove legacy fields
  status: admin.firestore.FieldValue.delete(),
  isPublic: admin.firestore.FieldValue.delete(),
  publishedAt: admin.firestore.FieldValue.delete(),
  deploymentType: admin.firestore.FieldValue.delete(),
  deploymentStatus: admin.firestore.FieldValue.delete(),
  deployedAt: admin.firestore.FieldValue.delete(),
  deploymentId: admin.firestore.FieldValue.delete()
});
```

---

## Summary Statistics

**Total Collections**: 11 top-level + 12 subcollections = **23 collections**

**Top-Level Collections**:
1. users
2. strategies
3. strategyAlerts (under review)
4. strategyBots (under review)
5. strategyOwnershipTransfers
6. sessions
7. symbols
8. featureFlags
9. devices
10. marketDataCoverage
11. system

**User Subcollections**:
1. users/{userId}/authentication_methods
2. users/{userId}/preferences/ai
3. users/{userId}/preferences/alerts
4. users/{userId}/subscription/current
5. users/{userId}/watchlist
6. users/{userId}/profile
7. users/{userId}/activity
8. users/{userId}/portfolio
9. users/{userId}/providers

**Strategy Subcollections**:
1. strategies/{strategyId}/subscriptions
2. strategies/{strategyId}/comments
3. strategies/{strategyId}/performance
4. strategies/{strategyId}/alertHistory

**Proposed Additions** (if alerts/bots move to subcollections):
- users/{userId}/strategyAlerts
- users/{userId}/strategyBots

---

## Next Steps

1. ✅ Review this ERD with team
2. ⏳ **Decide**: Move alerts/bots to user subcollections?
3. ⏳ **Decide**: Create separate data modules for alerts/bots?
4. ⏳ Remove legacy status fields from Strategy.java
5. ⏳ Write and test migration scripts
6. ⏳ Update security rules for new structure
7. ⏳ Deploy to production

---

**Last Updated**: December 30, 2024
**Document Version**: 1.0
