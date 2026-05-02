package com.apidech.sdk.emqxsdk;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for EmqxSdkClient against an external EMQX broker.
 *
 * Use this when you have an EMQX broker running externally (e.g., via docker-compose).
 *
 * Configure via environment variables:
 *   EMQX_HOST (default: localhost)
 *   EMQX_API_PORT (default: 18083)
 *   EMQX_USER (default: admin)
 *   EMQX_PASSWORD (default: public)
 *
 * Run with:
 *   mvn test -Dtest=EmqxSdkClientExternalIntegrationTest
 *
 * Or with custom settings:
 *   EMQX_HOST=192.168.1.100 EMQX_API_PORT=18083 mvn test -Dtest=EmqxSdkClientExternalIntegrationTest
 */
class EmqxSdkClientExternalIntegrationTest {

	private static final String EMQX_HOST = System.getenv().getOrDefault("EMQX_HOST", "localhost");
	private static final int EMQX_API_PORT = Integer.parseInt(System.getenv().getOrDefault("EMQX_API_PORT", "18083"));
	private static final String EMQX_USER = System.getenv().getOrDefault("EMQX_USER", "admin");
	private static final String EMQX_PASSWORD = System.getenv().getOrDefault("EMQX_PASSWORD", "public");

	private static EmqxSdkClient client;

	@BeforeAll
	static void setupClient() throws IOException {
		String baseUrl = "http://" + EMQX_HOST + ":" + EMQX_API_PORT + "/api/v5";

		System.out.println("=".repeat(60));
		System.out.println("EMQX Integration Test Configuration");
		System.out.println("=".repeat(60));
		System.out.println("Host:     " + EMQX_HOST);
		System.out.println("Port:     " + EMQX_API_PORT);
		System.out.println("User:     " + EMQX_USER);
		System.out.println("Base URL: " + baseUrl);
		System.out.println("=".repeat(60));

		client = EmqxSdkClient.builder()
				.baseUrl(baseUrl)
				.basicAuth(EMQX_USER, EMQX_PASSWORD)
				.connectTimeout(10_000)
				.readTimeout(10_000)
				.writeTimeout(10_000)
				.build();
	}

	// -------------------------------------------------------------------------
	// Node Management
	// -------------------------------------------------------------------------

	@Nested
	class NodeManagement {

		@Test
		void listNodes_returnsAtLeastOneNode() throws IOException {
			List<NodeDto> nodes = client.listNodes();

			assertNotNull(nodes);
			assertFalse(nodes.isEmpty(), "Should have at least one node. Is EMQX running on " + EMQX_HOST + ":" + EMQX_API_PORT + "?");

			NodeDto node = nodes.get(0);
			assertNotNull(node.node);
			assertNotNull(node.version);
			assertEquals("running", node.status, "Node should be in running state");
		}

		@Test
		void listNodes_nodeHasMetrics() throws IOException {
			List<NodeDto> nodes = client.listNodes();
			NodeDto node = nodes.get(0);

			assertNotNull(node.live_connections);
			assertTrue(node.live_connections >= 0, "Live connections should be >= 0");
			assertNotNull(node.memory_used);
			assertTrue(node.memory_used > 0, "Memory used should be > 0");
			assertNotNull(node.uptime_ms);
			assertTrue(node.uptime_ms >= 0, "Uptime should be >= 0");
		}

		@Test
		void listNodes_displaysNodeInfo() throws IOException {
			List<NodeDto> nodes = client.listNodes();
			NodeDto node = nodes.get(0);

			System.out.println("\n" + "-".repeat(60));
			System.out.println("Node Information");
			System.out.println("-".repeat(60));
			System.out.println("Node:              " + node.node);
			System.out.println("Version:           " + node.version);
			System.out.println("Status:            " + node.status);
			System.out.println("Uptime (ms):       " + node.uptime_ms);
			System.out.println("Memory Used:       " + formatBytes(node.memory_used));
			System.out.println("Memory Total:      " + formatBytes(node.memory_total));
			System.out.println("Live Connections:  " + node.live_connections);
			System.out.println("-".repeat(60));
		}
	}

	// -------------------------------------------------------------------------
	// User Management
	// -------------------------------------------------------------------------

	@Nested
	class UserManagement {

		private static final String TEST_USER_PREFIX = "test_" + System.currentTimeMillis();

		@Test
		void createUser_succeeds() throws IOException {
			String userId = TEST_USER_PREFIX + "_create";
			UserDto input = UserDto.builder()
					.user_id(userId)
					.password("TestPass@123")
					.is_superuser(false)
					.build();

			UserDto created = client.createUser(AuthenticatorId.DEFAULT, input);

			assertNotNull(created);
			assertEquals(userId, created.user_id);
			assertFalse(created.is_superuser);

			System.out.println("\n✓ Created user: " + userId);
		}

		@Test
		void getUser_returnsUser() throws IOException {
			String userId = TEST_USER_PREFIX + "_get";
			UserDto input = UserDto.builder()
					.user_id(userId)
					.password("TestPass@123")
					.build();
			client.createUser(AuthenticatorId.DEFAULT, input);

			UserDto retrieved = client.getUser(AuthenticatorId.DEFAULT, userId);

			assertNotNull(retrieved);
			assertEquals(userId, retrieved.user_id);

			System.out.println("✓ Retrieved user: " + userId);
		}

		@Test
		void listUsers_returnsListWithBuiltinUsers() throws IOException {
			List<UserDto> users = client.listUsers(AuthenticatorId.DEFAULT);

			assertNotNull(users);
			assertFalse(users.isEmpty(), "Should have at least the admin user");

			System.out.println("\n✓ Found " + users.size() + " user(s)");
			for (UserDto user : users) {
				System.out.println("  - " + user.user_id + (user.is_superuser ? " [superuser]" : ""));
			}
		}

		@Test
		void deleteUser_succeeds() throws IOException {
			String userId = TEST_USER_PREFIX + "_delete";
			UserDto input = UserDto.builder()
					.user_id(userId)
					.password("TestPass@123")
					.build();
			client.createUser(AuthenticatorId.DEFAULT, input);

			assertDoesNotThrow(() -> client.deleteUser(AuthenticatorId.DEFAULT, userId));
			System.out.println("\n✓ Deleted user: " + userId);
		}

		@Test
		void userLifecycle_endToEnd() throws IOException {
			String userId = TEST_USER_PREFIX + "_lifecycle";
			String password = "LifecyclePass@123";

			// Create
			UserDto input = UserDto.builder()
					.user_id(userId)
					.password(password)
					.is_superuser(false)
					.build();
			UserDto created = client.createUser(AuthenticatorId.DEFAULT, input);
			assertEquals(userId, created.user_id);

			// Read
			UserDto retrieved = client.getUser(AuthenticatorId.DEFAULT, userId);
			assertEquals(userId, retrieved.user_id);

			// Delete
			assertDoesNotThrow(() -> client.deleteUser(AuthenticatorId.DEFAULT, userId));

			System.out.println("\n✓ User lifecycle completed: " + userId);
		}
	}

	// -------------------------------------------------------------------------
	// Client Management
	// -------------------------------------------------------------------------

	@Nested
	class ClientManagement {

		@Test
		void listClients_returnsEmptyOrPopulatedList() throws IOException {
			List<ClientDto> clients = client.listClients();

			assertNotNull(clients);
			System.out.println("\n✓ Found " + clients.size() + " connected client(s)");

			for (ClientDto client : clients) {
				System.out.println("  - clientid: " + client.clientid + ", user: " + client.username + ", state: " + client.conn_state);
			}
		}
	}

	// -------------------------------------------------------------------------
	// Authenticator & Session
	// -------------------------------------------------------------------------

	@Nested
	class AuthenticatorAndSession {

		@Test
		void listAuthenticators_returnsBuiltinAuthenticator() throws IOException {
			List<AuthenticatorDto> auths = client.listAuthenticators();

			assertNotNull(auths);
			assertFalse(auths.isEmpty(), "Should have at least one authenticator");

			System.out.println("\n✓ Found " + auths.size() + " authenticator(s):");
			for (AuthenticatorDto auth : auths) {
				System.out.println("  - " + auth.id);
				if (auth.password_hash_algorithm != null) {
					System.out.println("    Algorithm: " + auth.password_hash_algorithm.name);
				}
			}
		}
	}

	// -------------------------------------------------------------------------
	// Utility
	// -------------------------------------------------------------------------

	private static String formatBytes(long bytes) {
		if (bytes <= 0) return "0 B";
		final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
		int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
		return String.format("%.1f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
	}
}
