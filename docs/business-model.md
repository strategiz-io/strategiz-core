# Strategiz Business Model

**Version**: 1.2
**Last Updated**: January 17, 2026
**Status**: APPROVED - Implementation in Progress

---

## Table of Contents

1. [Overview](#overview)
2. [Core Concepts](#core-concepts)
3. [Strategy Visibility Model](#strategy-visibility-model)
4. [Access Control Rules](#access-control-rules)
5. [User Scenarios](#user-scenarios)
6. [Ownership Transfer](#ownership-transfer)
7. [Subscription Model](#subscription-model)
8. [Platform Tiers](#platform-tiers)
9. [Product Feature Matrix](#product-feature-matrix)
10. [User Journeys](#user-journeys)
11. [Owner Subscription Setup](#owner-subscription-setup)
12. [Implementation Notes](#implementation-notes)

---

## Overview

Strategiz is a strategy marketplace where creators can publish trading strategies, users can subscribe to strategy owners to deploy their strategies, and the community can follow creators for updates.

### Key Principles

1. **Ownership is absolute** - Selling transfers complete ownership, original creator loses all access
2. **Following ≠ Access** - Following is purely social, grants no special access
3. **Subscription = Deployment** - Only subscribers can deploy strategies
4. **Performance is transparent** - All published+public strategies show full performance metrics
5. **Code is protected** - No one except owner sees the strategy code

---

## Core Concepts

### 1. Owner

**Definition**: The user who currently owns a strategy.

**How you become an owner:**
- Create a new strategy (you are the original owner)
- Purchase a strategy from another user (complete ownership transfer)

**Owner capabilities:**
- ✅ View strategy code
- ✅ Edit strategy (code, settings, metadata)
- ✅ Deploy strategy (as bot or alert)
- ✅ View all performance metrics
- ✅ Set pricing and visibility settings
- ✅ Transfer/sell ownership to another user
- ✅ Delete strategy

---

### 2. Subscriber

**Definition**: A user who pays a monthly subscription to a strategy owner.

**How it works:**
- Subscribe to an **OWNER** (not individual strategies)
- Monthly payment (e.g., $50/month) via Stripe
- Access to deploy **all PUBLIC strategies** owned by that user

**Subscriber capabilities:**
- ✅ Deploy any PUBLISHED + PUBLIC strategy from subscribed owner (as bot or alert)
- ✅ View performance metrics for PUBLIC strategies
- ❌ **CANNOT** access PRIVATE strategies (owner-only)
- ❌ **CANNOT** view strategy code
- ❌ **CANNOT** edit strategies
- ❌ **CANNOT** transfer ownership

**Important**: Subscription is to the OWNER, not strategies
- If Alice owns 5 strategies (3 PUBLIC, 2 PRIVATE), subscribers can only deploy the 3 PUBLIC ones
- If Alice sells a strategy to Bob, subscribers to Alice lose access to that strategy
- Subscriptions do NOT transfer with strategy ownership
- Owner controls what subscribers can access by setting strategies to PUBLIC

---

### 3. Follower

**Definition**: A user who follows another user for social updates.

**How it works:**
- Click "Follow" on a user's profile (FREE, no payment)
- Purely social relationship

**Follower capabilities:**
- 📢 See activity feed: "Alice published new strategy 'MACD Pro'"
- 📢 See updates: "Alice's 'RSI Scalper' hit 20% returns this month"
- 📊 View follower's public profile
- ❌ **NO special access to strategies**
- ❌ **NO deployment capabilities**

**Key point**: Following is completely separate from access control. It's for notifications and social graph only.

---

## Strategy Visibility Model

Each strategy has **two independent fields**:

### 1. publishStatus

**Values**: `DRAFT` | `PUBLISHED`

**Meaning**:
- `DRAFT` - Strategy is still in development, cannot be deployed
- `PUBLISHED` - Strategy is complete and ready for deployment

### 2. publicStatus

**Values**: `PRIVATE` | `PUBLIC`

**Meaning**:
- `PRIVATE` - Owner-only access, not visible to anyone else (including subscribers)
- `PUBLIC` - Visible to everyone, subscribers can deploy

---

### Valid Combinations

| publishStatus | publicStatus | Valid? | Description |
|---------------|--------------|--------|-------------|
| `DRAFT` | `PRIVATE` | ✅ | Work in progress, owner-only, cannot deploy |
| `DRAFT` | `PUBLIC` | ❌ | **INVALID** - Prevent in UI/API |
| `PUBLISHED` | `PRIVATE` | ✅ | Personal use - owner can deploy, no one else can see/access |
| `PUBLISHED` | `PUBLIC` | ✅ | Shared - visible in marketplace, subscribers can deploy |

---

### Detailed Capability Matrix

| Capability | DRAFT + PRIVATE | PUBLISHED + PRIVATE | PUBLISHED + PUBLIC |
|------------|-----------------|---------------------|-------------------|
| **Owner can view code** | ✅ | ✅ | ✅ |
| **Owner can edit** | ✅ | ✅ | ✅ |
| **Owner can deploy** | ❌ | ✅ | ✅ |
| **Visible in marketplace** | ❌ | ❌ | ✅ |
| **Subscribers can see** | ❌ | ❌ | ✅ |
| **Subscribers can deploy** | ❌ | ❌ | ✅ |
| **Public can see performance** | ❌ | ❌ | ✅ |
| **Can be listed for SALE** | ❌ | ❌ | ✅ |

---

### Use Cases

| Goal | State |
|------|-------|
| "I'm building a strategy" | DRAFT + PRIVATE |
| "I'm running this for myself, don't want to share" | PUBLISHED + PRIVATE |
| "I want my subscribers to use this" | PUBLISHED + PUBLIC |
| "I want to sell this strategy" | PUBLISHED + PUBLIC + Listed for Sale |

---

### Default Behavior

**When creating new strategy:**
```java
publishStatus = "DRAFT";
publicStatus = "PRIVATE";
```

**When user clicks "Publish":**
```java
publishStatus = "PUBLISHED";
// publicStatus stays PRIVATE (owner chooses to make PUBLIC separately)
```

**Validation:**
```java
if (publishStatus.equals("DRAFT") && publicStatus.equals("PUBLIC")) {
    throw new StrategizException(
        INVALID_STATUS_COMBINATION,
        "Cannot set draft strategy to public"
    );
}
```

---

## Access Control Rules

### What Can You See/Do?

| User Type | DRAFT + PRIVATE | PUBLISHED + PRIVATE | PUBLISHED + PUBLIC |
|-----------|-----------------|---------------------|-------------------|
| **Owner** | Everything | Everything | Everything |
| **Subscriber** | Cannot see | Cannot see | See name, performance, deploy |
| **Follower** | Cannot see | Cannot see | See name, performance (like public) |
| **Public** | Cannot see | Cannot see | See name, performance |

### Detailed Capabilities

#### DRAFT + PRIVATE (Work in Progress)
- **Owner**: Can view, edit, but CANNOT deploy
- **Everyone else**: Cannot see it exists

#### PUBLISHED + PRIVATE (Personal Use)
- **Owner**: Full access - can view, edit, deploy
- **Everyone else**: Cannot see it exists (not in search/browse, not accessible to subscribers)

#### PUBLISHED + PUBLIC (Shared / Marketplace)
- **Owner**: Full access
- **Subscribers**: Can see name, full performance, can deploy
- **Everyone**: Can see name, full performance, **cannot deploy**

---

## User Scenarios

### Scenario A: I'm the Owner of Strategy X

**What I can do:**
- ✅ View and edit strategy code
- ✅ View all performance metrics
- ✅ Deploy as bot/alert
- ✅ Set `publishStatus` (DRAFT/PUBLISHED)
- ✅ Set `publicStatus` (PRIVATE/PUBLIC)
- ✅ Set pricing
- ✅ Transfer/sell ownership
- ✅ Delete strategy

**What happens when I sell:**
- Buyer becomes new owner (gains all rights above)
- I lose ALL access and rights (complete transfer)
- No ongoing revenue share or access

---

### Scenario B: I'm Subscribed to Alice (who owns Strategy X)

**What I can do:**
- ✅ Deploy Strategy X as bot/alert (if PUBLISHED + PUBLIC)
- ✅ View full performance metrics for PUBLIC strategies
- ✅ Deploy ALL of Alice's PUBLIC strategies
- ❌ **CANNOT** access Alice's PRIVATE strategies (owner-only)
- ❌ Cannot view strategy code
- ❌ Cannot edit strategy
- ❌ Cannot transfer ownership

**What happens when I deploy:**
- Strategy runs against MY connected providers (Coinbase, Robinhood, etc.)
- Uses MY money/positions
- I configure MY risk settings (stop loss, position size)
- Alice does NOT see my deployment performance (privacy)

**What Alice controls:**
- Alice decides which strategies to make PUBLIC (shared with subscribers)
- Alice can keep certain strategies PRIVATE (her personal edge)
- If Alice changes a strategy from PUBLIC to PRIVATE, I lose access

---

### Scenario C: I'm Following Alice (who owns Strategy X)

**What I get:**
- 📢 Activity feed: "Alice published 'MACD Pro'"
- 📢 Updates: "Alice's 'RSI Scalper' hit 20% returns"
- 📊 View Alice's public profile

**What I CANNOT do:**
- ❌ No special access to Strategy X
- ❌ Cannot deploy (need to subscribe)
- ❌ Cannot view code
- Same access as any random public user for PUBLIC strategies

---

### Scenario D: I'm a Random User, Strategy X is PUBLISHED + PUBLIC

**What I can do:**
- ✅ Find Strategy X in search/browse
- ✅ View name, description, tags
- ✅ View full performance metrics (win rate, returns, drawdown, all charts)
- ✅ See owner info: "Created by Alice"
- ✅ Click "Subscribe to Alice for $50/mo" to gain deployment access
- ❌ Cannot view code
- ❌ Cannot deploy (need to subscribe first)

---

### Scenario E: I'm a Random User, Strategy X is PRIVATE or DRAFT

**What happens:**
- ❌ Cannot find in search/browse (invisible)
- ❌ Cannot access via direct link (404 or "Access Denied")
- 💡 PRIVATE strategies are owner-only (not even subscribers can see them)

---

## Ownership Transfer

### How Selling Works

**Current state:**
- Alice owns Strategy X
- Bob subscribes to Alice
- Charlie follows Alice

**Alice sells Strategy X to David for $500:**

1. **Ownership transfers completely:**
   - David becomes the new owner
   - Alice loses ALL access to Strategy X (cannot view, edit, deploy, or see performance)

2. **Subscriptions do NOT transfer:**
   - Bob is subscribed to ALICE (not Strategy X)
   - Bob can still deploy Alice's OTHER strategies
   - Bob CANNOT deploy Strategy X anymore (David owns it now)
   - If Bob wants to deploy Strategy X, he must subscribe to David

3. **Followers stay with Alice:**
   - Charlie still follows Alice
   - Charlie does NOT automatically follow David
   - Charlie sees: "Alice sold Strategy X to David"

### Business Model Rules

- ✅ Ownership transfer is complete and permanent
- ✅ One-time sale price (e.g., $500)
- ❌ NO ongoing revenue share for original creator
- ❌ NO residual access for original creator
- ✅ Unlimited ownership transfers allowed
- ✅ New owner can resell to another user

### Default State After Purchase

When a strategy is purchased, it automatically changes to:
- **publishStatus**: `PUBLISHED` (unchanged - was already published to be listed)
- **publicStatus**: `PRIVATE` (changed from PUBLIC to PRIVATE)

This means:
1. New owner can immediately deploy the strategy
2. Strategy is removed from marketplace (no longer visible to public)
3. New owner's subscribers do NOT automatically get access
4. New owner must explicitly set to PUBLIC to share with subscribers or resell

**Rationale**: Buyer paid for the strategy - they should control if/when to share it

---

## Subscription Model

### Subscription Tiers

**Current design**: Single tier per owner

- Owner sets their monthly price (e.g., $50/month)
- Subscriber gets access to deploy ALL **PUBLIC** strategies from that owner
- PRIVATE strategies remain owner-only (not included in subscription)
- No revenue sharing with platform (for MVP)

### Future Tiers (Optional)

Could implement multiple tiers per owner:
- **BASIC** ($20/mo): Deploy strategies
- **PREMIUM** ($50/mo): Deploy + priority support + exclusive strategies

### Subscription Lifecycle

1. **User clicks "Subscribe to Alice"**
   - Stripe checkout flow
   - Monthly recurring payment

2. **Subscription active:**
   - User can deploy all of Alice's PUBLISHED + PUBLIC strategies
   - PRIVATE strategies are excluded (Alice's personal use)
   - Renewed monthly automatically

3. **User cancels:**
   - Subscription continues until end of billing period
   - Then loses deployment access
   - Can still see PUBLIC performance (like any user)

4. **Ownership change:**
   - Alice sells a strategy to Bob
   - Subscribers to Alice CANNOT deploy that strategy anymore
   - No refund, no transfer to Bob's subscriber list

5. **Visibility change:**
   - If Alice changes a strategy from PUBLIC to PRIVATE, subscribers lose access
   - If Alice changes a strategy from PRIVATE to PUBLIC, subscribers gain access

---

## Platform Tiers

### Two-Gate Access Model

Users must pass TWO gates to deploy strategies:

1. **Strategy Access** (Pay Owner) - Right to USE the strategy
2. **Platform Tier** (Pay Platform) - Ability to RUN it (alerts, bots, execution)

```
┌──────────────────┐         ┌──────────────────┐
│  STRATEGY ACCESS │    +    │  PLATFORM TIER   │    =    ALERTS
│  (Pay Owner)     │         │  (Pay Platform)  │
└──────────────────┘         └──────────────────┘

    "What to trade"      +      "Get notified"     =    Value delivered
```

### Platform Fee

- **Owner keeps**: 85%
- **Platform fee**: 15%
- Example: $50/month subscription → Owner earns $42.50/subscriber

### Tier Structure

| Tier | Price | Alerts | Strategies | AI Credits | AI Models |
|------|-------|--------|------------|------------|-----------|
| **Free** | $0 | 0 | 0 | 0 | 0 |
| **Explorer** | $149/mo | 3 | 3 | 40K | 6 |
| **Pro** | $199/mo | Unlimited | Unlimited | 65K | 19 (all) |

---

## Product Feature Matrix

### Complete Feature Breakdown

| Feature | Free | Explorer ($149/mo) | Pro ($199/mo) |
|---------|------|--------------------|-----------------------|
| **MARKETPLACE** ||||
| Browse & view strategies | ✅ | ✅ | ✅ |
| Follow owners | ✅ | ✅ | ✅ |
| Subscribe/Purchase strategies | ✅ | ✅ | ✅ |
| **DEPLOYMENT** ||||
| Deploy alerts | ❌ | ✅ (3 alerts) | ✅ (unlimited) |
| Deploy bots | ❌ | ❌ | 🔜 Future |
| **CREATION** ||||
| Create strategies | ❌ | ✅ (3 strategies) | ✅ (unlimited) |
| Backtest | ❌ | ✅ | ✅ |
| Publish & sell | ❌ | ✅ | ✅ |
| Enable owner subscriptions | ❌ | ✅ | ✅ |
| **PORTFOLIO** ||||
| Connect broker | ✅ | ✅ | ✅ |
| Basic stats (holdings, P&L) | ✅ | ✅ | ✅ |
| Advanced analysis | ❌ | ✅ | ✅ |
| Export reports | ❌ | ✅ | ✅ |
| **LEARN** ||||
| Basic courses | ✅ | ✅ | ✅ |
| Advanced masterclasses | ❌ | ✅ | ✅ |
| **AI ASSISTANT** ||||
| AI chat | ❌ | ✅ (40K credits) | ✅ (65K credits) |
| AI models | ❌ | 6 models | 19 models (all) |

### Free Tier Value

| What's Free | Purpose |
|-------------|---------|
| Browse marketplace | Discovery funnel |
| View all PUBLIC strategy performance | Builds trust in platform |
| Follow owners (activity feed) | Engagement & retention |
| Subscribe/Purchase strategies (pay owner) | Owner revenue stream |
| Connect broker + basic stats | Hook for portfolio users |
| Basic learning courses | Trust building, SEO, onboarding |

### Paid Tier Value

| What's Paid | Why It's Paid |
|-------------|---------------|
| Deploy alerts | Core monetization - the main value |
| Create strategies | Requires compute for backtesting |
| Advanced portfolio analysis | Premium insights |
| Advanced courses | Value-add for subscribers |
| AI assistant | Compute costs |

---

## User Journeys

### Consumer Journey (Buys strategies)

```
Free: Browse → Follow owners → View performance → Basic portfolio
                    │
    Upgrade trigger: "I want alerts on this strategy"
                    │
                    ▼
Explorer ($149/mo): Subscribe to owner + Deploy alerts (3 max)
                    │
    Upgrade trigger: "I need more than 3 alerts"
                    │
                    ▼
Pro ($199/mo): Unlimited alerts + all AI models
```

### Creator Journey (Builds strategies)

```
Free: Browse → Learn basics → Study others' strategies
                    │
    Upgrade trigger: "I want to build my own"
                    │
                    ▼
Explorer ($149/mo): Create (3 max), backtest, publish → Enable subscriptions → Earn revenue
                    │
    Upgrade trigger: "I need more than 3 strategies"
                    │
                    ▼
Pro ($199/mo): Unlimited strategies + all AI models
```

### Analyst Journey (Portfolio focused)

```
Free: Connect broker → View holdings → Basic P&L
                    │
    Upgrade trigger: "I want risk metrics and reports"
                    │
                    ▼
Explorer ($149/mo): Full analysis suite + Export
```

---

## Owner Subscription Setup

### Requirements to Enable Subscriptions

**Required (hard block):**
- Verified email
- At least 1 PUBLISHED + PUBLIC strategy
- Stripe Connect account connected

**Recommended (soft warning):**
- Profile photo
- Profile bio
- At least 30 days backtest history on strategies

### Owner Subscription Settings

Owners can configure:
- **Monthly price**: Owner sets their price (suggested: $25-100/month)
- **Profile pitch**: Description shown to potential subscribers (500 char max)

Note: No trial period for owner subscriptions. Users can follow for free to validate before subscribing.

### Payout Schedule

- Payouts processed via Stripe Connect
- Payout frequency: Every 2 weeks
- Platform handles 1099 tax forms for US creators

---

## Implementation Notes

### Database Schema

**Strategy entity fields:**
```java
private String id;
private String ownerId;        // Current owner (can change)
private String creatorId;      // Original creator (never changes)
private String publishStatus;  // DRAFT | PUBLISHED
private String publicStatus;   // PRIVATE | PUBLIC
private String name;
private String description;
private String code;           // Protected, owner-only
// ... other fields
```

**UserSubscription entity:**
```java
private String id;
private String subscriberId;   // User who subscribes
private String ownerId;        // User being subscribed to
private String status;         // ACTIVE | CANCELLED | EXPIRED
private BigDecimal monthlyPrice;
private String stripeSubscriptionId;
private Timestamp subscribedAt;
// ... other fields
```

**UserFollowEntity:**
```java
private String id;             // Compound: {followerId}_{followingId}
private String followerId;     // User doing the following
private String followingId;    // User being followed
private Timestamp followedAt;
// ... other fields
```

---

### Access Control Logic

**StrategyAccessService pseudo-code:**

```java
public boolean canViewStrategy(String strategyId, String userId) {
    Strategy strategy = findById(strategyId);

    // Owner can always view
    if (userId.equals(strategy.getOwnerId())) {
        return true;
    }

    // Must be PUBLISHED + PUBLIC for anyone else to view
    // PRIVATE strategies are owner-only (not even subscribers)
    if ("PUBLISHED".equals(strategy.getPublishStatus()) &&
        "PUBLIC".equals(strategy.getPublicStatus())) {
        return true;
    }

    // Draft or Private = owner only
    return false;
}

public boolean canDeployStrategy(String strategyId, String userId) {
    Strategy strategy = findById(strategyId);

    // Must be published to deploy
    if (!"PUBLISHED".equals(strategy.getPublishStatus())) {
        return false;
    }

    // Owner can deploy any of their published strategies (public or private)
    if (userId.equals(strategy.getOwnerId())) {
        return true;
    }

    // Subscribers can only deploy PUBLIC strategies
    if ("PUBLIC".equals(strategy.getPublicStatus())) {
        return hasActiveSubscription(userId, strategy.getOwnerId());
    }

    // PRIVATE = owner only
    return false;
}

public boolean canViewCode(String strategyId, String userId) {
    Strategy strategy = findById(strategyId);

    // ONLY owner can view code
    return userId.equals(strategy.getOwnerId());
}
```

---

### API Endpoints

**Strategy visibility:**
```
GET /v1/strategies/public                    # Browse PUBLIC + PUBLISHED strategies
GET /v1/strategies/subscribed                # User's subscribed strategies
GET /v1/strategies/mine                      # User's owned strategies
GET /v1/strategies/{id}                      # View strategy (if allowed)
GET /v1/strategies/{id}/performance          # View performance (if allowed)
GET /v1/strategies/{id}/code                 # View code (owner only)
```

**Deployment:**
```
POST /v1/strategies/{id}/deploy              # Deploy as bot/alert (owner or subscriber)
```

**Social:**
```
POST /v1/social/follow/{userId}              # Follow a user
DELETE /v1/social/follow/{userId}            # Unfollow a user
GET /v1/social/feed                          # Get activity feed
```

**Subscription:**
```
POST /v1/subscriptions/subscribe/{ownerId}  # Subscribe to owner
GET /v1/subscriptions/mine                   # User's active subscriptions
DELETE /v1/subscriptions/{subscriptionId}    # Cancel subscription
```

---

## Approval

This business model is **APPROVED** for implementation.

**Approved by**: Product Team
**Effective Date**: December 31, 2025
**Review Cycle**: Quarterly

---

## Change Log

| Date | Version | Changes |
|------|---------|---------|
| 2025-12-31 | 1.0 | Initial business model documentation |
| 2026-01-16 | 1.1 | Clarified PRIVATE visibility: PRIVATE = owner-only (subscribers cannot access). Added detailed capability matrix. Added default state after purchase (PUBLISHED + PRIVATE). |
| 2026-01-17 | 1.2 | Added Two-Gate Access Model (Strategy Access + Platform Tier). Added complete Product Feature Matrix. Consolidated to 3 Platform Tiers: Free ($0), Explorer ($149/mo, 3 alerts/strategies, 40K credits, 6 models), Pro ($199/mo, unlimited, 65K credits, all 19 models). Added User Journeys with upgrade triggers. Added Owner Subscription Setup requirements. Added 15% platform fee. No trial for owner subscriptions. |
