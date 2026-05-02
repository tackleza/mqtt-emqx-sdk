# Integration Tests Setup

This project includes both **unit tests** (fast, with mocks) and **integration tests** (real MQTT broker).

## Unit Tests (Fast - No Docker Needed)

Run the existing unit tests with mock HTTP server:

```bash
mvn test -Dtest=EmqxSdkClientTest
```

All tests pass because they mock HTTP responses using `MockWebServer`. This tests:
- HTTP request/response handling
- JSON serialization/deserialization
- Error handling
- API contract validation

**Duration:** ~2 seconds | **No dependencies required**

---

## Integration Tests (Real EMQX Broker)

### Option 1: Using docker-compose (Recommended)

**Step 1: Start EMQX**
```bash
docker-compose up -d
```

This will:
- Start EMQX on port 1883 (MQTT) and 18083 (API)
- Wait for it to be ready
- Default credentials: `admin:public`

**Step 2: Run integration tests**
```bash
mvn test -Dtest=EmqxSdkClientIntegrationTest
```

**Step 3: Stop EMQX when done**
```bash
docker-compose down
```

### Option 2: Using TestContainers (CI/CD - Requires Docker Daemon)

If you have Docker available, you can also run:
```bash
mvn test -Dtest=EmqxSdkClientIntegrationTest
```

TestContainers will automatically spin up and tear down the EMQX container.

### Option 3: Connect to External EMQX

Set environment variables before running tests:
```bash
export EMQX_HOST=your-emqx-host
export EMQX_PORT=18083
export EMQX_USER=admin
export EMQX_PASSWORD=public

mvn test -Dtest=EmqxSdkClientIntegrationTestExternal
```

---

## What the Integration Tests Cover

✅ **Real broker interaction** - Actual API calls to EMQX  
✅ **User lifecycle** - Create, read, delete users  
✅ **Node management** - List nodes and metrics  
✅ **Client management** - List connected clients  
✅ **Session management** - Get/delete sessions  
✅ **Error scenarios** - Real HTTP error responses  

---

## EMQX Dashboard

Once running, access the EMQX dashboard:
- **URL:** http://localhost:18083
- **Username:** admin
- **Password:** public

Monitor live:
- Connected clients
- Message throughput
- Node status
- User management

---

## Troubleshooting

**"Could not find a valid Docker environment"**
- Docker daemon not running: `docker ps` should work
- Use docker-compose instead (Option 1)

**"Connection refused" on port 1883 or 18083**
- Check if EMQX is running: `docker ps | grep emqx`
- Wait 10-15 seconds for EMQX to fully start
- Check logs: `docker logs mqtt-emqx-test`

**Tests pass but create calls fail**
- EMQX may have different user auth backends configured
- Check authenticators: `curl -u admin:public http://localhost:18083/api/v5/authentication`

---

## Test Files

- `EmqxSdkClientTest.java` - Unit tests with mocks (23 tests)
- `EmqxSdkClientIntegrationTest.java` - Integration tests with real broker
- `docker-compose.yml` - EMQX broker container config
