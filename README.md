# mqtt-emqx-sdk

A lightweight Java client for EMQX Management REST API v5. Provides synchronous
methods for managing authenticators, users, clients, sessions, subscriptions, and
cluster nodes.

**⚠️ Beta & No Warranty**

> This SDK is in beta. APIs may change. Use at your own risk.

## Features

* **Authenticator Management** — create and list password-based authenticators
* **User Management** — create, retrieve, list, and delete users via built‑in or
  custom authenticators
* **Client Management** — list connected clients and disconnect by client ID
* **Session Management** — retrieve and delete client sessions
* **Subscription Management** — list subscriptions per client
* **Node Management** — list cluster nodes
* **Authentication** — supports HTTP Basic (API key/secret) and Bearer token (JWT)

## Requirements

* Java 21 or higher
* Maven 3.x
* OkHttp 4.9.3 + Gson 2.9.0 (included as transitive deps)

## Usage Examples

### JWT Auth (recommended for EMQX 5.x CE)

```java
import com.apidech.sdk.emqxsdk.EmqxSdkClient;
import com.apidech.sdk.emqxsdk.UserDto;

EmqxSdkClient client = EmqxSdkClient.builder()
    .baseUrl("http://localhost:18083/api/v5")
    .bearerAuth(jwtToken)   // obtain via POST /api/v5/login
    .build();

// List nodes
client.listNodes().forEach(n -> System.out.println(n.node + " " + n.version));

// Create authenticator (one-time setup)
client.createBuiltInDbAuthenticator("my-auth", "bcrypt", 10);

// Create user
UserDto user = UserDto.builder()
    .user_id("alice")
    .password("secret")
    .build();
client.createUser(AuthenticatorId.DEFAULT, user);

// List users (returns paginated {data:[], meta:{}} wrapper)
client.listUsers(AuthenticatorId.DEFAULT)
    .forEach(u -> System.out.println(u.getUserId()));
```

### Basic Auth (API key)

```java
EmqxSdkClient client = EmqxSdkClient.builder()
    .baseUrl("http://localhost:18083/api/v5")
    .basicAuth("your-api-key", "your-api-secret")
    .build();
```

> **Note:** EMQX 5.x CE does not expose API key creation via REST API.
> Create permanent API keys via **Dashboard → Settings → API Keys**.
> For testing, use JWT auth instead.

## EMQX Version Compatibility

| SDK Version | EMQX Version | Notes |
|------------|--------------|-------|
| 0.0.x      | **5.x**      | Current — v5 API (recommended) |
| 0.0.x      | 4.x          | ⚠️ Not tested; `/acl` endpoint removed in v5 |

> The `/acl` REST API (EMQX 4.x) does **not exist** in EMQX 5.x CE.
> ACLs are managed via `authorization/sources` (file-based) in v5.

## API Reference

### EmqxSdkClient Methods

* **Authenticator Management**
  * `List<AuthenticatorDto> listAuthenticators()`
  * `AuthenticatorDto createBuiltInDbAuthenticator(String name, String hashAlgo, int saltRounds)`

* **User Management** — use `AuthenticatorId.DEFAULT` for built-in DB
  * `UserDto createUser(AuthenticatorId, UserDto)`
  * `UserDto getUser(AuthenticatorId, String userId)`
  * `List<UserDto> listUsers(AuthenticatorId)`
  * `void deleteUser(AuthenticatorId, String userId)`

* **Client Management**
  * `List<ClientDto> listClients()`
  * `void disconnectClient(String clientId)`

* **Session Management**
  * `SessionDto getSession(String clientId)`
  * `void deleteSession(String clientId)`

* **Subscription Management**
  * `List<SubscriptionDto> listSubscriptions(String clientId)`

* **Node Management**
  * `List<NodeDto> listNodes()`

## Key Differences from v4.x

* User field is **`user_id`** (not `username`)
* List responses are **paginated**: `{"data": [...], "meta": {"count": N, ...}}`
* ACL methods removed — not available in EMQX 5.x CE

## Building

```bash
mvn compile    # compile
mvn test       # run tests (requires EMQX running at localhost:18083)
mvn package    # build JAR
```

## License

Apache License 2.0. See [LICENSE](LICENSE).
