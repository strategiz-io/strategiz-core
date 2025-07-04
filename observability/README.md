# 🔍 Strategiz Observability Stack

Complete observability solution with **logs, metrics, and dashboards** for the Strategiz application.

## 🚀 Quick Start

```bash
# 1. Start the observability stack
docker-compose -f docker-compose.observability.yml up -d

# 2. Create logs directory
mkdir -p logs

# 3. Access the dashboards
# Grafana: http://localhost:3001 (admin/strategiz123)
# Prometheus: http://localhost:9090
# Loki: http://localhost:3100
```

## 📊 What You Get

### **Grafana Dashboard** (http://localhost:3001)
- **Application Health** - Service status and uptime
- **Request Rate** - RPS and traffic patterns  
- **Error Rate** - Error percentage and trends
- **Response Time** - P95 latency metrics
- **Error Logs** - Real-time error log streaming
- **Authentication Events** - Passkey, OAuth, signup/signin tracking
- **API Performance** - Endpoint-by-endpoint performance

### **Log Search** (Loki in Grafana)
- **Structured Search**: `{job="strategiz-app"} |= "ERROR"`
- **Field Extraction**: `{job="strategiz-app"} | json | requestId="abc123"`
- **Level Filtering**: `{job="strategiz-app", level="ERROR"}`
- **User Tracking**: `{job="strategiz-app"} | json | userId="usr_123"`

### **Metrics** (Prometheus)
- **HTTP Metrics**: Request rate, duration, status codes
- **JVM Metrics**: Memory, GC, threads
- **Custom Metrics**: Business KPIs, user counts
- **Actuator Endpoints**: Health, info, metrics

## 🛠️ Integration with Your App

### **Backend (Spring Boot)**
Your app already has the metrics endpoint enabled in `application.properties`:
```properties
management.endpoints.web.exposure.include=health,info,prometheus,metrics
management.endpoint.prometheus.enabled=true
```

### **Frontend Logging**
Install the frontend logger:
```bash
cd strategiz-ui
# We'll create a custom logger utility
```

## 📈 Production Recommendations

### **For Small Scale (< 1M requests/day)**
- ✅ Current setup (Grafana + Loki + Prometheus)
- ✅ Single machine deployment
- ✅ File-based storage

### **For Medium Scale (1M-10M requests/day)**
```yaml
# Add to docker-compose
alertmanager:
  image: prom/alertmanager:latest
  # Configure Slack/email alerts

node-exporter:
  image: prom/node-exporter:latest
  # System metrics
```

### **For Large Scale (10M+ requests/day)**
Consider managed solutions:
- **Grafana Cloud** (free tier available)
- **Datadog** (paid, excellent UX)
- **New Relic** (paid, great APM)
- **AWS CloudWatch + X-Ray**

## 🚨 Alerting Rules

Create `observability/prometheus/rules/alerts.yml`:
```yaml
groups:
  - name: strategiz-alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m]) > 0.05
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          
      - alert: HighResponseTime
        expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Response time is too high"
```

## 🔧 Troubleshooting

### **No Metrics Showing**
```bash
# Check if Spring Boot actuator is accessible
curl http://localhost:8080/actuator/prometheus

# Check Prometheus targets
curl http://localhost:9090/api/v1/targets
```

### **No Logs Showing**
```bash
# Check if log files exist
ls -la logs/

# Check Promtail logs
docker logs strategiz-promtail
```

### **Grafana Login Issues**
- **Username**: admin
- **Password**: strategiz123
- **URL**: http://localhost:3001

## 📝 Log Formats

### **Backend JSON Logs**
```json
{
  "timestamp": "2024-06-29T19:30:00.123Z",
  "level": "INFO",
  "logger": "io.strategiz.service.auth.PasskeyService",
  "message": "User authentication successful",
  "mdc": {
    "requestId": "req_123",
    "userId": "usr_456",
    "requestPath": "/auth/passkey/authenticate",
    "requestMethod": "POST"
  }
}
```

### **Frontend Logs**
```json
{
  "timestamp": "2024-06-29T19:30:00.123Z",
  "level": "info",
  "message": "Passkey registration completed",
  "context": {
    "component": "PasskeyService",
    "userId": "usr_456",
    "sessionId": "sess_789"
  },
  "url": "http://localhost:3000/signup"
}
```

## 🎯 Key Queries

### **Find Authentication Issues**
```logql
{job="strategiz-app"} |~ "passkey|oauth" | json | level="ERROR"
```

### **Track User Journey**
```logql
{job="strategiz-app"} | json | userId="usr_123" | line_format "{{.timestamp}} [{{.level}}] {{.message}}"
```

### **Monitor API Performance**
```promql
histogram_quantile(0.95, 
  rate(http_server_requests_seconds_bucket{uri!~"/actuator.*"}[5m])
) by (uri)
```

## 🔒 Security Best Practices

- ✅ **No sensitive data in logs** (passwords, tokens, PII)
- ✅ **Log sampling** for high-volume endpoints  
- ✅ **Access controls** on dashboards
- ✅ **Log retention policies** (30-90 days)
- ✅ **Encrypted log shipping** for production

## 📚 Next Steps

1. **Enhanced Logging** - Add structured JSON logging
2. **Frontend Logger** - Create clean logging utility
3. **Distributed Tracing** - Add Jaeger for request tracing
4. **Custom Metrics** - Business KPIs and user analytics
5. **Alerting** - Slack/email notifications for issues

---

**🎉 You now have enterprise-grade observability in 15 minutes!** 