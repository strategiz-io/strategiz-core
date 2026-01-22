# LLM Router Enhancement Plan: Multi-Provider Support with Cost Tracking

## Overview

Enhance the existing LLM Router to:
1. Enable direct API integrations (Anthropic, OpenAI) alongside Vertex AI
2. Add real-time cost tracking via provider billing APIs
3. Display comprehensive LLM costs in the Console Operating Costs page

## Current State

### What Already Exists

**Backend (strategiz-core):**
- `LLMProvider` interface in `client-base`
- `LLMRouter` in `business-ai-chat` - central dispatcher
- 9 provider implementations (6 Vertex AI + 3 Direct API)
- `client-anthropic-direct` with Claude 4.5 models (disabled)
- `client-openai-direct` with GPT-4o/o1 models (disabled)
- `client-grok-direct` with Grok models (disabled)

**Console (strategiz-ui/apps/console):**
- `ConsoleCostsScreen.tsx` - Operating Costs page
- `AiProviderBreakdownChart.tsx` - visualizes AI costs by provider
- `costsSlice.ts` - Redux state for costs
- `AiCostsSummary` type with provider breakdown

### What's Missing

1. **Billing API integrations** - Currently no real cost data from providers
2. **Cost aggregation service** - Need to pull from OpenAI, Anthropic, GCP BigQuery
3. **Enhanced console widgets** - Model-level breakdown, daily trends, alerts

---

## Phase 1: Enable Direct Provider Integrations

### 1.1 Enable Anthropic Direct (Claude 4.5)

**Files to modify:**
- `application-api/src/main/resources/application.properties`
- Vault secrets

**Configuration:**
```properties
# Enable Anthropic Direct API
anthropic.direct.enabled=true
anthropic.direct.api-url=https://api.anthropic.com
anthropic.direct.timeout-seconds=60
anthropic.direct.max-tokens=8192
```

**Vault secrets to add:**
```
secret/strategiz/anthropic-direct-api-key
```

**Models enabled:**
- `claude-opus-4-5-20251101`
- `claude-sonnet-4-5-20250514`
- `claude-haiku-4-5-20250514`

### 1.2 Enable OpenAI Direct

**Configuration:**
```properties
# Enable OpenAI Direct API
openai.direct.enabled=true
openai.direct.api-url=https://api.openai.com
openai.direct.timeout-seconds=60
openai.direct.max-tokens=4096
```

**Vault secrets to add:**
```
secret/strategiz/openai-direct-api-key
```

**Models enabled:**
- `gpt-4o`
- `gpt-4o-mini`
- `o1`
- `o1-mini`

### 1.3 Update Frontend Model List

**File:** `strategiz-ui/apps/web/src/services/modelService.ts`

Already updated with Claude 4.5 models. Verify backend returns these in `/v1/learn/chat/models`.

---

## Phase 2: Cost Tracking Service (Backend)

### 2.1 Embed in LLM Router Module: `business-ai-chat`

Embed cost tracking directly in the existing `business-ai-chat` module alongside `LLMRouter`. This keeps everything together for future extraction as a standalone service.

**Location:** `strategiz-core/business/business-ai-chat/`

**Enhanced Structure:**
```
business-ai-chat/
├── pom.xml                              # Add BigQuery dependency
└── src/main/java/io/strategiz/business/aichat/
    ├── LLMRouter.java                   # Existing - central dispatcher
    ├── AIChatBusiness.java              # Existing - chat logic
    │
    │   # NEW: Cost Tracking (embedded for future extraction)
    ├── costs/
    │   ├── LLMCostTrackingService.java  # Main cost service
    │   ├── LLMCostAggregator.java       # Aggregates from all providers
    │   ├── LLMCostSyncScheduler.java    # Daily/hourly sync job
    │   ├── providers/
    │   │   ├── BillingProvider.java     # Interface
    │   │   ├── OpenAIBillingProvider.java
    │   │   ├── AnthropicBillingProvider.java
    │   │   └── GcpBillingProvider.java  # BigQuery for Vertex AI
    │   └── model/
    │       ├── ProviderCostReport.java
    │       ├── ModelCostBreakdown.java
    │       ├── DailyCostEntry.java
    │       └── CostAlert.java
    │
    ├── prompt/                          # Existing prompts
    └── context/                         # Existing context providers
```

**Why embed here:**
1. Single module to extract when productizing
2. Cost tracking naturally couples with routing (same providers)
3. Shared configuration (API keys, endpoints)
4. Easier to expose as unified `/v1/llm/*` API

### 2.2 Cost Provider Interface

```java
public interface CostProvider {
    String getProviderName();

    Mono<ProviderCostReport> fetchCosts(LocalDate startDate, LocalDate endDate);

    Mono<List<ModelCostBreakdown>> fetchCostsByModel(LocalDate startDate, LocalDate endDate);

    boolean isEnabled();
}
```

### 2.3 OpenAI Cost Provider

**API Endpoints:**
```
GET https://api.openai.com/v1/organization/usage/completions
GET https://api.openai.com/v1/organization/costs
Authorization: Bearer $OPENAI_ADMIN_KEY
```

**Implementation:**
```java
@Component
@ConditionalOnProperty(name = "openai.billing.enabled", havingValue = "true")
public class OpenAICostProvider implements CostProvider {

    @Value("${openai.admin.api-key}")
    private String adminApiKey;

    @Override
    public Mono<ProviderCostReport> fetchCosts(LocalDate startDate, LocalDate endDate) {
        // Call OpenAI Usage API
        // Group by model
        // Return aggregated costs
    }
}
```

**Vault secrets:**
```
secret/strategiz/openai-admin-api-key   # Different from regular API key!
```

### 2.4 Anthropic Cost Provider

**API Endpoints:**
```
GET https://api.anthropic.com/v1/organizations/usage_report/messages
GET https://api.anthropic.com/v1/organizations/cost_report
x-api-key: $ANTHROPIC_ADMIN_KEY
```

**Implementation:**
```java
@Component
@ConditionalOnProperty(name = "anthropic.billing.enabled", havingValue = "true")
public class AnthropicCostProvider implements CostProvider {

    @Value("${anthropic.admin.api-key}")
    private String adminApiKey;

    @Override
    public Mono<ProviderCostReport> fetchCosts(LocalDate startDate, LocalDate endDate) {
        // Call Anthropic Admin API
        // Extract cached vs uncached tokens
        // Return costs in USD
    }
}
```

**Vault secrets:**
```
secret/strategiz/anthropic-admin-api-key   # Admin key (sk-ant-admin-...)
```

### 2.5 GCP Billing Provider (Vertex AI)

**Approach:** Query BigQuery billing export

**Prerequisites:**
1. Enable billing export to BigQuery in GCP Console
2. Grant BigQuery read access to service account

**Implementation:**
```java
@Component
@ConditionalOnProperty(name = "gcp.billing.enabled", havingValue = "true")
public class GcpBillingProvider implements CostProvider {

    private final BigQuery bigQuery;

    @Value("${gcp.billing.dataset}")
    private String billingDataset;  // e.g., "project.billing_export"

    @Override
    public Mono<ProviderCostReport> fetchCosts(LocalDate startDate, LocalDate endDate) {
        String query = """
            SELECT
                service.description as service,
                sku.description as model,
                SUM(cost) as total_cost,
                SUM(usage.amount) as total_usage
            FROM `%s`
            WHERE service.description LIKE '%%Vertex AI%%'
              AND usage_start_time >= @startDate
              AND usage_start_time < @endDate
            GROUP BY service, model
            """.formatted(billingDataset);

        // Execute query and map to ProviderCostReport
    }
}
```

### 2.6 Cost Aggregator Service

```java
@Service
public class LLMCostAggregator {

    private final List<CostProvider> providers;
    private final LLMCostRepository repository;  // Firestore

    public Mono<AggregatedCostReport> getAggregatedCosts(LocalDate startDate, LocalDate endDate) {
        return Flux.fromIterable(providers)
            .filter(CostProvider::isEnabled)
            .flatMap(p -> p.fetchCosts(startDate, endDate))
            .collectList()
            .map(this::aggregate);
    }

    public Mono<List<ModelCostBreakdown>> getCostsByModel(LocalDate startDate, LocalDate endDate) {
        // Aggregate costs grouped by model across all providers
    }
}
```

### 2.7 Cost Sync Scheduler

```java
@Component
@EnableScheduling
public class CostSyncScheduler {

    private final LLMCostAggregator aggregator;
    private final LLMCostRepository repository;

    // Sync daily at 2 AM
    @Scheduled(cron = "0 0 2 * * *")
    public void syncDailyCosts() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        aggregator.getAggregatedCosts(yesterday, yesterday)
            .flatMap(repository::save)
            .subscribe();
    }

    // Sync hourly for near-real-time (last 24 hours)
    @Scheduled(cron = "0 0 * * * *")
    public void syncHourlyCosts() {
        // Update current day's costs
    }
}
```

### 2.8 Data Models

```java
@Data
public class ProviderCostReport {
    private String provider;          // "openai", "anthropic", "google"
    private LocalDate date;
    private BigDecimal totalCost;     // USD
    private long totalTokens;
    private long inputTokens;
    private long outputTokens;
    private long cachedTokens;        // Anthropic-specific
    private long requestCount;
    private List<ModelCostBreakdown> byModel;
}

@Data
public class ModelCostBreakdown {
    private String modelId;           // "gpt-4o", "claude-sonnet-4-5"
    private String provider;
    private BigDecimal cost;
    private long inputTokens;
    private long outputTokens;
    private long requestCount;
    private BigDecimal costPerRequest;
    private BigDecimal costPer1kTokens;
}

@Data
public class DailyCostEntry {
    private LocalDate date;
    private BigDecimal totalCost;
    private Map<String, BigDecimal> costByProvider;
    private Map<String, BigDecimal> costByModel;
}
```

### 2.9 REST API Endpoints

**New Controller:** `service-console/LLMCostController.java`

```java
@RestController
@RequestMapping("/v1/console/llm-costs")
public class LLMCostController extends BaseController {

    private final LLMCostAggregator aggregator;

    @GetMapping("/summary")
    public Mono<LLMCostSummaryResponse> getSummary(
            @RequestParam(defaultValue = "30") int days) {
        // Current month summary with provider breakdown
    }

    @GetMapping("/by-model")
    public Mono<List<ModelCostBreakdown>> getCostsByModel(
            @RequestParam @DateTimeFormat(iso = DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DATE) LocalDate endDate) {
        // Cost breakdown by individual model
    }

    @GetMapping("/daily")
    public Mono<List<DailyCostEntry>> getDailyCosts(
            @RequestParam(defaultValue = "30") int days) {
        // Daily cost trend
    }

    @GetMapping("/by-provider/{provider}")
    public Mono<ProviderCostReport> getProviderDetails(
            @PathVariable String provider,
            @RequestParam(defaultValue = "30") int days) {
        // Detailed breakdown for single provider
    }

    @PostMapping("/refresh")
    public Mono<Void> triggerRefresh() {
        // Manual refresh from billing APIs
    }
}
```

---

## Phase 3: Console UI Enhancements

### 3.1 New Components

**Location:** `apps/console/src/features/console/components/llm-costs/`

```
llm-costs/
├── LLMCostSummaryCards.tsx      # Total spend, top model, trend indicator
├── LLMCostByModelChart.tsx      # Bar chart of costs per model
├── LLMCostTrendChart.tsx        # Daily cost trend line chart
├── LLMProviderComparison.tsx    # Side-by-side provider metrics
├── LLMCostTable.tsx             # Detailed table with all models
└── LLMCostAlerts.tsx            # Budget alerts, anomaly warnings
```

### 3.2 LLM Cost Summary Cards

```tsx
interface LLMCostSummaryCardsProps {
  totalCost: number;
  costChange: number;        // vs previous period
  topModel: string;
  topModelCost: number;
  totalRequests: number;
  avgCostPerRequest: number;
}

const LLMCostSummaryCards: React.FC<LLMCostSummaryCardsProps> = (props) => {
  return (
    <Grid container spacing={2}>
      <Grid item xs={12} sm={6} md={3}>
        <MetricCard
          title="LLM Spend (MTD)"
          value={formatCurrency(props.totalCost)}
          change={props.costChange}
          icon={<SmartToyIcon />}
        />
      </Grid>
      {/* More cards... */}
    </Grid>
  );
};
```

### 3.3 Cost by Model Chart

```tsx
// Horizontal bar chart showing cost per model
const LLMCostByModelChart: React.FC<{ data: ModelCostBreakdown[] }> = ({ data }) => {
  const chartData = data
    .sort((a, b) => b.cost - a.cost)
    .slice(0, 10);  // Top 10 models

  return (
    <Card>
      <CardContent>
        <Typography variant="h6">Cost by Model</Typography>
        <ResponsiveContainer height={400}>
          <BarChart data={chartData} layout="vertical">
            <XAxis type="number" tickFormatter={v => `$${v}`} />
            <YAxis type="category" dataKey="modelId" width={150} />
            <Tooltip content={<ModelTooltip />} />
            <Bar dataKey="cost" fill="#4285f4">
              {chartData.map((entry, i) => (
                <Cell key={i} fill={getProviderColor(entry.provider)} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  );
};
```

### 3.4 Redux State Updates

**File:** `apps/console/src/features/console/redux/costsSlice.ts`

Add new state and thunks:

```typescript
interface LLMCostsState {
  summary: LLMCostSummary | null;
  byModel: ModelCostBreakdown[];
  dailyCosts: DailyCostEntry[];
  loading: boolean;
  error: string | null;
}

// New thunks
export const fetchLLMCostSummary = createAsyncThunk(
  'costs/fetchLLMSummary',
  async (days: number = 30) => {
    return await llmCostsApi.getSummary(days);
  }
);

export const fetchLLMCostsByModel = createAsyncThunk(
  'costs/fetchLLMByModel',
  async ({ startDate, endDate }: DateRange) => {
    return await llmCostsApi.getCostsByModel(startDate, endDate);
  }
);
```

### 3.5 Update ConsoleCostsScreen

Add a new section for detailed LLM costs:

```tsx
// In ConsoleCostsScreen.tsx

{/* Existing sections... */}

{/* NEW: Detailed LLM Costs Section */}
<Box sx={{ mt: 4 }}>
  <Typography variant="h5" gutterBottom>
    LLM Provider Costs (Real-Time)
  </Typography>

  <Grid container spacing={3}>
    <Grid item xs={12}>
      <LLMCostSummaryCards data={llmCostSummary} />
    </Grid>

    <Grid item xs={12} md={6}>
      <LLMCostByModelChart data={costsByModel} />
    </Grid>

    <Grid item xs={12} md={6}>
      <LLMCostTrendChart data={dailyLLMCosts} />
    </Grid>

    <Grid item xs={12}>
      <LLMCostTable data={costsByModel} />
    </Grid>
  </Grid>
</Box>
```

### 3.6 API Client Updates

**File:** `apps/console/src/features/console/services/consoleApiClient.ts`

```typescript
export const llmCostsApi = {
  getSummary: async (days: number = 30): Promise<LLMCostSummary> => {
    const response = await consoleApi.get(`/llm-costs/summary?days=${days}`);
    return response.data;
  },

  getCostsByModel: async (startDate: string, endDate: string): Promise<ModelCostBreakdown[]> => {
    const response = await consoleApi.get('/llm-costs/by-model', {
      params: { startDate, endDate }
    });
    return response.data;
  },

  getDailyCosts: async (days: number = 30): Promise<DailyCostEntry[]> => {
    const response = await consoleApi.get(`/llm-costs/daily?days=${days}`);
    return response.data;
  },

  refreshCosts: async (): Promise<void> => {
    await consoleApi.post('/llm-costs/refresh');
  },
};
```

---

## Phase 4: Alerts & Monitoring

### 4.1 Budget Alerts

**Backend:** Add alert thresholds to `AiModelConfig`

```java
@Data
public class LLMBudgetConfig {
    private BigDecimal monthlyBudget;        // e.g., $500
    private BigDecimal dailyAlertThreshold;  // e.g., $50
    private BigDecimal anomalyMultiplier;    // e.g., 2.0 (alert if 2x normal)
    private List<String> alertEmails;
}
```

**Alert triggers:**
- Daily spend exceeds threshold
- Monthly spend reaches 80%, 90%, 100% of budget
- Anomaly detection (sudden spike vs 7-day average)

### 4.2 Console Alert Widget

```tsx
const LLMCostAlerts: React.FC<{ alerts: CostAlert[] }> = ({ alerts }) => {
  return (
    <Card>
      <CardContent>
        <Typography variant="h6">Cost Alerts</Typography>
        {alerts.map(alert => (
          <Alert severity={alert.severity} sx={{ mt: 1 }}>
            <AlertTitle>{alert.title}</AlertTitle>
            {alert.message}
          </Alert>
        ))}
      </CardContent>
    </Card>
  );
};
```

---

## Implementation Order

### Sprint 1: Enable Direct Providers (1-2 days)
- [ ] Add Anthropic API key to Vault
- [ ] Enable `anthropic.direct.enabled=true`
- [ ] Add OpenAI API key to Vault
- [ ] Enable `openai.direct.enabled=true`
- [ ] Verify models appear in frontend dropdown
- [ ] Test chat with Claude 4.5 and GPT-4o

### Sprint 2: Cost Tracking Backend (3-4 days)
- [ ] Create `business-llm-costs` module
- [ ] Implement `CostProvider` interface
- [ ] Implement `OpenAICostProvider`
- [ ] Implement `AnthropicCostProvider`
- [ ] Implement `GcpBillingProvider` (BigQuery)
- [ ] Create `LLMCostAggregator` service
- [ ] Add REST endpoints to `service-console`
- [ ] Add cost sync scheduler

### Sprint 3: Console UI (2-3 days)
- [ ] Create `LLMCostSummaryCards` component
- [ ] Create `LLMCostByModelChart` component
- [ ] Create `LLMCostTrendChart` component
- [ ] Create `LLMCostTable` component
- [ ] Update `costsSlice.ts` with new thunks
- [ ] Update `ConsoleCostsScreen.tsx` with new section
- [ ] Add API client methods

### Sprint 4: Alerts & Polish (1-2 days)
- [ ] Implement budget configuration
- [ ] Add alert generation logic
- [ ] Create `LLMCostAlerts` component
- [ ] Add email notifications (optional)
- [ ] Testing and bug fixes

---

## Configuration Summary

### Vault Secrets Required

**Sprint 1 - Direct API Access (LLM requests):**
```bash
# Add these secrets to Vault for direct LLM API access
# Vault path format: secret/strategiz/{key-name}

# Anthropic (Claude 4.5 models)
vault kv put secret/strategiz anthropic.api-key="sk-ant-api03-YOUR_KEY_HERE"

# OpenAI (GPT-4o, o1 models)
vault kv put secret/strategiz openai.api-key="sk-proj-YOUR_KEY_HERE"

# xAI (Grok models)
vault kv put secret/strategiz grok.api-key="xai-YOUR_KEY_HERE"
```

**Where to get API keys:**
- Anthropic: https://console.anthropic.com/settings/keys
- OpenAI: https://platform.openai.com/api-keys
- xAI: https://console.x.ai/

**Sprint 2 - Billing API Access (cost tracking):**
```bash
# Admin API Keys for billing/usage APIs (different from regular API keys!)

# Anthropic Admin Key (for Usage & Cost API)
vault kv put secret/strategiz anthropic.admin-api-key="sk-ant-admin-YOUR_ADMIN_KEY"

# OpenAI Admin Key (for Usage API)
vault kv put secret/strategiz openai.admin-api-key="sk-admin-YOUR_ADMIN_KEY"
```

**Where to get Admin keys:**
- Anthropic: https://console.anthropic.com/settings/admin-api (requires admin role)
- OpenAI: https://platform.openai.com/settings/organization/admin-keys

### Application Properties

```properties
# Direct Provider Enablement
anthropic.direct.enabled=true
openai.direct.enabled=true

# Billing API Enablement
anthropic.billing.enabled=true
openai.billing.enabled=true
gcp.billing.enabled=true
gcp.billing.dataset=strategiz-prod.billing_export.gcp_billing_export_v1

# Cost Tracking
llm.costs.sync.enabled=true
llm.costs.cache.ttl-minutes=60
llm.costs.budget.monthly=500
llm.costs.budget.daily-alert=50
```

---

## Future Enhancements

1. **Cost Optimization Recommendations**
   - Suggest switching to cheaper models for simple tasks
   - Identify prompt caching opportunities

2. **Per-User Cost Attribution**
   - Track which users/features consume most tokens
   - Enable cost chargebacks

3. **Additional Providers**
   - Mistral Direct API
   - Cohere Direct API
   - Local models (Ollama)

---

## Phase 5: Extract as Standalone Service (Future)

When ready to productize, extract `business-ai-chat` as a separate microservice.

### 5.1 New Repository: `strategiz-router`

```
strategiz-router/
├── src/main/java/io/strategiz/router/
│   ├── LLMRouterApplication.java        # Spring Boot entry point
│   ├── api/
│   │   ├── ChatController.java          # POST /v1/chat/completions
│   │   ├── ModelsController.java        # GET /v1/models
│   │   └── CostsController.java         # GET /v1/costs/*
│   ├── routing/
│   │   ├── LLMRouter.java               # From business-ai-chat
│   │   └── providers/                   # All LLMProvider implementations
│   ├── costs/
│   │   └── ...                          # Cost tracking (from business-ai-chat)
│   ├── billing/
│   │   ├── UsageTracker.java            # Per-API-key usage
│   │   ├── QuotaEnforcer.java           # Rate limits
│   │   └── BillingService.java          # Stripe integration
│   └── config/
│       └── MultiTenantConfig.java       # API key → tenant mapping
├── Dockerfile
└── cloudbuild.yaml                      # Deploy to Cloud Run
```

### 5.2 API Design (OpenAI-Compatible)

```yaml
# OpenAI-compatible endpoints for drop-in replacement
POST /v1/chat/completions
  - Standard chat completions
  - Model routing based on request

GET /v1/models
  - List available models
  - Filtered by API key permissions

# Custom endpoints
GET /v1/usage
  - Token usage for API key
  - Cost breakdown

GET /v1/costs/summary
  - Aggregated costs (admin only)

POST /v1/admin/keys
  - Create API keys (admin only)
```

### 5.3 Multi-Tenant Architecture

```
┌─────────────────────────────────────────────────┐
│              Strategiz Router API               │
│         POST /v1/chat/completions               │
├─────────────────────────────────────────────────┤
│  API Key Authentication                         │
│  ├── sk-strat-xxx → Tenant A (quota: 1M/mo)    │
│  ├── sk-strat-yyy → Tenant B (quota: 500K/mo)  │
│  └── sk-strat-zzz → Internal (unlimited)       │
├─────────────────────────────────────────────────┤
│  Usage Tracking (per key, per model)            │
│  Rate Limiting (per key)                        │
│  Cost Attribution (per tenant)                  │
├──────────┬──────────┬──────────┬───────────────┤
│ Vertex AI│ Anthropic│  OpenAI  │  Future...    │
└──────────┴──────────┴──────────┴───────────────┘
```

### 5.4 Monetization Options

1. **Usage-Based Pricing**
   - Pass-through provider costs + margin (e.g., 10%)
   - Prepaid credits model (like OpenRouter)

2. **Tiered Plans**
   - Free: 100K tokens/month
   - Pro: 1M tokens/month @ $29/mo
   - Enterprise: Custom quotas, SLA

3. **Enterprise Features**
   - Private deployments
   - Custom model fine-tuning
   - Dedicated support

### 5.5 Extraction Checklist

When ready to extract:

- [ ] Create new `strategiz-router` repository
- [ ] Copy `business-ai-chat` code
- [ ] Copy all `client-*` provider modules
- [ ] Add API key authentication layer
- [ ] Add multi-tenant support
- [ ] Add Stripe billing integration
- [ ] Create separate Cloud Run deployment
- [ ] Set up separate domain (e.g., `api.strategiz.io`)
- [ ] Update strategiz-core to call router service (or keep embedded)
- [ ] Build marketing site / docs

---

## Architecture Summary

```
Current (Embedded):
┌─────────────────────────────────────────────────┐
│              strategiz-core                     │
│  ┌─────────────────────────────────────────┐   │
│  │         business-ai-chat                 │   │
│  │  ┌─────────────┐  ┌─────────────────┐   │   │
│  │  │  LLMRouter  │  │  costs/         │   │   │
│  │  │  (routing)  │  │  (tracking)     │   │   │
│  │  └─────────────┘  └─────────────────┘   │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  ┌─────────────────────────────────────────┐   │
│  │  client-* (providers)                    │   │
│  │  gemini-vertex, claude-vertex, etc.     │   │
│  └─────────────────────────────────────────┘   │
└─────────────────────────────────────────────────┘
         ↓ (future extraction)
┌─────────────────────────────────────────────────┐
│           strategiz-router (standalone)         │
│  Same code, deployed separately                 │
│  + Multi-tenant + Billing + Public API          │
└─────────────────────────────────────────────────┘
```
