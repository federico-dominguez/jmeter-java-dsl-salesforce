# jmeter-java-dsl-salesforce

JMeter Java DSL demo for performance testing on Salesforce environments.

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
$env:SALESFORCE_USERNAME = "your-salesforce-username"
$env:SALESFORCE_CLIENT_ID = "your-connected-app-client-id"
$env:SALESFORCE_PRIVATE_KEY = "your-base64-encoded-private-key"

# BlazeMeter API token (required only for cloud execution)
$env:BZ_TOKEN = "your-blazemeter-api-token"
```

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

1. Set your BlazeMeter API token:
```powershell
$env:BZ_TOKEN = "your-blazemeter-api-token"
```

2. Run the BlazeMeter test:
```powershell
mvn exec:java -Dexec.mainClass="com.fedd.salesforce.PerformanceTest"
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

### Results

- **Local runs**: Check `target/jtls/*.jtl` for JMeter result files
- **BlazeMeter runs**: View detailed reports in the BlazeMeter web UI

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
