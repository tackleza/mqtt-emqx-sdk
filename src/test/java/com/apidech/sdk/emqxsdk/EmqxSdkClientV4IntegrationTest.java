package com.apidech.sdk.emqxsdk;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for EmqxSdkClient against EMQX 4.4.8.
 *
 * This demonstrates real API calls against a live MQTT broker running in Docker.
 * Tests the SDK's HTTP client against actual EMQX responses.
 *
 * Start EMQX 4.4.8:
 *   docker-compose up -d
 *
 * Run tests:
 *   mvn test -Dtest=EmqxSdkClientV4IntegrationTest
 *
 * Stop EMQX:
 *   docker-compose down
 */
class EmqxSdkClientV4IntegrationTest {

	private static final String EMQX_HOST = System.getenv().getOrDefault("EMQX_HOST", "localhost");
	private static final int EMQX_API_PORT = Integer.parseInt(System.getenv().getOrDefault("EMQX_API_PORT", "18083"));
	private static final String EMQX_USER = System.getenv().getOrDefault("EMQX_USER", "admin");
	private static final String EMQX_PASSWORD = System.getenv().getOrDefault("EMQX_PASSWORD", "public");

	private static EmqxSdkClient client;

	@BeforeAll
	static void setupClient() throws IOException {
		// EMQX 4.4.8 uses /api/v4 instead of /api/v5
		String baseUrl = "http://" + EMQX_HOST + ":" + EMQX_API_PORT + "/api/v4";

		System.out.println("\n" + "=".repeat(60));
		System.out.println("EMQX 4.4.8 Integration Test");
		System.out.println("=".repeat(60));
		System.out.println("Connecting to: " + baseUrl);
		System.out.println("=".repeat(60));

		client = EmqxSdkClient.builder()
				.baseUrl(baseUrl)
				.basicAuth(EMQX_USER, EMQX_PASSWORD)
				.connectTimeout(10_000)
				.readTimeout(10_000)
				.writeTimeout(10_000)
				.build();
	}

	@Test
	void testNodeListReturnsLiveData() throws IOException {
		System.out.println("\n📊 Testing node list...");
		List<NodeDto> nodes = client.listNodes();

		assertNotNull(nodes);
		assertFalse(nodes.isEmpty(), "Should have at least one node");

		NodeDto node = nodes.get(0);
		System.out.println("✅ Node found: " + node.node);
		System.out.println("   Version: " + node.version);
		System.out.println("   Status: " + node.status);
		System.out.println("   Memory: " + formatBytes(node.memory_used) + " / " + formatBytes(node.memory_total));
		System.out.println("   Live connections: " + node.live_connections);
	}

	@Test
	void testClientListReturnsData() throws IOException {
		System.out.println("\n📱 Testing client list...");
		List<ClientDto> clients = client.listClients();

		assertNotNull(clients);
		System.out.println("✅ Found " + clients.size() + " connected client(s)");

		for (ClientDto c : clients) {
			System.out.println("   - " + c.clientid + " (user: " + c.username + ", state: " + c.conn_state + ")");
		}
	}

	@Test
	void testListAuthenticators() throws IOException {
		System.out.println("\n🔐 Testing authenticators...");
		List<AuthenticatorDto> auths = client.listAuthenticators();

		assertNotNull(auths);
		System.out.println("✅ Found " + auths.size() + " authenticator(s)");

		for (AuthenticatorDto auth : auths) {
			System.out.println("   - " + auth.id);
		}
	}

	@Test
	void testEndToEnd() throws IOException {
		System.out.println("\n🔄 Testing end-to-end flow...");

		// Get nodes
		List<NodeDto> nodes = client.listNodes();
		assertTrue(!nodes.isEmpty());
		System.out.println("✅ Step 1: Retrieved nodes");

		// Get clients
		List<ClientDto> clients = client.listClients();
		assertNotNull(clients);
		System.out.println("✅ Step 2: Retrieved clients");

		// Get authenticators
		List<AuthenticatorDto> auths = client.listAuthenticators();
		assertTrue(!auths.isEmpty());
		System.out.println("✅ Step 3: Retrieved authenticators");

		System.out.println("✅ End-to-end test complete!");
	}

	private static String formatBytes(long bytes) {
		if (bytes <= 0) return "0 B";
		final String[] units = new String[]{"B", "KB", "MB", "GB"};
		int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
		return String.format("%.1f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
	}
}
