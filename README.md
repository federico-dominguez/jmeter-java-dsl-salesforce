# jmeter-java-dsl-salesforce

JMeter Java DSL demo for performance testing on Salesforce environments.

> 🗺️ **[View Development Roadmap](ROADMAP.md)** - Strategic plan for portfolio enhancements

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Salesforce credentials configured (see Environment Variables section)
- BlazeMeter account and API token (for cloud execution)

## Project Structure

- `src/test/java/com/fedd/salesforce/tests/` - Test plan definitions
  - `LeadToCashTestPlan.java` - Local execution test plan
  - `LeadToCashBlazeMeterTestPlan.java` - BlazeMeter cloud execution test plan
- `src/test/java/com/fedd/salesforce/scenarios/` - Thread group scenarios (auth, lead-to-cash, cleanup)
- `src/test/java/com/fedd/salesforce/services/` - Service layer for Salesforce API interactions
- `src/main/resources/data/leads_data.csv` - Test data for lead creation

## Environment Variables

Set the following environment variables before running tests:

```powershell
# Salesforce credentials (required for all runs)
$env:SALESFORCE_USERNAME = "your-salesforce-username"          # Your Salesforce user email
$env:SALESFORCE_CLIENT_ID = "your-connected-app-client-id"     # Connected App Consumer Key
$env:SALESFORCE_PRIVATE_KEY = "your-base64-encoded-private-key" # Base64-encoded private key (no PEM headers)
$env:AUDIENCE = "https://login.salesforce.com"                  # Optional: defaults to login.salesforce.com (use test.salesforce.com for sandbox)

# BlazeMeter API token (required only for cloud execution)
$env:BZ_TOKEN = "your-blazemeter-api-token:api-secret"          # Format: id:secret
```

**Important for BlazeMeter:**
- All Salesforce environment variables are automatically passed as JMeter properties to BlazeMeter
- The JWT generator will read from properties in the cloud environment
- Ensure your private key is properly Base64-encoded (use `[Convert]::ToBase64String([IO.File]::ReadAllBytes("path\to\key.pem"))` in PowerShell)

## Running Tests

### Local Execution

Run tests locally using JMeter engine:

```powershell
mvn test
```

This executes the `test()` method in `PerformanceTest.java`, which:
- Uses the local test plan (`LeadToCashTestPlan`)
- Writes results to `target/jtls/`
- Asserts zero errors

### BlazeMeter Cloud Execution

Run tests on BlazeMeter's cloud infrastructure:

1. **Set environment variables in BlazeMeter UI:**
   - Go to your test in BlazeMeter
   - Navigate to **Configuration** tab
   - Scroll to **User Defined Variables** section
   - Add the following variables:
     - `SALESFORCE_USERNAME`: Your Salesforce user email
     - `SALESFORCE_CLIENT_ID`: Connected App Consumer Key
     - `SALESFORCE_PRIVATE_KEY`: Base64-encoded private key (no PEM headers/footers)
     - `AUDIENCE`: `https://login.salesforce.com` (or `https://test.salesforce.com` for sandbox)

2. Set your BlazeMeter API token locally:
```powershell
$env:BZ_TOKEN = "your-blazemeter-api-token"
```

3. Run the BlazeMeter test:
```powershell
mvn exec:java -Dexec.mainClass="com.fedd.salesforce.PerformanceTest" -Dexec.classpathScope=test
```

Or run the specific test method:
```powershell
mvn -Dtest=PerformanceTest#blazeMeterTest test
```

This:
- Uploads `leads_data.csv` as an asset to BlazeMeter
- Uses `LeadToCashBlazeMeterTestPlan` (references asset filename)
- Runs 1 user with 1-minute ramp-up and hold
- Asserts 99th percentile response time < 5 seconds
- JWT generator reads credentials from BlazeMeter environment variables

### Results

- **Local runs**: Check `target/jtls/*.jtl` for JMeter result files
- **BlazeMeter runs**: View detailed reports in the BlazeMeter web UI

## Advanced Load Testing Scenarios

The project includes four specialized test scenarios designed to validate different performance characteristics within Salesforce Developer Edition constraints.

### Salesforce Developer Edition Limits

All scenarios are designed to respect Salesforce Developer Edition API limits:
- **Concurrent requests**: 5-25 (typically ~10-15 before throttling)
- **Daily API calls**: 5,000-15,000
- **Data storage**: 5MB
- **References**: [Salesforce API Limits](https://developer.salesforce.com/docs/atlas.en-us.salesforce_app_limits_cheatsheet.meta/salesforce_app_limits_cheatsheet/salesforce_app_limits_platform_api.htm) | [Concurrent Request Limits](https://help.salesforce.com/s/articleView?id=000385436&type=1)

### Test Scenarios

#### 1. Stress Test - Breaking Point Analysis

**Purpose**: Find the actual concurrent request limit before Salesforce throttling occurs.

**Load Pattern**:
- 15 users, 20 iterations each
- Immediate start (no ramp-up)

**Expected Behavior**:
- Response times increase as concurrency grows
- HTTP 503 errors or `UNABLE_TO_LOCK_ROW` at ~10-15 concurrent users
- Identifies stable concurrent user capacity

**Success Criteria**:
- Document exact user count where p95 > 5 seconds
- Note when error rate > 0%
- Determine safe concurrent capacity

**Run**:
```powershell
mvn test -Dtest=StressTest
```

**Duration**: ~15 minutes | **API Calls**: ~300-450

---

#### 2. Spike Test - Traffic Surge Validation

**Purpose**: Test system behavior during sudden traffic spikes and recovery.

**Load Pattern**:
- **Phase 1 (Baseline)**: 2 users, 10 iterations each
- **Phase 2 (Spike)**: 10 users, 5 iterations each (sudden surge)
- **Phase 3 (Recovery)**: 2 users, 10 iterations each

**Expected Behavior**:
- Baseline phase shows normal response times
- Spike phase triggers rate limiting (503 errors or slower response)
- Recovery phase returns to normal within 1-2 minutes

**Success Criteria**:
- Error rate during spike < 20%
- Recovery time < 2 minutes (baseline response time restored)
- No errors during recovery phase

**Run**:
```powershell
mvn test -Dtest=SpikeTest
```

**Duration**: ~5-7 minutes | **API Calls**: ~150-200

**Real-World Scenario**: Simulates end-of-quarter sales rush when team activity suddenly increases.

---

#### 3. Soak Test - Long-Duration Stability

**Purpose**: Detect memory leaks, resource exhaustion, and performance degradation over time.

**Load Pattern**:
- 5 users sustained for 1 hour
- Moderate, consistent load

**Expected Behavior**:
- Response times remain stable throughout
- No gradual increase (would indicate memory leak)
- Error rate stays at 0%
- Total API calls < 2,000 (within daily limit)

**Success Criteria**:
- p95 at 55 minutes == p95 at 5 minutes (±10%)
- 0% error rate throughout
- No `UNABLE_TO_LOCK_ROW` errors
- Total API calls < 2,000

**Run**:
```powershell
mvn test -Dtest=SoakTest
```

**Duration**: ~1 hour 5 minutes | **API Calls**: ~1,500

**What It Tests**: Memory leaks, session management, connection pooling, API daily limit accumulation

---

#### 4. Volume Test - Large Dataset Performance

**Purpose**: Validate query performance and data processing with larger record counts.

**Load Pattern**:
- 5 users processing 100 unique lead records
- Each lead goes through full Lead-to-Cash workflow
- Uses `volume_leads_data.csv` (100 unique records)

**Expected Behavior**:
- Response times scale linearly with dataset size
- SOQL queries handle larger result sets efficiently
- Storage stays under 1MB (well under 5MB limit)

**Success Criteria**:
- p95 with 100 records ≤ p95 with 10 records (±15%)
- 0% error rate (no storage limit errors)
- All 100 unique leads created successfully
- No data conflicts or `UNABLE_TO_LOCK_ROW` errors

**Run**:
```powershell
mvn test -Dtest=VolumeTest
```

**Duration**: ~7-10 minutes | **API Calls**: ~500

**Comparison**: Compare results against baseline test (10 records) to measure scaling impact.

---

### Monitoring with Grafana

All test scenarios send real-time metrics to InfluxDB and display in Grafana dashboards:

1. **Start monitoring stack**:
```powershell
docker-compose up -d
```

2. **Access dashboard**: http://localhost:3000 (admin/admin)

3. **Metrics tracked**:
   - Response time percentiles (p50, p95, p99)
   - Throughput (requests/second)
   - Error rate
   - Per-transaction performance
   - Time-series trends

4. **Dashboard features**:
   - Real-time updates during test execution
   - Historical data retention
   - Transaction filtering (excludes setup/teardown)
   - SLA threshold alerts

For detailed monitoring setup, see [MONITORING.md](MONITORING.md).

## Build Commands

```powershell
# Clean and compile
mvn clean compile test-compile

# Run tests
mvn test

# Skip tests during build
mvn clean install -DskipTests
```

## Notes

- The project uses JMeter Java DSL 2.1 for programmatic test definition
- BlazeMeter integration automatically detects `BZ_TOKEN` environment variable
- If `BZ_TOKEN` is not set, the BlazeMeter test will skip gracefully
- Test data CSV must be present at `src/main/resources/data/leads_data.csv`
