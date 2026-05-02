# mqtt-emqx-sdk

A lightweight Java client for the EMQX Management REST API v5.

## Features

- **Authenticators** — create and list password-based auth chains
- **Users** — create, get, list, delete via any authenticator
- **Clients** — list connected clients, disconnect by client ID
- **Sessions** — get and delete client sessions
- **Subscriptions** — list topics a client is subscribed to
- **Nodes** — list cluster nodes and their status
- **Auth** — supports Bearer token (JWT) and Basic Auth (API key)

## Requirements

- Java 21
- Maven 3.x

## Quick Start

### 1. Add dependency

```xml
<dependency>
  <groupId>com.apidech.sdk</groupId>
  <artifactId>mqtt-emqx-sdk</artifactId>
  <version>0.1.0-beta</version>
</dependency>
```

Or grab the JAR from the `target/` directory after building.

### 2. Authenticate

EMQX 5.x CE uses JWT for the Management API. Obtain a token via the login endpoint:

```java
// 1. Get a JWT
HttpClient http = HttpClient.newHttpClient();
var tokenRequest = HttpRequest.newBuilder()
    .uri(URI.create("http://localhost:18083/api/v5/login"))
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString(
        "{\"username\":\"admin\",\"password\":\"public\"}"))
    .build();

var tokenResponse = http.send(tokenRequest,
    HttpResponse.BodyHandlers.ofString());
String jwt = new JSONObject(tokenResponse.body())
    .getString("token");

// 2. Build the client
EmqxSdkClient client = EmqxSdkClient.builder()
    .baseUrl("http://localhost:18083/api/v5")
    .bearerAuth(jwt)
    .build();
```

For permanent API keys, create one at **Dashboard → Settings → API Keys**, then use:

```java
EmqxSdkClient client = EmqxSdkClient.builder()
    .baseUrl("http://localhost:18083/api/v5")
    .basicAuth("your-api-key", "your-api-secret")
    .build();
```

### 3. Use it

```java
// List cluster nodes
client.listNodes()
    .forEach(n -> System.out.println(n.node + " " + n.status));

// Set up built-in DB authenticator (one-time)
client.createBuiltInDbAuthenticator("my-auth", "bcrypt", 10);

// Create a user
UserDto user = UserDto.builder()
    .user_id("alice")
    .password("secret123")
    .build();
client.createUser(AuthenticatorId.DEFAULT, user);

// List all users
client.listUsers(AuthenticatorId.DEFAULT)
    .forEach(u -> System.out.println(u.getUserId()));

// Disconnect a client
client.disconnectClient("client-42");
```

## API Reference

### EmqxSdkClient

#### Authenticator Management
| Method | Description |
|--------|-------------|
| `listAuthenticators()` | List all configured authenticator chains |
| `createBuiltInDbAuthenticator(name, hashAlgo, saltRounds)` | Create a built-in DB authenticator |

#### User Management
| Method | Description |
|--------|-------------|
| `createUser(AuthenticatorId, UserDto)` | Register a new user |
| `getUser(AuthenticatorId, String userId)` | Get a single user |
| `listUsers(AuthenticatorId)` | List all users |
| `deleteUser(AuthenticatorId, String userId)` | Delete a user |

#### Client Management
| Method | Description |
|--------|-------------|
| `listClients()` | List connected MQTT clients |
| `disconnectClient(String clientId)` | Force-disconnect a client |

#### Session Management
| Method | Description |
|--------|-------------|
| `getSession(String clientId)` | Get session details for a client |
| `deleteSession(String clientId)` | Delete a client's session |

#### Subscription Management
| Method | Description |
|--------|-------------|
| `listSubscriptions(String clientId)` | List topics a client is subscribed to |

#### Node Management
| Method | Description |
|--------|-------------|
| `listNodes()` | List all cluster nodes |

## Key Things to Know

- **User field** — use `user_id`, not `username` (EMQX convention)
- **List responses** — EMQX returns `{"data": [...], "meta": {...}}`; this SDK unwraps to `List<T>` automatically
- **ACLs** — the `/acl` REST endpoint doesn't exist in EMQX 5.x CE. ACLs are configured via `authorization/sources` (file-based)
- **No API key creation via REST** — EMQX 5.x CE doesn't expose this; use Dashboard UI to create keys

## Building & Testing

### Build Commands

```bash
mvn compile   # compile
mvn test      # run unit tests (no EMQX needed — uses MockWebServer)
mvn package   # build JAR
mvn install   # install to local Maven repository
```

### Unit Tests

The project includes **23 comprehensive unit tests** using `MockWebServer`:

```bash
mvn test -Dtest=EmqxSdkClientTest
```

✅ **No external dependencies** — tests run in ~2 seconds  
✅ **Full API coverage** — node/user/client/session/subscription management  
✅ **Error handling** — tests HTTP errors, JSON parsing, auth failures  

These tests verify:
- Request methods (GET, POST, DELETE)
- URL paths and query parameters
- Authorization headers
- Response JSON deserialization
- Error code handling

### Integration Tests (Optional)

If you want to test against a real EMQX broker, see [README_INTEGRATION_TESTS.md](README_INTEGRATION_TESTS.md) for setup instructions using Docker.

**Note:** Integration tests require EMQX 4.4.8+ and Docker. The unit tests are recommended for CI/CD pipelines.

## Project Structure

```
src/
├── main/java/com/apidech/sdk/emqxsdk/
│   ├── EmqxSdkClient.java         # Main HTTP client
│   ├── EmqxApiException.java      # Error handling
│   ├── *Dto.java                  # Data transfer objects (20+ classes)
│   └── AuthenticatorId.java       # Authenticator enumeration
│
└── test/java/com/apidech/sdk/emqxsdk/
    ├── EmqxSdkClientTest.java                    # 23 unit tests ✅
    ├── EmqxSdkClientExternalIntegrationTest.java # External broker tests
    ├── EmqxSdkClientIntegrationTest.java         # TestContainers tests
    └── EmqxSdkClientV4IntegrationTest.java       # EMQX 4.4.8 tests

docker-compose.yml                # Local EMQX test environment
run-integration-tests.sh           # Helper script for Docker testing
README_INTEGRATION_TESTS.md        # Integration testing guide
```

## EMQX Version Compatibility

| SDK Version | EMQX | Notes |
|------------|------|-------|
| `0.1.0-beta` | **5.x** | ✅ Recommended |
| `0.1.0-beta` | 4.4.8 | ⚠️ API format differs; unit tests pass |

## Docker Test Environment (Optional)

A `docker-compose.yml` is included for local integration testing with a real EMQX broker:

```bash
# Start EMQX 4.4.8
docker-compose up -d

# Wait for readiness, then run tests
mvn test -Dtest=EmqxSdkClientExternalIntegrationTest

# Stop and clean up
docker-compose down
```

**Or use the helper script:**

```bash
./run-integration-tests.sh up      # Start EMQX + run tests
./run-integration-tests.sh down    # Stop EMQX
./run-integration-tests.sh logs    # View EMQX logs
```

**Access the broker:**
- Dashboard: http://localhost:18083 (admin / public)
- MQTT: `localhost:1883`
- REST API: `http://localhost:18083/api/v4`

**For full details on integration testing**, see [README_INTEGRATION_TESTS.md](README_INTEGRATION_TESTS.md).

## License

Apache License 2.0. See [LICENSE](LICENSE).
