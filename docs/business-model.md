# Strategiz Business Model

**Version**: 1.0
**Last Updated**: December 31, 2025
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
8. [Implementation Notes](#implementation-notes)

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
- Access to deploy **all published strategies** owned by that user

**Subscriber capabilities:**
- ✅ Deploy any PUBLISHED strategy from subscribed owner (as bot or alert)
- ✅ View performance metrics (same as public users for PUBLIC strategies)
- ✅ View PRIVATE strategies from subscribed owner (members-only access)
- ❌ **CANNOT** view strategy code
- ❌ **CANNOT** edit strategies
- ❌ **CANNOT** transfer ownership

**Important**: Subscription is to the OWNER, not strategies
- If Alice owns 5 strategies and sells 2 to Bob, subscribers to Alice can now only deploy her remaining 3 strategies
- Subscriptions do NOT transfer with strategy ownership

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
- `DRAFT` - Strategy is still in development, not ready for use
- `PUBLISHED` - Strategy is complete and ready for deployment

### 2. publicStatus

**Values**: `PRIVATE` | `PUBLIC`

**Meaning**:
- `PRIVATE` - Only owner and subscribers can see it
- `PUBLIC` - Everyone can see name and performance (but not code)

---

### Valid Combinations

| publishStatus | publicStatus | Valid? | Description |
|---------------|--------------|--------|-------------|
| `DRAFT` | `PRIVATE` | ✅ | Working on it, owner-only access |
| `DRAFT` | `PUBLIC` | ❌ | **INVALID** - Prevent in UI/API |
| `PUBLISHED` | `PRIVATE` | ✅ | "Members-only" - only subscribers can see/deploy |
| `PUBLISHED` | `PUBLIC` | ✅ | Public marketplace - everyone sees performance, subscribers deploy |

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
| **Subscriber** | Cannot see | See name, performance, deploy | See name, performance, deploy |
| **Follower** | Cannot see | Cannot see | See name, performance (like public) |
| **Public** | Cannot see | Cannot see | See name, performance |

### Detailed Capabilities

#### DRAFT + PRIVATE
- **Owner**: Full access (edit, view, deploy)
- **Everyone else**: Cannot see it exists

#### PUBLISHED + PRIVATE (Members-Only)
- **Owner**: Full access
- **Subscribers**: Can see name, full performance, can deploy
- **Everyone else**: Cannot see it exists (not in search/browse)

#### PUBLISHED + PUBLIC (Public Marketplace)
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
- ✅ Deploy Strategy X as bot/alert (if PUBLISHED)
- ✅ View full performance metrics
- ✅ View Strategy X even if PRIVATE (members-only access)
- ✅ Deploy ALL of Alice's published strategies (not just Strategy X)
- ❌ Cannot view strategy code
- ❌ Cannot edit strategy
- ❌ Cannot transfer ownership

**What happens when I deploy:**
- Strategy runs against MY connected providers (Coinbase, Robinhood, etc.)
- Uses MY money/positions
- I configure MY risk settings (stop loss, position size)
- Alice does NOT see my deployment performance (privacy)

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
- 💡 Only visible to: Owner and subscribers (if PUBLISHED + PRIVATE)

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

---

## Subscription Model

### Subscription Tiers

**Current design**: Single tier per owner

- Owner sets their monthly price (e.g., $50/month)
- Subscriber gets access to deploy ALL published strategies from that owner
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
   - User can deploy all of Alice's PUBLISHED strategies
   - Renewed monthly automatically

3. **User cancels:**
   - Subscription continues until end of billing period
   - Then loses deployment access
   - Can still see PUBLIC performance (like any user)

4. **Ownership change:**
   - Alice sells a strategy to Bob
   - Subscribers to Alice CANNOT deploy that strategy anymore
   - No refund, no transfer to Bob's subscriber list

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

    // Published + Public = everyone can view
    if ("PUBLISHED".equals(strategy.getPublishStatus()) &&
        "PUBLIC".equals(strategy.getPublicStatus())) {
        return true;
    }

    // Published + Private = only subscribers can view
    if ("PUBLISHED".equals(strategy.getPublishStatus()) &&
        "PRIVATE".equals(strategy.getPublicStatus())) {
        return hasActiveSubscription(userId, strategy.getOwnerId());
    }

    // Draft = owner only
    return false;
}

public boolean canDeployStrategy(String strategyId, String userId) {
    Strategy strategy = findById(strategyId);

    // Owner can always deploy
    if (userId.equals(strategy.getOwnerId())) {
        return true;
    }

    // Must be published for subscribers to deploy
    if (!"PUBLISHED".equals(strategy.getPublishStatus())) {
        return false;
    }

    // Subscriber can deploy published strategies
    return hasActiveSubscription(userId, strategy.getOwnerId());
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
