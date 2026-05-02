package com.apidech.sdk.emqxsdk;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class EmqxSdkClientTest {

	private MockWebServer server;
	private String        baseUrl;
	private EmqxSdkClient client;

	@BeforeEach
	void startServer() throws IOException {
		server  = new MockWebServer();
		server.start();
		baseUrl = server.url("/api/v5").toString();
		client  = EmqxSdkClient.builder()
				.baseUrl(baseUrl)
				.bearerAuth("test-token")
				.build();
	}

	@AfterEach
	void stopServer() throws IOException {
		if (server != null) server.shutdown();
	}

	// -------------------------------------------------------------------------
	// Helper assertions
	// -------------------------------------------------------------------------

	private void assertGetRequest(RecordedRequest req, String expectedPath) {
		assertNotNull(req);
		assertEquals("GET", req.getMethod());
		assertTrue(req.getPath().contains(expectedPath),
				"Expected path to contain " + expectedPath + " but was " + req.getPath());
		assertEquals("Bearer test-token", req.getHeader("Authorization"));
	}

	private void assertDeleteRequest(RecordedRequest req, String expectedPath) {
		assertNotNull(req);
		assertEquals("DELETE", req.getMethod());
		assertTrue(req.getPath().contains(expectedPath),
				"Expected path to contain " + expectedPath + " but was " + req.getPath());
	}

	// -------------------------------------------------------------------------
	// Node Management
	// -------------------------------------------------------------------------

	@Nested
	class NodeManagement {

		@Test
		void listNodes_returnsNodeList() throws Exception {
			String json = """
				[
				  {"node": "emqx@127.0.0.1", "version": "5.8.4",
				   "status": "running", "uptime_ms": 100000,
				   "memory_used": 1024, "memory_total": 4096,
				   "live_connections": 5}
				]
				""";
			server.enqueue(new MockResponse()
					.setBody(json)
					.addHeader("Content-Type", "application/json"));

			List<NodeDto> nodes = client.listNodes();

			assertEquals(1, nodes.size());
			assertEquals("emqx@127.0.0.1", nodes.get(0).node);
			assertEquals("5.8.4", nodes.get(0).version);
			assertEquals(Integer.valueOf(5), nodes.get(0).live_connections);
			assertGetRequest(server.takeRequest(), "/nodes");
		}

		@Test
		void listNodes_emptyList() throws Exception {
			server.enqueue(new MockResponse()
					.setBody("[]")
					.addHeader("Content-Type", "application/json"));

			List<NodeDto> nodes = client.listNodes();
			assertTrue(nodes.isEmpty());
		}

		@Test
		void listNodes_httpError_throwsEmqxApiException() throws Exception {
			server.enqueue(new MockResponse()
					.setResponseCode(500)
					.setBody("{\"code\":\"INTERNAL_ERROR\",\"message\":\"boom\"}"));

			EmqxApiException ex = assertThrows(EmqxApiException.class,
					() -> client.listNodes());
			assertEquals(500, ex.httpStatus());
			assertTrue(ex.emqxMessage().contains("boom"));
		}
	}

	// -------------------------------------------------------------------------
	// User Management
	// -------------------------------------------------------------------------

	@Nested
	class UserManagement {

//		private static final String AUTH_ID = "password_based:built_in_database";

		@Test
		void createUser_returnsUser() throws Exception {
			String response = """
				{"user_id": "alice", "is_superuser": false}
				""";
			server.enqueue(new MockResponse()
					.setBody(response)
					.addHeader("Content-Type", "application/json"));

			UserDto input = UserDto.builder()
					.user_id("alice")
					.password("secret")
					.build();
			UserDto result = client.createUser(AuthenticatorId.DEFAULT, input);

			assertEquals("alice", result.user_id);
			assertEquals("alice", result.getUserId());
			assertFalse(result.is_superuser);

			RecordedRequest req = server.takeRequest();
			assertEquals("POST", req.getMethod());
			assertTrue(req.getPath().contains("/authentication/"));
			assertTrue(req.getPath().contains("/users"));
			assertEquals("Bearer test-token", req.getHeader("Authorization"));
		}

		@Test
		void getUser_returnsUser() throws Exception {
			server.enqueue(new MockResponse()
					.setBody("{\"user_id\":\"alice\",\"is_superuser\":false}")
					.addHeader("Content-Type", "application/json"));

			UserDto result = client.getUser(AuthenticatorId.DEFAULT, "alice");

			assertEquals("alice", result.user_id);
			assertGetRequest(server.takeRequest(),
					"/authentication/password_based:built_in_database/users/alice");
		}

		@Test
		void listUsers_paginatedResponse_returnsDataArray() throws Exception {
			// EMQX v5 returns {data:[...], meta:{}}
			String response = """
				{"data":[
				  {"user_id":"alice","is_superuser":false},
				  {"user_id":"bob","is_superuser":true}
				],"meta":{"count":2,"limit":100,"page":1,"hasnext":false}}
				""";
			server.enqueue(new MockResponse()
					.setBody(response)
					.addHeader("Content-Type", "application/json"));

			List<UserDto> users = client.listUsers(AuthenticatorId.DEFAULT);

			assertEquals(2, users.size());
			assertEquals("alice", users.get(0).user_id);
			assertFalse(users.get(0).is_superuser);
			assertEquals("bob", users.get(1).user_id);
			assertTrue(users.get(1).is_superuser);
			assertGetRequest(server.takeRequest(),
					"/authentication/password_based:built_in_database/users");
		}

		@Test
		void listUsers_emptyPaginatedResponse_returnsEmptyList() throws Exception {
			server.enqueue(new MockResponse()
					.setBody("{\"data\":[],\"meta\":{\"count\":0}}")
					.addHeader("Content-Type", "application/json"));

			List<UserDto> users = client.listUsers(AuthenticatorId.DEFAULT);
			assertTrue(users.isEmpty());
		}

		@Test
		void deleteUser_succeeds() throws Exception {
			server.enqueue(new MockResponse().setResponseCode(204));

			assertDoesNotThrow(() -> client.deleteUser(AuthenticatorId.DEFAULT, "alice"));

			assertDeleteRequest(server.takeRequest(),
					"/authentication/password_based:built_in_database/users/alice");
		}

		@Test
		void getUser_notFound_throwsEmqxApiException() throws Exception {
			server.enqueue(new MockResponse()
					.setResponseCode(404)
					.setBody("{\"code\":\"NOT_FOUND\",\"message\":\"User not found\"}"));

			EmqxApiException ex = assertThrows(EmqxApiException.class,
					() -> client.getUser(AuthenticatorId.DEFAULT, "ghost"));
			assertEquals(404, ex.httpStatus());
			assertTrue(ex.getMessage().contains("404"));
		}
	}

	// -------------------------------------------------------------------------
	// Client Management
	// -------------------------------------------------------------------------

	@Nested
	class ClientManagement {

		@Test
		void listClients_paginatedResponse() throws Exception {
			String response = """
				{"data":[
				  {"clientid":"c1","username":"alice","ip_address":"127.0.0.1",
				   "port":1883,"keepalive":60,"conn_state":"connected"}
				],"meta":{"count":1,"limit":100,"page":1,"hasnext":false}}
				""";
			server.enqueue(new MockResponse()
					.setBody(response)
					.addHeader("Content-Type", "application/json"));

			List<ClientDto> clients = client.listClients();

			assertEquals(1, clients.size());
			assertEquals("c1", clients.get(0).clientid);
			assertEquals("alice", clients.get(0).username);
			assertEquals(60, clients.get(0).keepalive.intValue());
			assertGetRequest(server.takeRequest(), "/clients");
		}

		@Test
		void disconnectClient_succeeds() throws Exception {
			server.enqueue(new MockResponse().setResponseCode(204));

			assertDoesNotThrow(() -> client.disconnectClient("c1"));
			assertDeleteRequest(server.takeRequest(), "/clients/c1");
		}
	}

	// -------------------------------------------------------------------------
	// Session Management
	// -------------------------------------------------------------------------

	@Nested
	class SessionManagement {

		@Test
		void getSession_returnsSession() throws Exception {
			server.enqueue(new MockResponse()
					.setBody("""
						{"clientid":"c1","existing":true,"expiry":0,
						 "max_inflight":32,"mqueue_len":0,"forbidden":false}
						""")
					.addHeader("Content-Type", "application/json"));

			SessionDto s = client.getSession("c1");

			assertEquals("c1", s.clientid);
			assertTrue(s.existing);
			assertEquals(32, s.max_inflight.intValue());
			assertGetRequest(server.takeRequest(), "/sessions/c1");
		}

		@Test
		void deleteSession_succeeds() throws Exception {
			server.enqueue(new MockResponse().setResponseCode(204));

			assertDoesNotThrow(() -> client.deleteSession("c1"));
			assertDeleteRequest(server.takeRequest(), "/sessions/c1");
		}
	}

	// -------------------------------------------------------------------------
	// Subscription Management
	// -------------------------------------------------------------------------

	@Nested
	class SubscriptionManagement {

		@Test
		void listSubscriptions_paginated() throws Exception {
			String response = """
				{"data":[
				  {"clientid":"c1","topic":"a/b","qos":1},
				  {"clientid":"c1","topic":"c/d","qos":0}
				],"meta":{"count":2,"limit":100,"page":1,"hasnext":false}}
				""";
			server.enqueue(new MockResponse()
					.setBody(response)
					.addHeader("Content-Type", "application/json"));

			List<SubscriptionDto> subs = client.listSubscriptions("c1");

			assertEquals(2, subs.size());
			assertEquals("a/b", subs.get(0).topic);
			assertEquals(1, subs.get(0).qos.intValue());
			assertEquals("c/d", subs.get(1).topic);
			assertGetRequest(server.takeRequest(), "/subscriptions/c1");
		}
	}

	// -------------------------------------------------------------------------
	// Authenticator Management
	// -------------------------------------------------------------------------

	@Nested
	class AuthenticatorManagement {

		@Test
		void listAuthenticators_returnsList() throws Exception {
			String response = """
				[
				  {"id":"password_based:built_in_database","mechanism":"password_based",
				   "backend":"built_in_database","enable":true,
                   "password_hash_algorithm":{"name":"bcrypt","salt_rounds":10}}
				]
				""";
			server.enqueue(new MockResponse()
					.setBody(response)
					.addHeader("Content-Type", "application/json"));

			List<AuthenticatorDto> auths = client.listAuthenticators();

			assertEquals(1, auths.size());
			assertEquals("password_based:built_in_database", auths.get(0).id);
			assertEquals("bcrypt", auths.get(0).password_hash_algorithm.name);
			assertGetRequest(server.takeRequest(), "/authentication");
		}

		@Test
		void createBuiltInDbAuthenticator_returnsDescriptor() throws Exception {
			server.enqueue(new MockResponse()
					.setBody("""
						{"id":"password_based:my_auth","mechanism":"password_based",
						 "backend":"built_in_database","enable":true}
						""")
					.addHeader("Content-Type", "application/json"));

			AuthenticatorDto result =
					client.createBuiltInDbAuthenticator("my_auth", "bcrypt", 10);

			assertEquals("password_based:my_auth", result.id);
			RecordedRequest req = server.takeRequest();
			assertEquals("POST", req.getMethod());
			assertEquals("/api/v5/authentication", req.getPath());
			String body = req.getBody().readUtf8();
			assertTrue(body.length() > 0, "Request body should not be empty, got: " + body);
			assertTrue(body.contains("password_based"), "body=" + body);
		}
	}

	// -------------------------------------------------------------------------
	// Error Handling
	// -------------------------------------------------------------------------

	@Nested
	class ErrorHandling {

		@Test
		void httpError_parsesJsonErrorMessage() throws Exception {
			server.enqueue(new MockResponse()
					.setResponseCode(400)
					.setBody("""
						{"code":"BAD_REQUEST","message":"User already exists"}
						"""));

			EmqxApiException ex = assertThrows(EmqxApiException.class,
					() -> client.listUsers(AuthenticatorId.DEFAULT));
			assertEquals(400, ex.httpStatus());
			assertEquals("User already exists", ex.emqxMessage());
		}

		@Test
		void httpError_nonJsonBody_usesRawBody() throws Exception {
			server.enqueue(new MockResponse()
					.setResponseCode(500)
					.setBody("Internal Server Error"));

			EmqxApiException ex = assertThrows(EmqxApiException.class,
					() -> client.listNodes());
			assertEquals(500, ex.httpStatus());
			assertEquals("Internal Server Error", ex.emqxMessage());
		}

		@Test
		void httpError_emptyBody_usesUnknownError() throws Exception {
			server.enqueue(new MockResponse().setResponseCode(502));

			EmqxApiException ex = assertThrows(EmqxApiException.class,
					() -> client.listNodes());
			assertEquals(502, ex.httpStatus());
			assertEquals("Unknown error", ex.emqxMessage());
		}
	}

	// -------------------------------------------------------------------------
	// Builder
	// -------------------------------------------------------------------------

	@Nested
	class BuilderTests {

		@Test
		void builder_bearerAuth_setsHeader() throws Exception {
			server.enqueue(new MockResponse().setBody("[]"));

			EmqxSdkClient c = EmqxSdkClient.builder()
					.baseUrl(baseUrl)
					.bearerAuth("my-jwt-token")
					.build();
			c.listNodes();

			RecordedRequest req = server.takeRequest();
			assertEquals("Bearer my-jwt-token", req.getHeader("Authorization"));
		}

		@Test
		void builder_basicAuth_setsHeader() throws Exception {
			server.enqueue(new MockResponse().setBody("[]"));

			EmqxSdkClient c = EmqxSdkClient.builder()
					.baseUrl(baseUrl)
					.basicAuth("my-key", "my-secret")
					.build();
			c.listNodes();

			RecordedRequest req = server.takeRequest();
			assertTrue(req.getHeader("Authorization").startsWith("Basic "));
		}

		@Test
		void builder_timeouts_dontThrowOnConstruction() {
			EmqxSdkClient c = EmqxSdkClient.builder()
					.baseUrl(baseUrl)
					.connectTimeout(1_000)
					.readTimeout(1_000)
					.writeTimeout(1_000)
					.bearerAuth("x")
					.build();

			assertNotNull(c);
		}

		@Test
		void builder_noBaseUrl_throwsIllegalState() {
			IllegalStateException ex = assertThrows(IllegalStateException.class,
					() -> EmqxSdkClient.builder()
							.bearerAuth("x")
							.build());
			assertTrue(ex.getMessage().contains("baseUrl"));
		}
	}
}
