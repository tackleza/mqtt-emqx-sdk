package com.apidech.sdk.emqxsdk;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * EMQX REST API client (v5).
 *
 * Provides synchronous methods to interact with EMQX Management REST API,
 * including user, client, session, subscription, and node management.
 *
 * <p>Note: ACL management ({@code /acl}) is not available in EMQX 5.x CE
 * via REST API. ACLs are configured via the file-based authorization backend
 * or the {@code /authorization/sources} API.</p>
 */
public final class EmqxSdkClient {

	private final OkHttpClient httpClient;
	private final String       baseUrl;
	private final Gson         gson;

	private EmqxSdkClient(Builder b) {
		this.baseUrl = b.baseUrl;
		this.gson    = b.gson != null ? b.gson : new Gson();

		OkHttpClient.Builder httpB = new OkHttpClient.Builder()
				.connectTimeout(b.connectTimeoutMs, TimeUnit.MILLISECONDS)
				.readTimeout(b.readTimeoutMs, TimeUnit.MILLISECONDS)
				.writeTimeout(b.writeTimeoutMs, TimeUnit.MILLISECONDS);

		if (b.authInterceptor != null) {
			httpB.addInterceptor(b.authInterceptor);
		}

		this.httpClient = httpB.build();
	}

	// -------------------------------------------------------------------------
	// Internal helpers
	// -------------------------------------------------------------------------

	private void throwOnError(Response resp) throws IOException {
		if (!resp.isSuccessful()) {
			String body = resp.body() != null ? resp.body().string() : "";
			throw new EmqxApiException(resp.code(), parseErrorMessage(body));
		}
	}

	private String parseErrorMessage(String body) {
		try {
			JsonObject json = gson.fromJson(body, JsonObject.class);
			if (json != null) {
				if (json.has("message")) return json.get("message").getAsString();
				if (json.has("reason"))  return json.get("reason").getAsString();
				if (json.has("error"))   return json.get("error").getAsString();
				if (json.has("code"))    return json.get("code").getAsString();
			}
		} catch (Exception ignored) { /* not JSON */ }
		return body.isBlank() ? "Unknown error" : body;
	}

	private String bodyString(Response resp) throws IOException {
		return resp.body() != null ? resp.body().string() : "";
	}

	private <T> List<T> parseListResponse(Response resp, Type elementType) throws IOException {
		throwOnError(resp);
		String body = bodyString(resp);
		if (body.isBlank()) {
			// Some list endpoints return empty body on empty result
			//noinspection unchecked
			return (List<T>) java.util.Collections.emptyList();
		}
		// Check if wrapped in {data:[], meta:{}} format
		try {
			JsonObject wrapper = gson.fromJson(body, JsonObject.class);
			if (wrapper != null && wrapper.has("data")) {
				Type listType = TypeToken.getParameterized(List.class, elementType).getType();
				return gson.fromJson(wrapper.getAsJsonArray("data"), listType);
			}
		} catch (Exception ignored) { /* fall through to array parse */ }
		// Plain array response
		Type listType = TypeToken.getParameterized(List.class, elementType).getType();
		return gson.fromJson(body, listType);
	}

	// -------------------------------------------------------------------------
	// User Management
	// -------------------------------------------------------------------------

	/**
	 * Create a new user in the specified authenticator chain.
	 *
	 * <p>The authenticator must exist in EMQX (e.g. created via
	 * {@code POST /authentication} with {@code mechanism: "password_based"}
	 * and {@code backend: "built_in_database"}).</p>
	 *
	 * @param authenticatorId the authenticator ID (e.g. "password_based:built_in_database")
	 * @param user           contains {@code user_id} and {@code password}
	 * @return the created {@link UserDto} (includes EMQX-set fields like {@code is_superuser})
	 * @throws EmqxApiException on HTTP errors or parse failures
	 */
	public UserDto createUser(AuthenticatorId authenticatorId, UserDto user) throws IOException {
		String url  = String.format("%s/authentication/%s/users", baseUrl, authenticatorId);
		String json = gson.toJson(user);
		RequestBody body = RequestBody.create(json, MediaType.get("application/json"));
		Request req = new Request.Builder().url(url).post(body).build();
		try (Response resp = httpClient.newCall(req).execute()) {
			throwOnError(resp);
			return gson.fromJson(bodyString(resp), UserDto.class);
		}
	}

	/**
	 * Retrieve details of a specific user.
	 *
	 * @param authenticatorId the authenticator ID
	 * @param userId         the user identifier
	 * @return the {@link UserDto}
	 * @throws EmqxApiException if not found or on network error
	 */
	public UserDto getUser(AuthenticatorId authenticatorId, String userId) throws IOException {
		String url  = String.format("%s/authentication/%s/users/%s", baseUrl, authenticatorId, userId);
		Request req = new Request.Builder().url(url).get().build();
		try (Response resp = httpClient.newCall(req).execute()) {
			throwOnError(resp);
			return gson.fromJson(bodyString(resp), UserDto.class);
		}
	}

	/**
	 * List all users under the given authenticator chain.
	 *
	 * @param authenticatorId the authenticator ID
	 * @return a list of {@link UserDto}
	 * @throws EmqxApiException on network or parse errors
	 */
	public List<UserDto> listUsers(AuthenticatorId authenticatorId) throws IOException {
		String url  = String.format("%s/authentication/%s/users", baseUrl, authenticatorId);
		Request req = new Request.Builder().url(url).get().build();
		try (Response resp = httpClient.newCall(req).execute()) {
			return parseListResponse(resp, UserDto.class);
		}
	}

	/**
	 * Delete a user by identifier.
	 *
	 * @param authenticatorId the authenticator ID
	 * @param userId         the user to delete
	 * @throws EmqxApiException if deletion fails
	 */
	public void deleteUser(AuthenticatorId authenticatorId, String userId) throws IOException {
		String url  = String.format("%s/authentication/%s/users/%s", baseUrl, authenticatorId, userId);
		Request req = new Request.Builder().url(url).delete().build();
		try (Response resp = httpClient.newCall(req).execute()) {
			throwOnError(resp);
		}
	}

	// -------------------------------------------------------------------------
	// Client Management
	// -------------------------------------------------------------------------

	/**
	 * List all connected MQTT clients.
	 *
	 * @return a list of {@link ClientDto}
	 * @throws EmqxApiException on network or parse errors
	 */
	public List<ClientDto> listClients() throws IOException {
		String url  = String.format("%s/clients", baseUrl);
		Request req = new Request.Builder().url(url).get().build();
		try (Response resp = httpClient.newCall(req).execute()) {
			return parseListResponse(resp, ClientDto.class);
		}
	}

	/**
	 * Disconnect a client by client ID.
	 *
	 * @param clientId the ID of the client to disconnect
	 * @throws EmqxApiException if disconnection fails
	 */
	public void disconnectClient(String clientId) throws IOException {
		String url  = String.format("%s/clients/%s", baseUrl, clientId);
		Request req = new Request.Builder().url(url).delete().build();
		try (Response resp = httpClient.newCall(req).execute()) {
			throwOnError(resp);
		}
	}

	// -------------------------------------------------------------------------
	// Session Management
	// -------------------------------------------------------------------------

	/**
	 * Retrieve session information for a client.
	 *
	 * @param clientId the ID of the client
	 * @return the {@link SessionDto}
	 * @throws EmqxApiException if not found
	 */
	public SessionDto getSession(String clientId) throws IOException {
		String url  = String.format("%s/sessions/%s", baseUrl, clientId);
		Request req = new Request.Builder().url(url).get().build();
		try (Response resp = httpClient.newCall(req).execute()) {
			throwOnError(resp);
			return gson.fromJson(bodyString(resp), SessionDto.class);
		}
	}

	/**
	 * Delete a session for a client.
	 *
	 * @param clientId the ID of the client
	 * @throws EmqxApiException if deletion fails
	 */
	public void deleteSession(String clientId) throws IOException {
		String url  = String.format("%s/sessions/%s", baseUrl, clientId);
		Request req = new Request.Builder().url(url).delete().build();
		try (Response resp = httpClient.newCall(req).execute()) {
			throwOnError(resp);
		}
	}

	// -------------------------------------------------------------------------
	// Subscription Management
	// -------------------------------------------------------------------------

	/**
	 * List subscriptions for a client.
	 *
	 * @param clientId the ID of the client
	 * @return a list of {@link SubscriptionDto}
	 * @throws EmqxApiException on network or parse errors
	 */
	public List<SubscriptionDto> listSubscriptions(String clientId) throws IOException {
		String url  = String.format("%s/subscriptions/%s", baseUrl, clientId);
		Request req = new Request.Builder().url(url).get().build();
		try (Response resp = httpClient.newCall(req).execute()) {
			return parseListResponse(resp, SubscriptionDto.class);
		}
	}

	// -------------------------------------------------------------------------
	// Node Management
	// -------------------------------------------------------------------------

	/**
	 * List all cluster nodes.
	 *
	 * @return a list of {@link NodeDto}
	 * @throws EmqxApiException on network or parse errors
	 */
	public List<NodeDto> listNodes() throws IOException {
		String url  = String.format("%s/nodes", baseUrl);
		Request req = new Request.Builder().url(url).get().build();
		try (Response resp = httpClient.newCall(req).execute()) {
			return parseListResponse(resp, NodeDto.class);
		}
	}

	// -------------------------------------------------------------------------
	// Authenticator Management (bonus — create/list built-in authenticators)
	// -------------------------------------------------------------------------

	/**
	 * List all configured authentication chains.
	 *
	 * @return a list of authenticator info objects (as generic JsonObject)
	 * @throws EmqxApiException on network or parse errors
	 */
	public List<AuthenticatorDto> listAuthenticators() throws IOException {
		String url  = String.format("%s/authentication", baseUrl);
		Request req = new Request.Builder().url(url).get().build();
		try (Response resp = httpClient.newCall(req).execute()) {
			return parseListResponse(resp, AuthenticatorDto.class);
		}
	}

	/**
	 * Create a built-in database authenticator.
	 *
	 * @param name               authenticator name (used as part of the ID)
	 * @param passwordHashAlgo  hash algorithm: "bcrypt", "sha256", "md5", etc.
	 * @param saltRounds        salt rounds (for bcrypt, use 10)
	 * @return the created authenticator descriptor
	 * @throws EmqxApiException on HTTP errors
	 */
	public AuthenticatorDto createBuiltInDbAuthenticator(String name, String passwordHashAlgo, int saltRounds) throws IOException {
		String url  = String.format("%s/authentication", baseUrl);
		String json = gson.toJson(new AuthenticatorDto.BuiltInDb(name, passwordHashAlgo, saltRounds));
		RequestBody body = RequestBody.create(json, MediaType.get("application/json"));
		Request req = new Request.Builder().url(url).post(body).build();
		try (Response resp = httpClient.newCall(req).execute()) {
			throwOnError(resp);
			return gson.fromJson(bodyString(resp), AuthenticatorDto.class);
		}
	}

	// -------------------------------------------------------------------------
	// Builder
	// -------------------------------------------------------------------------

	public static Builder builder() { return new Builder(); }

	public static class Builder {
		private String      baseUrl;
		private Interceptor authInterceptor;
		private Gson        gson;
		private long        connectTimeoutMs = 10_000;
		private long        readTimeoutMs    = 30_000;
		private long        writeTimeoutMs   = 30_000;

		public Builder baseUrl(String baseUrl) { this.baseUrl = baseUrl; return this; }

		public Builder gson(Gson gson) { this.gson = gson; return this; }

		public Builder connectTimeout(long ms)  { this.connectTimeoutMs = ms; return this; }
		public Builder readTimeout(long ms)     { this.readTimeoutMs    = ms; return this; }
		public Builder writeTimeout(long ms)   { this.writeTimeoutMs   = ms; return this; }

		/**
		 * Use HTTP Basic Auth with API key and secret.
		 * Note: EMQX 5.x CE does not expose API key creation via REST API;
		 * use the Dashboard UI (Settings → API Keys) or JWT instead.
		 */
		public Builder basicAuth(String apiKey, String apiSecret) {
			final String cred = Credentials.basic(apiKey, apiSecret);
			this.authInterceptor = chain -> {
				Request req = chain.request().newBuilder()
						.header("Authorization", cred)
						.build();
				return chain.proceed(req);
			};
			return this;
		}

		/**
		 * Use Bearer token authentication (JWT).
		 * Obtain a JWT by calling {@code POST <baseUrl>/login}
		 * with {@code {"username": "...", "password": "..."}}.
		 */
		public Builder bearerAuth(String token) {
			this.authInterceptor = chain -> {
				Request req = chain.request().newBuilder()
						.header("Authorization", "Bearer " + token)
						.build();
				return chain.proceed(req);
			};
			return this;
		}

		public EmqxSdkClient build() {
			if (baseUrl == null || baseUrl.isBlank()) {
				throw new IllegalStateException("baseUrl must be set");
			}
			return new EmqxSdkClient(this);
		}
	}
}
