# Authorization Standard

**Version**: 1.0
**Last Updated**: December 31, 2025
**Status**: MANDATORY for all service modules

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Required Annotations](#required-annotations)
4. [Authentication Levels (ACR)](#authentication-levels-acr)
5. [Service Implementation Checklist](#service-implementation-checklist)
6. [Code Examples](#code-examples)
7. [Common Patterns](#common-patterns)
8. [Resource Access Levels](#resource-access-levels)
9. [Testing Requirements](#testing-requirements)
10. [Security Best Practices](#security-best-practices)
11. [Troubleshooting](#troubleshooting)

---

## Overview

Strategiz uses a **three-layer authorization architecture** to ensure secure access to all API endpoints:

1. **Layer 0**: Token Generation (`business-token-auth`) - Creates PASETO tokens after authentication
2. **Layer 1**: Endpoint Protection (`framework-authorization`) - Validates tokens and enforces access control
3. **Layer 2**: Fine-Grained Authorization (`framework-authorization` + OpenFGA) - Relationship-based permissions

### Core Principle

**Every service endpoint MUST be explicitly authorized.** There are no public endpoints by default.

---

## Architecture

### System Components

```
┌─────────────────────────────────────────────────────────────────┐
│                    Authorization Flow                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. User authenticates (SMS, TOTP, Passkey, OAuth, etc.)       │
│     ↓                                                            │
│  2. business-token-auth generates PASETO token with ACR level   │
│     ↓                                                            │
│  3. Token stored in HTTP-only cookie (strategiz-access-token)   │
│     ↓                                                            │
│  4. Client sends request with cookie or Authorization header    │
│     ↓                                                            │
│  5. framework-authorization intercepts via Spring AOP           │
│     ├─ Validates PASETO token signature                         │
│     ├─ Checks @RequireAuth ACR level                            │
│     ├─ Checks @RequireScope (if present)                        │
│     └─ Checks @Authorize via OpenFGA (if present)               │
│     ↓                                                            │
│  6. SecurityContext populated with AuthenticatedUser            │
│     ↓                                                            │
│  7. Controller method executes with @AuthUser injection         │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Module Responsibilities

| Module | Responsibility | Used When |
|--------|---------------|-----------|
| `framework-authorization` | Endpoint protection with annotations | Every API request |
| `business-token-auth` | Token generation/validation | Login, token refresh |
| `data-auth` | Store auth credentials | Signup, MFA enrollment |

---

## Required Annotations

### 1. `@RequireAuth` (MANDATORY)

**Purpose**: Protect endpoints and enforce minimum authentication level

**Scope**: Class-level or method-level

**Parameters**:
- `minAcr` (String): Minimum ACR level required (default: "1")
- `allowDemoMode` (boolean): Allow demo users (default: true)

**Examples**:

```java
// Basic authentication (any logged-in user)
@RequireAuth
public ResponseEntity<?> getProfile() { ... }

// Require MFA
@RequireAuth(minAcr = "2")
public ResponseEntity<?> transferFunds() { ... }

// Require strong MFA, no demo users
@RequireAuth(minAcr = "3", allowDemoMode = false)
public ResponseEntity<?> executeLiveTrade() { ... }
```

**Class-level usage** (applies to all methods):

```java
@RestController
@RequestMapping("/v1/portfolios")
@RequireAuth(minAcr = "1")  // ← All endpoints require auth
public class PortfolioController extends BaseController {

    // Inherits @RequireAuth(minAcr = "1")
    @GetMapping
    public ResponseEntity<?> list() { ... }

    // Override with stricter requirement
    @DeleteMapping("/{id}")
    @RequireAuth(minAcr = "2")
    public ResponseEntity<?> delete(@PathVariable String id) { ... }
}
```

---

### 2. `@AuthUser` (RECOMMENDED)

**Purpose**: Inject authenticated user into controller method parameter

**Scope**: Method parameter

**Parameters**:
- `required` (boolean): Require authentication (default: true)

**Examples**:

```java
// Required authentication (throws 401 if missing)
@GetMapping("/profile")
@RequireAuth
public ResponseEntity<?> getProfile(@AuthUser AuthenticatedUser user) {
    String userId = user.getUserId();
    String email = user.getEmail();
    String acr = user.getAcr();  // Authentication level
    // ...
}

// Optional authentication (null if not logged in)
@GetMapping("/public")
public ResponseEntity<?> publicEndpoint(
    @AuthUser(required = false) AuthenticatedUser user
) {
    if (user != null) {
        // Personalized response for logged-in users
    }
    // ...
}
```

**Benefits**:
- ✅ No manual token extraction
- ✅ Type-safe user access
- ✅ Consistent across all controllers
- ✅ Automatically validated by framework

---

### 3. `@Authorize` (OPTIONAL - For Fine-Grained Access)

**Purpose**: Check user-resource relationships via OpenFGA

**Scope**: Method-level only

**Parameters**:
- `relation` (String): Required relationship (e.g., "owner", "viewer")
- `resource` (ResourceType): Resource type enum
- `resourceId` (String): SpEL expression to extract resource ID

**Examples**:

```java
// Check if user owns the strategy
@DeleteMapping("/strategies/{id}")
@RequireAuth(minAcr = "1")
@Authorize(
    relation = "owner",
    resource = ResourceType.STRATEGY,
    resourceId = "#id"
)
public ResponseEntity<?> deleteStrategy(@PathVariable String id) { ... }

// Check if user can view portfolio (owner, editor, or viewer)
@GetMapping("/portfolios/{portfolioId}")
@RequireAuth
@Authorize(
    relation = "viewer",
    resource = ResourceType.PORTFOLIO,
    resourceId = "#portfolioId"
)
public ResponseEntity<?> viewPortfolio(@PathVariable String portfolioId) { ... }

// Extract from request body
@PostMapping("/strategies/deploy")
@RequireAuth(minAcr = "2")
@Authorize(
    relation = "owner",
    resource = ResourceType.STRATEGY,
    resourceId = "#request.strategyId"
)
public ResponseEntity<?> deployStrategy(@RequestBody DeployRequest request) { ... }
```

**When to use**:
- ✅ Multi-user resources (strategies, portfolios, teams)
- ✅ Hierarchical permissions (owner > editor > viewer)
- ❌ User's own data (just check `user.getUserId()` instead)
- ❌ Public endpoints

---

### 4. `@RequireScope` (OPTIONAL - For OAuth/API Keys)

**Purpose**: Require specific scopes in PASETO token

**Scope**: Method-level

**Parameters**:
- `value` (String[]): Required scopes
- `mode` (ScopeMode): AND or OR logic (default: AND)

**Examples**:

```java
// Require "trades:write" scope
@PostMapping("/trades")
@RequireAuth
@RequireScope("trades:write")
public ResponseEntity<?> executeTrade() { ... }

// Require ANY of these scopes
@GetMapping("/data")
@RequireAuth
@RequireScope(value = {"data:read", "admin"}, mode = ScopeMode.OR)
public ResponseEntity<?> getData() { ... }
```

**When to use**:
- ✅ API key authentication
- ✅ OAuth client restrictions
- ✅ Service-to-service auth
- ❌ Regular user authentication (use ACR instead)

---

## Authentication Levels (ACR)

ACR (Authentication Context Reference) levels define the **strength** of authentication.

### ACR Level Definitions

| Level | Name | Requirements | Use Cases |
|-------|------|-------------|-----------|
| **0** | Partial/Signup | In-progress signup, unverified | Signup flow, email verification |
| **1** | Single-Factor | Email/password, SMS, Email OTP | Basic operations, viewing data |
| **2** | Multi-Factor | Any two factors (SMS + password, TOTP + password) | Money transfers, trading, settings changes |
| **3** | Strong MFA | Hardware key (Passkey, YubiKey) + another factor | Live trading, large transfers, admin actions |

### Setting ACR Levels

#### During Authentication

**Token generation** (`business-token-auth`):

```java
// Single-factor auth (email/password)
String acr = "1";

// Multi-factor auth (password + TOTP)
String acr = "2";

// Passkey authentication (hardware-backed)
String acr = "3";

// Generate token with ACR
PasetoToken token = pasetoTokenProvider.generateToken(userId, acr, scopes);
```

#### Enforcing in Controllers

```java
// Allow any authenticated user (ACR >= 1)
@RequireAuth(minAcr = "1")

// Require MFA (ACR >= 2)
@RequireAuth(minAcr = "2")

// Require strong MFA (ACR >= 3)
@RequireAuth(minAcr = "3")
```

---

## Service Implementation Checklist

When creating a **new service module**, follow this checklist:

### 1. ✅ Add Dependencies

**File**: `service/service-yourmodule/pom.xml`

```xml
<dependencies>
    <!-- Service Base -->
    <dependency>
        <groupId>io.strategiz</groupId>
        <artifactId>service-framework-base</artifactId>
        <version>${project.version}</version>
    </dependency>

    <!-- REQUIRED: Authorization Framework -->
    <dependency>
        <groupId>io.strategiz</groupId>
        <artifactId>framework-authorization</artifactId>
        <version>${project.version}</version>
    </dependency>

    <!-- REQUIRED: Exception Framework -->
    <dependency>
        <groupId>io.strategiz</groupId>
        <artifactId>framework-exception</artifactId>
        <version>${project.version}</version>
    </dependency>
</dependencies>
```

### 2. ✅ Extend BaseController

**File**: `service-yourmodule/controller/YourController.java`

```java
@RestController
@RequestMapping("/v1/yourmodule")
@RequireAuth(minAcr = "1")  // ← MANDATORY
public class YourController extends BaseController {

    @Override
    protected String getModuleName() {
        return "service-yourmodule";
    }

    // ... your endpoints
}
```

### 3. ✅ Annotate All Endpoints

```java
// ✅ CORRECT
@GetMapping("/items")
@RequireAuth
public ResponseEntity<?> list(@AuthUser AuthenticatedUser user) {
    // Authenticated user available
}

// ❌ WRONG - No authorization
@GetMapping("/items")
public ResponseEntity<?> list() {
    // SECURITY VIOLATION - Anyone can access!
}
```

### 4. ✅ Use @AuthUser for User Context

```java
// ✅ CORRECT - Type-safe, validated
@PostMapping("/items")
@RequireAuth
public ResponseEntity<?> create(
    @RequestBody CreateRequest request,
    @AuthUser AuthenticatedUser user
) {
    String userId = user.getUserId();
    item.setOwnerId(userId);
    // ...
}

// ❌ WRONG - Manual token extraction
@PostMapping("/items")
public ResponseEntity<?> create(@RequestHeader("Authorization") String authHeader) {
    // Don't do this! Use @AuthUser instead
}
```

### 5. ✅ Set Appropriate ACR Levels

```java
// ✅ CORRECT - ACR based on sensitivity
@GetMapping("/balance")
@RequireAuth(minAcr = "1")  // Viewing is low-risk
public ResponseEntity<?> getBalance() { ... }

@PostMapping("/withdraw")
@RequireAuth(minAcr = "2")  // Money movement requires MFA
public ResponseEntity<?> withdraw() { ... }

@PostMapping("/live-trade")
@RequireAuth(minAcr = "3", allowDemoMode = false)  // Live trading requires strong auth
public ResponseEntity<?> executeLiveTrade() { ... }
```

---

## Code Examples

### Example 1: Basic CRUD Controller

```java
package io.strategiz.service.tasks.controller;

import io.strategiz.framework.authorization.annotation.RequireAuth;
import io.strategiz.framework.authorization.annotation.AuthUser;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.service.base.controller.BaseController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for task management.
 *
 * All endpoints require authentication (ACR >= 1).
 */
@RestController
@RequestMapping("/v1/tasks")
@RequireAuth(minAcr = "1")  // ← Applied to all endpoints
public class TaskController extends BaseController {

    @Override
    protected String getModuleName() {
        return "service-tasks";
    }

    private final TaskService taskService;

    @Autowired
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * List all tasks for the authenticated user.
     * Inherits @RequireAuth(minAcr = "1") from class level.
     */
    @GetMapping
    public ResponseEntity<?> list(@AuthUser AuthenticatedUser user) {
        List<Task> tasks = taskService.findByUserId(user.getUserId());
        return ResponseEntity.ok(tasks);
    }

    /**
     * Create a new task.
     */
    @PostMapping
    public ResponseEntity<?> create(
        @RequestBody CreateTaskRequest request,
        @AuthUser AuthenticatedUser user
    ) {
        Task task = taskService.create(request, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }

    /**
     * Update a task.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
        @PathVariable String id,
        @RequestBody UpdateTaskRequest request,
        @AuthUser AuthenticatedUser user
    ) {
        Task task = taskService.update(id, request, user.getUserId());
        return ResponseEntity.ok(task);
    }

    /**
     * Delete a task.
     * Requires MFA due to destructive nature.
     */
    @DeleteMapping("/{id}")
    @RequireAuth(minAcr = "2")  // ← Override with stricter requirement
    public ResponseEntity<?> delete(
        @PathVariable String id,
        @AuthUser AuthenticatedUser user
    ) {
        taskService.delete(id, user.getUserId());
        return ResponseEntity.noContent().build();
    }
}
```

### Example 2: Fine-Grained Authorization with OpenFGA

```java
package io.strategiz.service.strategies.controller;

import io.strategiz.framework.authorization.annotation.*;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.framework.authorization.model.ResourceType;
import io.strategiz.service.base.controller.BaseController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for strategy management with fine-grained access control.
 */
@RestController
@RequestMapping("/v1/strategies")
@RequireAuth(minAcr = "1")
public class StrategyController extends BaseController {

    @Override
    protected String getModuleName() {
        return "service-strategies";
    }

    /**
     * View strategy details.
     * Requires "viewer" relation (includes owner, editor, viewer).
     */
    @GetMapping("/{id}")
    @Authorize(
        relation = "viewer",
        resource = ResourceType.STRATEGY,
        resourceId = "#id"
    )
    public ResponseEntity<?> getStrategy(@PathVariable String id) {
        // User has been verified to have "viewer" access
        Strategy strategy = strategyService.findById(id);
        return ResponseEntity.ok(strategy);
    }

    /**
     * Edit strategy.
     * Requires "editor" relation (includes owner, editor).
     */
    @PutMapping("/{id}")
    @Authorize(
        relation = "editor",
        resource = ResourceType.STRATEGY,
        resourceId = "#id"
    )
    public ResponseEntity<?> updateStrategy(
        @PathVariable String id,
        @RequestBody UpdateStrategyRequest request
    ) {
        Strategy strategy = strategyService.update(id, request);
        return ResponseEntity.ok(strategy);
    }

    /**
     * Delete strategy.
     * Requires "owner" relation (owner only).
     */
    @DeleteMapping("/{id}")
    @RequireAuth(minAcr = "2")  // Also require MFA
    @Authorize(
        relation = "owner",
        resource = ResourceType.STRATEGY,
        resourceId = "#id"
    )
    public ResponseEntity<?> deleteStrategy(@PathVariable String id) {
        strategyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Transfer ownership.
     * Requires "owner" relation.
     */
    @PostMapping("/{strategyId}/transfer")
    @RequireAuth(minAcr = "2")
    @Authorize(
        relation = "owner",
        resource = ResourceType.STRATEGY,
        resourceId = "#strategyId"
    )
    public ResponseEntity<?> transferOwnership(
        @PathVariable String strategyId,
        @RequestBody TransferRequest request,
        @AuthUser AuthenticatedUser user
    ) {
        strategyService.transferOwnership(
            strategyId,
            user.getUserId(),
            request.getNewOwnerId()
        );
        return ResponseEntity.ok().build();
    }
}
```

### Example 3: Mixed Public and Protected Endpoints

```java
package io.strategiz.service.marketplace.controller;

/**
 * Marketplace controller with mixed access levels.
 */
@RestController
@RequestMapping("/v1/marketplace")
public class MarketplaceController extends BaseController {

    @Override
    protected String getModuleName() {
        return "service-marketplace";
    }

    /**
     * Browse marketplace (public, but personalized if logged in).
     */
    @GetMapping("/browse")
    public ResponseEntity<?> browse(
        @AuthUser(required = false) AuthenticatedUser user  // ← Optional
    ) {
        if (user != null) {
            // Personalized results based on user preferences
            return ResponseEntity.ok(marketplaceService.getPersonalized(user.getUserId()));
        }
        // Public results
        return ResponseEntity.ok(marketplaceService.getPublic());
    }

    /**
     * Purchase strategy (requires authentication).
     */
    @PostMapping("/purchase")
    @RequireAuth(minAcr = "2")  // ← Require MFA for purchases
    public ResponseEntity<?> purchase(
        @RequestBody PurchaseRequest request,
        @AuthUser AuthenticatedUser user
    ) {
        Purchase purchase = marketplaceService.purchase(request, user.getUserId());
        return ResponseEntity.ok(purchase);
    }
}
```

---

## Common Patterns

### Pattern 1: User-Owned Resources

**Scenario**: Resource belongs to a single user (watchlist, preferences, profile)

**Implementation**: Check `userId` in service layer

```java
@PutMapping("/preferences")
@RequireAuth
public ResponseEntity<?> updatePreferences(
    @RequestBody PreferencesRequest request,
    @AuthUser AuthenticatedUser user
) {
    // No @Authorize needed - just verify ownership
    preferencesService.update(user.getUserId(), request);
    return ResponseEntity.ok().build();
}
```

**Service Layer**:

```java
public void update(String userId, PreferencesRequest request) {
    // Find by userId - ensures user can only modify their own data
    Preferences prefs = repository.findByUserId(userId)
        .orElseThrow(() -> new StrategizException(NOT_FOUND));

    prefs.update(request);
    repository.save(prefs);
}
```

---

### Pattern 2: Shared Resources (OpenFGA)

**Scenario**: Resource can be accessed by multiple users with different permissions

**Implementation**: Use `@Authorize` annotation

```java
@GetMapping("/teams/{teamId}/members")
@RequireAuth
@Authorize(
    relation = "member",  // User must be a team member
    resource = ResourceType.TEAM,
    resourceId = "#teamId"
)
public ResponseEntity<?> listMembers(@PathVariable String teamId) {
    List<Member> members = teamService.getMembers(teamId);
    return ResponseEntity.ok(members);
}
```

**OpenFGA Relationship Setup** (done separately):

```java
// When user joins team
fgaClient.writeTuple(new TupleKey(
    "user:" + userId,      // subject
    "member",              // relation
    "team:" + teamId       // object
));
```

---

### Pattern 3: Admin-Only Endpoints

**Scenario**: Endpoint should only be accessible by administrators

**Implementation**: Custom check in service layer or use scope

```java
@DeleteMapping("/admin/users/{userId}")
@RequireAuth(minAcr = "2")
@RequireScope("admin")  // ← Require admin scope
public ResponseEntity<?> deleteUser(@PathVariable String userId) {
    adminService.deleteUser(userId);
    return ResponseEntity.noContent().build();
}
```

**Alternative** (role check in service):

```java
@DeleteMapping("/admin/users/{userId}")
@RequireAuth(minAcr = "2")
public ResponseEntity<?> deleteUser(
    @PathVariable String userId,
    @AuthUser AuthenticatedUser user
) {
    if (!user.hasRole("ADMIN")) {
        throw new StrategizException(FORBIDDEN, "Admin access required");
    }
    adminService.deleteUser(userId);
    return ResponseEntity.noContent().build();
}
```

---

### Pattern 4: Public Read, Protected Write

**Scenario**: Anyone can view, but only authenticated users can modify

```java
@RestController
@RequestMapping("/v1/blog")
public class BlogController extends BaseController {

    // Public read
    @GetMapping("/posts")
    public ResponseEntity<?> listPosts() {
        return ResponseEntity.ok(blogService.getPublicPosts());
    }

    // Protected write
    @PostMapping("/posts")
    @RequireAuth(minAcr = "1")
    public ResponseEntity<?> createPost(
        @RequestBody PostRequest request,
        @AuthUser AuthenticatedUser user
    ) {
        Post post = blogService.create(request, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(post);
    }
}
```

---

## Resource Access Levels

Strategiz implements a **four-level access control model** for resources (strategies, marketplace content, etc.):

### Access Level Hierarchy

```
┌──────────────────────────────────────────────────────────────────┐
│                    Resource Access Levels                         │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Level 1: OWNER (Full Access)                                    │
│  ─────────────────────────────                                   │
│  • View code (with watermark)                                     │
│  • Edit strategy                                                  │
│  • Transfer ownership                                             │
│  • Deploy as bot/alert                                            │
│  • View full performance metrics                                  │
│  • Set pricing and publish settings                               │
│                                                                   │
│  Level 2: SUBSCRIBER (Deploy Access)                              │
│  ──────────────────────────────────                              │
│  • Deploy strategy as bot/alert (NO code access)                 │
│  • View owner's deployments                                       │
│  • View full performance metrics                                  │
│  • Subscribe to user, not individual strategies                   │
│                                                                   │
│  Level 3: FOLLOWER (Activity & Performance)                       │
│  ───────────────────────────────────────                         │
│  • View activity feed (publications, updates)                     │
│  • View performance metrics of published strategies               │
│  • View strategy metadata (name, description, tags)               │
│  • CANNOT view code                                               │
│  • CANNOT deploy strategies                                       │
│  • CANNOT see private/draft strategies                            │
│                                                                   │
│  Level 4: PUBLIC (Performance Only)                               │
│  ───────────────────────────────────                             │
│  • View performance of publicly published strategies              │
│  • View strategy metadata                                         │
│  • CANNOT view code                                               │
│  • CANNOT deploy                                                  │
│  • CANNOT see private/draft strategies                            │
│  • CANNOT see activity feed                                       │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### Implementation with OpenFGA

Use the `@Authorize` annotation to enforce relationship-based access:

```java
@RestController
@RequestMapping("/v1/strategies")
@RequireAuth(minAcr = "1")
public class StrategyController extends BaseController {

    // Owner only - view code
    @GetMapping("/{id}/code")
    @Authorize(relation = "owner", resource = ResourceType.STRATEGY, resourceId = "#id")
    public ResponseEntity<?> getStrategyCode(
        @PathVariable String id,
        @AuthUser AuthenticatedUser user
    ) {
        String code = strategyAccessService.getCode(id, user.getUserId());
        return ResponseEntity.ok(Map.of("code", code));
    }

    // Subscriber OR owner - deploy strategy
    @PostMapping("/{id}/deploy")
    public ResponseEntity<?> deployStrategy(
        @PathVariable String id,
        @AuthUser AuthenticatedUser user
    ) {
        // Check if user is owner OR has active subscription to owner
        if (!strategyAccessService.canDeploy(id, user.getUserId())) {
            throw new StrategizException(
                StrategyErrorDetails.UNAUTHORIZED_DEPLOY,
                "service-strategy"
            );
        }
        Deployment deployment = deploymentService.deploy(id, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(deployment);
    }

    // Follower OR subscriber OR owner OR public - view performance
    @GetMapping("/{id}/performance")
    public ResponseEntity<?> getPerformance(
        @PathVariable String id,
        @AuthUser AuthenticatedUser user
    ) {
        // Check access level
        if (!strategyAccessService.canViewPerformance(id, user.getUserId())) {
            throw new StrategizException(
                StrategyErrorDetails.UNAUTHORIZED_VIEW,
                "service-strategy"
            );
        }
        Performance performance = performanceService.getMetrics(id);
        return ResponseEntity.ok(performance);
    }

    // Follower OR owner - view activity
    @GetMapping("/users/{userId}/activity")
    public ResponseEntity<?> getActivityFeed(
        @PathVariable String userId,
        @AuthUser AuthenticatedUser user
    ) {
        if (!strategyAccessService.canViewActivity(userId, user.getUserId())) {
            throw new StrategizException(
                StrategyErrorDetails.UNAUTHORIZED_VIEW_ACTIVITY,
                "service-strategy"
            );
        }
        List<Activity> activity = activityService.getFeed(userId);
        return ResponseEntity.ok(activity);
    }
}
```

### StrategyAccessService

Implement access control business logic in a dedicated service:

```java
@Service
public class StrategyAccessService extends BaseService {

    @Autowired
    private UserFollowRepository followRepository;

    @Autowired
    private UserSubscriptionRepository subscriptionRepository;

    @Autowired
    private StrategyRepository strategyRepository;

    /**
     * Check if user can view strategy code (owner only).
     */
    public boolean canViewCode(String strategyId, String userId) {
        Strategy strategy = strategyRepository.findById(strategyId)
            .orElseThrow(() -> new StrategizException(
                StrategyErrorDetails.NOT_FOUND, MODULE_NAME
            ));
        return strategy.getOwnerId().equals(userId);
    }

    /**
     * Check if user can deploy strategy (owner OR subscriber to owner).
     */
    public boolean canDeploy(String strategyId, String userId) {
        Strategy strategy = strategyRepository.findById(strategyId)
            .orElseThrow(() -> new StrategizException(
                StrategyErrorDetails.NOT_FOUND, MODULE_NAME
            ));

        // Owner can always deploy
        if (strategy.getOwnerId().equals(userId)) {
            return true;
        }

        // Check if user has active subscription to owner
        return hasActiveUserSubscription(strategy.getOwnerId(), userId);
    }

    /**
     * Check if user can view performance (owner, subscriber, follower, or public).
     */
    public boolean canViewPerformance(String strategyId, String userId) {
        Strategy strategy = strategyRepository.findById(strategyId)
            .orElseThrow(() -> new StrategizException(
                StrategyErrorDetails.NOT_FOUND, MODULE_NAME
            ));

        // Owner can always view
        if (strategy.getOwnerId().equals(userId)) {
            return true;
        }

        // Subscriber to owner can view
        if (hasActiveUserSubscription(strategy.getOwnerId(), userId)) {
            return true;
        }

        // Follower of owner can view published strategies
        if (isFollowing(strategy.getOwnerId(), userId) &&
            strategy.getPublishStatus().equals("PUBLISHED")) {
            return true;
        }

        // Public can view publicly published strategies
        if (strategy.getPublicStatus().equals("PUBLIC") &&
            strategy.getPublishStatus().equals("PUBLISHED")) {
            return true;
        }

        return false;
    }

    /**
     * Check if user can view activity (follower OR owner).
     */
    public boolean canViewActivity(String targetUserId, String currentUserId) {
        // Own activity always visible
        if (targetUserId.equals(currentUserId)) {
            return true;
        }

        // Follower can view activity
        return isFollowing(targetUserId, currentUserId);
    }

    // Helper methods

    private boolean hasActiveUserSubscription(String ownerId, String subscriberId) {
        return subscriptionRepository.findActiveSubscription(subscriberId, ownerId)
            .isPresent();
    }

    private boolean isFollowing(String followedId, String followerId) {
        return followRepository.isFollowing(followerId, followedId);
    }
}
```

### Follower-Specific Rules

**What Followers CAN Access**:
- ✅ Activity feed showing strategy publications and updates
- ✅ Full performance metrics of **published** strategies
- ✅ Strategy metadata (name, description, tags, author)
- ✅ Follow/unfollow the user

**What Followers CANNOT Access**:
- ❌ Strategy source code
- ❌ Deploy strategies as bots or alerts
- ❌ Private or draft strategies
- ❌ Edit strategy settings
- ❌ Transfer ownership

**Implementation Notes**:
- Following is a FREE social feature
- No subscription required to follow
- Followers stored in `user_follows` collection with compound ID
- Following grants read-only access to published content only
- Upgrade to user subscription for deployment access

---

## Testing Requirements

### Unit Tests

**Test authorization in controllers**:

```java
@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testList_WithoutAuth_Returns401() throws Exception {
        mockMvc.perform(get("/v1/tasks"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void testList_WithAuth_ReturnsOk() throws Exception {
        String token = generateValidToken("user123", "1");

        mockMvc.perform(get("/v1/tasks")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    void testDelete_WithoutMFA_Returns403() throws Exception {
        String token = generateValidToken("user123", "1");  // ACR 1

        mockMvc.perform(delete("/v1/tasks/task123")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    @Test
    void testDelete_WithMFA_ReturnsNoContent() throws Exception {
        String token = generateValidToken("user123", "2");  // ACR 2

        mockMvc.perform(delete("/v1/tasks/task123")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());
    }
}
```

### Integration Tests

**Test end-to-end authorization flow**:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthorizationIntegrationTest {

    @Test
    void testFullAuthFlow() {
        // 1. User signs up
        SignupResponse signup = authService.signup(email, password);

        // 2. User logs in with MFA
        LoginResponse login = authService.loginWithMFA(email, password, totpCode);
        assertEquals("2", login.getAcr());  // Verify ACR level

        // 3. Make authenticated request
        Response response = given()
            .cookie("strategiz-access-token", login.getToken())
            .when()
            .get("/v1/tasks")
            .then()
            .statusCode(200)
            .extract().response();
    }
}
```

---

## Security Best Practices

### ✅ DO

1. **Always use `@RequireAuth`** on all non-public endpoints
2. **Use `@AuthUser`** instead of manual token extraction
3. **Set appropriate ACR levels** based on operation sensitivity
4. **Use OpenFGA** for shared resources with complex permissions
5. **Validate ownership** in service layer for user-owned resources
6. **Use HTTP-only cookies** for web clients
7. **Support both cookie and Authorization header** for flexibility
8. **Test authorization** in unit and integration tests
9. **Document ACR requirements** in API documentation
10. **Use demo mode flag** for production-only operations

### ❌ DON'T

1. **Don't skip authorization** - every endpoint must have explicit protection
2. **Don't trust client-provided user IDs** - always use `@AuthUser`
3. **Don't use ACR 0** for normal operations - it's for signup flow only
4. **Don't store tokens in localStorage** - use HTTP-only cookies
5. **Don't bypass authorization** in tests - test the real flow
6. **Don't use string literals** for ACR levels - use constants if needed
7. **Don't implement custom auth** - use the framework
8. **Don't mix authorization and business logic** - keep them separate
9. **Don't log tokens** - they contain sensitive information
10. **Don't assume authentication = authorization** - verify permissions

---

## Troubleshooting

### Issue: 401 Unauthorized

**Symptoms**: All requests return 401 even with valid credentials

**Causes**:
1. Token not being sent (check cookies or Authorization header)
2. Token expired
3. Invalid token signature
4. Missing `@RequireAuth` annotation

**Debug**:
```java
// Enable debug logging
logging.level.io.strategiz.framework.authorization=DEBUG
```

**Check**:
- Cookie name is `strategiz-access-token`
- Cookie domain is set correctly (`.strategiz.io` for production)
- Authorization header format: `Bearer <token>`

---

### Issue: 403 Forbidden

**Symptoms**: Request is authenticated but rejected with 403

**Causes**:
1. Insufficient ACR level (e.g., endpoint requires ACR 2 but user has ACR 1)
2. Missing required scope
3. OpenFGA authorization failed (no relationship)
4. Demo mode user accessing production-only endpoint

**Debug**:
```java
@GetMapping("/debug/user")
@RequireAuth
public ResponseEntity<?> debugUser(@AuthUser AuthenticatedUser user) {
    return ResponseEntity.ok(Map.of(
        "userId", user.getUserId(),
        "acr", user.getAcr(),
        "scopes", user.getScopes()
    ));
}
```

---

### Issue: @AuthUser is null

**Symptoms**: `@AuthUser` parameter is null in controller

**Causes**:
1. `@AuthUser(required = false)` is set
2. Authorization aspect not configured
3. Missing `framework-authorization` dependency

**Solution**:
1. Verify dependency in `pom.xml`
2. Check Spring Boot auto-configuration is enabled
3. Ensure `@RequireAuth` is present

---

### Issue: OpenFGA Check Failing

**Symptoms**: `@Authorize` annotation rejects valid users

**Causes**:
1. Relationship not created in OpenFGA
2. Wrong relation name (case-sensitive)
3. Wrong resource type
4. SpEL expression error

**Debug**:
```java
// Manually check relationship
boolean hasAccess = fgaClient.check(
    "user:" + userId,
    "owner",
    "strategy:" + strategyId
);
```

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-12-31 | Initial release - comprehensive authorization standard |

---

## References

- [framework-authorization README](../../framework/framework-authorization/README.md)
- [Security Overview](./overview.md)
- [Secrets Management](./secrets-management.md)
- [OpenFGA Documentation](https://openfga.dev/docs)
- [PASETO Specification](https://paseto.io/)

---

## Approval

This standard is **MANDATORY** for all service modules in the Strategiz platform.

**Approved by**: Engineering Team
**Effective Date**: December 31, 2025
**Review Cycle**: Quarterly
