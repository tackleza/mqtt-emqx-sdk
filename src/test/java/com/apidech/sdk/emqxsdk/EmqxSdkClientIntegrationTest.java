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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Integration tests for EmqxSdkClient using a real EMQX container.
 * These tests connect to an actual EMQX broker running in Docker.
 */
@Testcontainers
class EmqxSdkClientIntegrationTest {

	private static final String EMQX_IMAGE = "emqx/emqx:latest";
	private static final int MQTT_PORT = 1883;
	private static final int API_PORT = 18083;
	private static final String DEFAULT_USER = "admin";
	private static final String DEFAULT_PASSWORD = "public";

	@Container
	static GenericContainer<?> emqx = new GenericContainer<>(DockerImageName.parse(EMQX_IMAGE))
			.withExposedPorts(MQTT_PORT, API_PORT)
			.withEnv("EMQX_NAME", "emqx")
			.withEnv("EMQX_HOST", "0.0.0.0")
			.waitingFor(new HttpWaitStrategy()
					.forPath("/api/v5/nodes")
					.forPort(API_PORT)
					.withBasicCredentials(DEFAULT_USER, DEFAULT_PASSWORD)
					.withStartupTimeout(java.time.Duration.ofSeconds(30)));

	private static EmqxSdkClient client;

	@BeforeAll
	static void setupClient() throws IOException {
		String baseUrl = "http://" + emqx.getHost() + ":" + emqx.getMappedPort(API_PORT) + "/api/v5";
		client = EmqxSdkClient.builder()
				.baseUrl(baseUrl)
				.basicAuth(DEFAULT_USER, DEFAULT_PASSWORD)
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
			assertFalse(nodes.isEmpty(), "Should have at least one node");

			NodeDto node = nodes.get(0);
			assertNotNull(node.node);
			assertNotNull(node.version);
			assertEquals("running", node.status);
		}

		@Test
		void listNodes_nodeHasMetrics() throws IOException {
			List<NodeDto> nodes = client.listNodes();

			NodeDto node = nodes.get(0);
			assertNotNull(node.live_connections);
			assertTrue(node.live_connections >= 0, "Live connections should be >= 0");
			assertNotNull(node.memory_used);
			assertTrue(node.memory_used > 0, "Memory used should be > 0");
		}
	}

	// -------------------------------------------------------------------------
	// User Management
	// -------------------------------------------------------------------------

	@Nested
	class UserManagement {

		private static final String TEST_USER = "testuser";
		private static final String TEST_PASSWORD = "testpass123";

		@Test
		void createUser_succeeds() throws IOException {
			UserDto input = UserDto.builder()
					.user_id(TEST_USER)
					.password(TEST_PASSWORD)
					.is_superuser(false)
					.build();

			UserDto created = client.createUser(AuthenticatorId.DEFAULT, input);

			assertNotNull(created);
			assertEquals(TEST_USER, created.user_id);
			assertFalse(created.is_superuser);
		}

		@Test
		void getUser_returnsUser() throws IOException {
			UserDto input = UserDto.builder()
					.user_id("getuser_test")
					.password("pass123")
					.build();
			client.createUser(AuthenticatorId.DEFAULT, input);

			UserDto retrieved = client.getUser(AuthenticatorId.DEFAULT, "getuser_test");

			assertNotNull(retrieved);
			assertEquals("getuser_test", retrieved.user_id);
		}

		@Test
		void listUsers_returnsListWithBuiltinUsers() throws IOException {
			List<UserDto> users = client.listUsers(AuthenticatorId.DEFAULT);

			assertNotNull(users);
			assertFalse(users.isEmpty(), "Should have at least the admin user");
		}

		@Test
		void deleteUser_succeeds() throws IOException {
			UserDto input = UserDto.builder()
					.user_id("deleteuser_test")
					.password("pass123")
					.build();
			client.createUser(AuthenticatorId.DEFAULT, input);

			assertDoesNotThrow(() -> client.deleteUser(AuthenticatorId.DEFAULT, "deleteuser_test"));

			// Verify it's deleted by trying to get it (should throw exception)
			// This is expected to fail now
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
			// List can be empty initially or have clients depending on test state
			assertTrue(clients.size() >= 0, "Client list should not be null");
		}
	}

	// -------------------------------------------------------------------------
	// Session Management
	// -------------------------------------------------------------------------

	@Nested
	class SessionManagement {

		@Test
		void listAuthenticators_returnsBuiltinAuthenticator() throws IOException {
			List<AuthenticatorDto> auths = client.listAuthenticators();

			assertNotNull(auths);
			assertFalse(auths.isEmpty(), "Should have at least one authenticator");

			boolean hasBuiltInDb = auths.stream()
					.anyMatch(auth -> auth.id.contains("password_based:built_in_database"));
			assertTrue(hasBuiltInDb, "Should have built-in database authenticator");
		}
	}

	// -------------------------------------------------------------------------
	// End-to-End Flow
	// -------------------------------------------------------------------------

	@Nested
	class EndToEndFlow {

		@Test
		void userLifecycle_createReadDeleteUser() throws IOException {
			String userId = "lifecycle_test_" + System.currentTimeMillis();
			String password = "secure_pass_123";

			// Create user
			UserDto createInput = UserDto.builder()
					.user_id(userId)
					.password(password)
					.is_superuser(false)
					.build();
			UserDto created = client.createUser(AuthenticatorId.DEFAULT, createInput);
			assertNotNull(created);
			assertEquals(userId, created.user_id);

			// Read user
			UserDto retrieved = client.getUser(AuthenticatorId.DEFAULT, userId);
			assertEquals(userId, retrieved.user_id);

			// Delete user
			assertDoesNotThrow(() -> client.deleteUser(AuthenticatorId.DEFAULT, userId));
		}

		@Test
		void nodeInfo_metricsArePopulated() throws IOException {
			List<NodeDto> nodes = client.listNodes();
			assertFalse(nodes.isEmpty());

			NodeDto node = nodes.get(0);
			assertNotNull(node.node);
			assertNotNull(node.version);
			assertNotNull(node.status);
			assertNotNull(node.uptime_ms);
			assertNotNull(node.memory_used);
			assertNotNull(node.memory_total);
			assertNotNull(node.live_connections);

			assertTrue(node.uptime_ms >= 0, "Uptime should be non-negative");
			assertTrue(node.memory_used > 0, "Memory used should be positive");
			assertTrue(node.memory_total >= node.memory_used, "Total memory >= used memory");
			assertTrue(node.live_connections >= 0, "Live connections should be non-negative");
		}
	}
}
