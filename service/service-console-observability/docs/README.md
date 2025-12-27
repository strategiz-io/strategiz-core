# 🔍 Strategiz Monitoring & Observability

Complete monitoring and observability solution for Strategiz, providing **application monitoring**, **infrastructure observability**, and **visualization dashboards**.

## 📁 Directory Structure

```
service-monitoring/
├── src/main/java/                        # 🏗️ APPLICATION MONITORING
│   ├── config/
│   │   ├── ActuatorConfig.java           # Spring Boot Actuator setup & Prometheus metrics
│   │   └── ServiceMonitoringConfig.java  # Base monitoring beans & RestTemplate
│   ├── controller/
│   │   └── HealthCheckController.java    # Custom health endpoints (/health/*)
│   ├── health/
│   │   └── ProviderApiHealthIndicator.java # Custom health indicators
│   └── model/
│       └── StatusResponse.java           # Health response models
├── infrastructure/                        # 🐳 INFRASTRUCTURE OBSERVABILITY
│   ├── docker-compose.observability.yml  # Complete observability stack
│   ├── prometheus/
│   │   └── prometheus.yml                # Prometheus scraping config
│   ├── loki/
│   │   └── promtail-config.yml          # Log aggregation config
│   └── setup.sh                         # Infrastructure setup script
├── dashboards/                           # 📊 VISUALIZATION & DASHBOARDS
│   └── grafana/
│       ├── dashboards/                   # Pre-built Grafana dashboards
│       └── provisioning/                # Grafana auto-provisioning
├── pom.xml                              # Maven dependencies
└── README.md                            # This file
```

## 🚀 Quick Start

### 1. Start Infrastructure Stack
```bash
cd service/service-monitoring/infrastructure
docker-compose -f docker-compose.observability.yml up -d
```

### 2. Create Logs Directory
```bash
mkdir -p logs
```

### 3. Start Strategiz Application
```bash
cd ../../..
./scripts/local/start.sh
```

### 4. Access Dashboards
- **Grafana**: http://localhost:3001 (admin/strategiz123)
- **Prometheus**: http://localhost:9090  
- **Loki**: http://localhost:3100

## 📊 What You Get

### **Application Monitoring** (Java Code)
- ✅ **Actuator Endpoints** - `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`
- ✅ **Custom Health Checks** - Provider API status, database connectivity
- ✅ **Prometheus Metrics** - JVM, HTTP requests, custom business metrics
- ✅ **Application Tags** - Environment, version, deployment info

### **Infrastructure Observability** (Docker Stack)
- ✅ **Metrics Collection** - Prometheus scraping application metrics
- ✅ **Log Aggregation** - Loki + Promtail collecting application logs
- ✅ **Time-Series Storage** - Prometheus for metrics, Loki for logs

### **Visualization & Dashboards** (Grafana)
- ✅ **Application Health** - Service status and uptime
- ✅ **Request Metrics** - RPS, latency, error rates
- ✅ **JVM Metrics** - Memory, GC, threads
- ✅ **Business Metrics** - User registrations, API usage
- ✅ **Log Analysis** - Error logs, request tracing

## 🔧 Configuration

### Application Properties
```properties
# Actuator endpoints (in application.properties)
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=always  
management.metrics.export.prometheus.enabled=true
```

### Health Check Endpoints
- `/actuator/health` - Overall application health
- `/actuator/health/custom` - Custom health checks
- `/health/providers` - Provider API health status

### Prometheus Metrics
- **HTTP Requests**: `http_server_requests_seconds`
- **JVM Memory**: `jvm_memory_used_bytes`
- **Custom Business**: `strategiz_user_registrations_total`

## 🛠️ Development

### Adding Custom Health Checks
```java
@Component
public class MyHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        return Health.up()
            .withDetail("custom", "All good!")
            .build();
    }
}
```

### Adding Custom Metrics
```java
@Component
public class MyMetricsService {
    private final Counter myCounter;
    
    public MyMetricsService(MeterRegistry meterRegistry) {
        this.myCounter = Counter.builder("my_custom_metric")
            .register(meterRegistry);
    }
    
    public void recordEvent() {
        myCounter.increment();
    }
}
```

### Adding Grafana Dashboards
1. Create dashboard in Grafana UI
2. Export JSON to `dashboards/grafana/dashboards/`
3. Restart Grafana to auto-import

## 🎯 Architecture Benefits

- **🎯 Single Source of Truth** - All monitoring in one module
- **🔧 Easy Maintenance** - Co-located configs and code
- **📈 Scalable** - Easy to add new metrics, health checks, dashboards
- **🏗️ Clear Separation** - Application vs Infrastructure vs Visualization
- **🧪 Testable** - Health checks and metrics can be unit tested

## 📝 Troubleshooting

### Check Application Health
```bash
curl http://localhost:8080/actuator/health
```

### Check Prometheus Metrics
```bash
curl http://localhost:8080/actuator/prometheus
```

### View Infrastructure Logs
```bash
docker-compose -f infrastructure/docker-compose.observability.yml logs
```
