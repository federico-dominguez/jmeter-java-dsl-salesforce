# 📊 Performance Monitoring with InfluxDB + Grafana

This guide shows you how to set up real-time performance monitoring for your JMeter tests.

## ⚙️ Prerequisites

**Required:**
- Docker Desktop installed
- **CPU Virtualization enabled in BIOS** (Intel VT-x or AMD-V)
  - Windows feature "Hyper-V" must be enabled
  - To check: `systeminfo | findstr /C:"Virtualization"` should show "Yes"
  
**If virtualization is not available:**
- Use BlazeMeter cloud testing (already integrated in this project)
- BlazeMeter provides built-in performance dashboards
- This local monitoring stack is optional for local development

## 🎯 What You Get

- **Real-time Dashboards**: Watch your test metrics live
- **Historical Trends**: Track performance over time
- **Key Metrics**:
  - Response time percentiles (p50, p95, p99)
  - Throughput (requests/second)
  - Error rate and error count
  - Active users and load profile

## 🚀 Quick Start (5 minutes)

### 1. Start Monitoring Stack

```powershell
# Start InfluxDB + Grafana
docker-compose up -d

# Verify services are running
docker-compose ps
```

Expected output:
```
NAME                COMMAND             STATUS              PORTS
jmeter-influxdb     ...                 Up 5 seconds        0.0.0.0:8086->8086/tcp
jmeter-grafana      ...                 Up 5 seconds        0.0.0.0:3000->3000/tcp
```

### 2. Run Your Performance Test

```powershell
# Compile and run test
mvn clean test

# Or run specific test
mvn -Dtest=PerformanceTest#test test
```

### 3. View Real-Time Dashboard

Open Grafana: http://localhost:3000

**Login credentials:**
- Username: `admin`
- Password: `admin`

The dashboard "**JMeter Performance Test - Salesforce Lead to Cash**" will automatically load with your test data.

## 📈 Dashboard Panels

### Response Time Percentiles
Shows p50, p95, and p99 response times over time. Helps identify performance degradation.

**SLA Targets:**
- p50: < 500ms ✅
- p95: < 2000ms ✅
- p99: < 5000ms ✅

### Throughput
Requests per second. Shows load handling capacity.

**Target:** 10+ TPS

### Error Rate
Percentage of failed requests. Should stay below 1%.

**Target:** < 1% (99%+ success rate)

### Errors Per Second
Absolute number of errors. Any spike indicates issues.

**Target:** 0 errors

## 🔧 Configuration

### InfluxDB Connection

The test plans are configured to send metrics to:
- **URL:** `http://localhost:8086`
- **Organization:** `jmeter`
- **Bucket:** `jmeter`
- **Token:** `jmeter-admin-token-please-change-in-production`

**⚠️ For production:** Change the token in both:
1. `docker-compose.yml` (DOCKER_INFLUXDB_INIT_ADMIN_TOKEN)
2. Test plan code (`influxDbListener().token()`)

### Customizing the Dashboard

1. Open Grafana: http://localhost:3000
2. Go to Dashboards → JMeter Performance Test
3. Click ⚙️ (Settings) → Save As → New name
4. Edit panels, add new metrics, customize thresholds

**Export your dashboard:**
```
Dashboard → Share → Export → Save to file
```

Copy to `grafana/dashboards/` folder to persist.

## 📊 Viewing Historical Data

InfluxDB stores all test results. To query historical data:

### Option 1: Grafana Time Range
- Click time range in top-right (e.g., "Last 5 minutes")
- Select custom range (e.g., "Last 30 days")

### Option 2: InfluxDB UI
1. Open http://localhost:8086
2. Login: `admin` / `adminpassword`
3. Go to Data Explorer
4. Query your test metrics

### Example Query:
```flux
from(bucket: "jmeter")
  |> range(start: -7d)
  |> filter(fn: (r) => r["_measurement"] == "jmeter")
  |> filter(fn: (r) => r["_field"] == "pct95.0")
  |> mean()
```

## 🛑 Stopping Monitoring

```powershell
# Stop services (keeps data)
docker-compose stop

# Stop and remove containers (keeps data in volumes)
docker-compose down

# Remove everything including data
docker-compose down -v
```

## 🐛 Troubleshooting

### Dashboard shows "No Data"

**Cause:** Test hasn't run yet or InfluxDB connection failed.

**Solution:**
1. Check InfluxDB is running: `docker-compose ps`
2. Run a test: `mvn test`
3. Refresh Grafana dashboard

### Connection Refused Error

**Cause:** InfluxDB not accessible from JMeter.

**Solution:**
```powershell
# Check InfluxDB logs
docker-compose logs influxdb

# Restart InfluxDB
docker-compose restart influxdb
```

### High Memory Usage

**Cause:** InfluxDB stores all metrics in memory before flushing to disk.

**Solution:**
```powershell
# Increase Docker memory limit
# Docker Desktop → Settings → Resources → Memory: 4GB+
```

## 🎯 Best Practices

### 1. Baseline Your Tests
Run the same test 3 times to establish a baseline:
```powershell
mvn test  # Run 1
mvn test  # Run 2
mvn test  # Run 3
```
Average the p95 response time = your baseline.

### 2. Set Up Alerts (Advanced)
Configure Grafana alerts to notify you when:
- p95 > 2000ms
- Error rate > 1%
- Throughput drops > 20%

### 3. Compare Test Runs
Use Grafana variables to compare:
- Before/after code changes
- Different load levels
- Different environments

### 4. Clean Old Data
```powershell
# Delete data older than 30 days (inside InfluxDB container)
docker exec jmeter-influxdb influx delete \
  --bucket jmeter \
  --start 1970-01-01T00:00:00Z \
  --stop $(date -d '30 days ago' -Iseconds)
```

## 📚 Next Steps

- [ ] Run baseline tests and document results
- [ ] Set up Grafana alerts for SLA violations
- [ ] Create custom dashboards for specific transactions
- [ ] Export dashboards for CI/CD pipeline
- [ ] Integrate with BlazeMeter for cloud tests

## 🔗 References

- [InfluxDB Documentation](https://docs.influxdata.com/influxdb/v2.7/)
- [Grafana Documentation](https://grafana.com/docs/)
- [JMeter Java DSL - InfluxDB](https://abstracta.github.io/jmeter-java-dsl/guide/#influx-db-listener)
