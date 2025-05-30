# mqtt-emqx-sdk

A lightweight Java client for EMQX Management REST API. Provides synchronous methods for managing users, ACL rules, clients, sessions, subscriptions, and cluster nodes.

**⚠️ Beta & No Warranty**

> This SDK is in beta, untested, and provided *as-is* without any warranty. Use at your own risk. **APIs may change frequently.**

## Features

* **User Management**: create, retrieve, list, and delete users via built‑in or custom authenticators
* **ACL Management**: create, retrieve, list, and delete ACL rules
* **Client Management**: list connected clients and disconnect by client ID
* **Session Management**: retrieve and delete client sessions
* **Subscription Management**: list subscriptions per client
* **Node Management**: list cluster nodes
* **Authentication**: supports HTTP Basic (API key/secret) and Bearer token (JWT)
* **Built‑in Enums**: `AuthenticatorId` enum for standard EMQX authentication chains

## Getting Started

### Prerequisites

* Java 21 or higher
* Maven 3.x (for building this SDK)
* EMQX broker (v4.x or later) with Management API enabled

### Dependencies

* **OkHttp**: `com.squareup.okhttp3:okhttp:4.9.3`
* **Gson**: `com.google.code.gson:gson:2.9.0`

## Usage Examples

### 1. Basic Auth Example

```java
import com.apidech.sdk.emqxsdk.EmqxSdkClient;
import com.apidech.sdk.emqxsdk.AuthenticatorId;
import com.apidech.sdk.emqxsdk.UserDto;

public class BasicAuthExample {
    public static void main(String[] args) throws Exception {
        EmqxSdkClient client = EmqxSdkClient.builder()
            .baseUrl("http://localhost:8080/api/v5")
            .basicAuth("apiKey", "apiSecret")
            .build();

        UserDto user = new UserDto("alice", "secret", "built_in_database");
        client.createUser(AuthenticatorId.DEFAULT, user);
        client.listUsers(AuthenticatorId.DEFAULT)
            .forEach(u -> System.out.println(u.getUsername()));
    }
}
```

### 2. Bearer Token Example

```java
import com.apidech.sdk.emqxsdk.EmqxSdkClient;
import com.apidech.sdk.emqxsdk.AuthenticatorId;

public class BearerAuthExample {
    public static void main(String[] args) throws Exception {
        String jwtToken = "eyJhbGci..."; // obtained from EMQX login endpoint

        EmqxSdkClient client = EmqxSdkClient.builder()
            .baseUrl("http://localhost:8080/api/v5")
            .bearerAuth(jwtToken)
            .build();

        client.listClients()
            .forEach(c -> System.out.println(c.getClientid()));
    }
}
```

## API Reference

### `EmqxSdkClient` Methods

* **User Management**

  * `UserDto createUser(AuthenticatorId, UserDto)`
  * `UserDto getUser(AuthenticatorId, String)`
  * `List<UserDto> listUsers(AuthenticatorId)`
  * `void deleteUser(AuthenticatorId, String)`

* **ACL Management**

  * `AclDto createAcl(AclDto)`
  * `AclDto getAcl(int)`
  * `List<AclDto> listAcls()`
  * `void deleteAcl(int)`

* **Client Management**

  * `List<ClientDto> listClients()`
  * `void disconnectClient(String)`

* **Session Management**

  * `SessionDto getSession(String)`
  * `void deleteSession(String)`

* **Subscription Management**

  * `List<SubscriptionDto> listSubscriptions(String)`

* **Node Management**

  * `List<NodeDto> listNodes()`

## AuthenticatorId

Use the `AuthenticatorId` enum to select which authentication chain to call. If you don’t know which to choose, use `AuthenticatorId.DEFAULT`.

Common IDs:

* `AuthenticatorId.DEFAULT` — alias for `password_based:built_in_database`
* `AuthenticatorId.PASSWORD_BASED_MYSQL`
* `AuthenticatorId.PASSWORD_BASED_POSTGRESQL`
* `AuthenticatorId.JWT`
* `AuthenticatorId.SCRAM_BUILT_IN_DATABASE`
* ...see source for full list

## License

Apache License 2.0. See [LICENSE](LICENSE) for details.
