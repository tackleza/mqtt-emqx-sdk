package com.apidech.sdk.emqxsdk;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * EMQX REST API client.
 *
 * Provides synchronous methods to interact with EMQX Management REST API,
 * including user, ACL, client, session, subscription, and node management.
 */
public final class EmqxSdkClient {

    private final OkHttpClient httpClient;
    private final String baseUrl;
    private final Gson gson;

    private EmqxSdkClient(Builder b) {
        this.baseUrl = b.baseUrl;
        this.gson    = b.gson != null ? b.gson : new Gson();
        OkHttpClient.Builder httpB = new OkHttpClient.Builder();
        if (b.authInterceptor != null) {
            httpB.addInterceptor(b.authInterceptor);
        }
        this.httpClient = httpB.build();
    }

    // --------- User Management ---------

    /**
     * Create a new user in the specified authenticator chain.
     *
     * @param authenticatorId the ID of the authentication plugin (e.g., "default")
     * @param user            details of the user to create
     * @return the created {@link UserDto}
     * @throws IOException if a network or serialization error occurs
     */
    public UserDto createUser(AuthenticatorId authenticatorId, UserDto user) throws IOException {
        String url = String.format("%s/authentication/%s/users", baseUrl, authenticatorId);
        String json = gson.toJson(user);
        RequestBody body = RequestBody.create(json, MediaType.get("application/json"));
        Request req = new Request.Builder().url(url).post(body).build();
        try (Response resp = httpClient.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("Error creating user: " + resp);
            }
            return gson.fromJson(resp.body().string(), UserDto.class);
        }
    }

    /**
     * Retrieve details of a specific user.
     *
     * @param authenticatorId the ID of the authentication plugin
     * @param username        the username to fetch
     * @return the {@link UserDto} for the specified user
     * @throws IOException if a network or parsing error occurs or user not found
     */
    public UserDto getUser(AuthenticatorId authenticatorId, String username) throws IOException {
        String url = String.format("%s/authentication/%s/users/%s", baseUrl, authenticatorId, username);
        Request req = new Request.Builder().url(url).get().build();
        try (Response resp = httpClient.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("Error fetching user: " + resp);
            }
            return gson.fromJson(resp.body().string(), UserDto.class);
        }
    }

    /**
     * List all users under the given authenticator chain.
     *
     * @param authenticatorId the ID of the authentication plugin
     * @return a list of {@link UserDto}
     * @throws IOException if a network or parsing error occurs
     */
    public List<UserDto> listUsers(AuthenticatorId authenticatorId) throws IOException {
        String url = String.format("%s/authentication/%s/users", baseUrl, authenticatorId);
        Request req = new Request.Builder().url(url).get().build();
        try (Response resp = httpClient.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("Error listing users: " + resp);
            }
            Type listType = new TypeToken<List<UserDto>>() {}.getType();
            return gson.fromJson(resp.body().string(), listType);
        }
    }

    /**
     * Delete a user by username.
     *
     * @param authenticatorId the ID of the authentication plugin
     * @param username        the username to delete
     * @throws IOException if a network error occurs or deletion fails
     */
    public void deleteUser(AuthenticatorId authenticatorId, String username) throws IOException {
        String url = String.format("%s/authentication/%s/users/%s", baseUrl, authenticatorId, username);
        Request req = new Request.Builder().url(url).delete().build();
        try (Response resp = httpClient.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("Error deleting user: " + resp);
            }
        }
    }

    // --------- ACL Management ---------

    /**
     * Create a new ACL rule.
     *
     * @param acl the ACL rule details
     * @return the created {@link AclDto}
     * @throws IOException if a network or serialization error occurs
     */
    public AclDto createAcl(AclDto acl) throws IOException {
        String url = String.format("%s/acl", baseUrl);
        String json = gson.toJson(acl);
        RequestBody body = RequestBody.create(json, MediaType.get("application/json"));
        Request req = new Request.Builder().url(url).post(body).build();
        try (Response resp = httpClient.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("Error creating ACL: " + resp);
            }
            return gson.fromJson(resp.body().string(), AclDto.class);
        }
    }

    /**
     * Retrieve an ACL rule by its ID.
     *
     * @param aclId the ID of the ACL rule
     * @return the {@link AclDto}
     * @throws IOException if a network or parsing error occurs or rule not found
     */
    public AclDto getAcl(int aclId) throws IOException {
        String url = String.format("%s/acl/%d", baseUrl, aclId);
        Request req = new Request.Builder().url(url).get().build();
        try (Response resp = httpClient.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("Error fetching ACL: " + resp);
            }
            return gson.fromJson(resp.body().string(), AclDto.class);
        }
    }

    /**
     * List all ACL rules.
     *
     * @return a list of {@link AclDto}
     * @throws IOException if a network or parsing error occurs
     */
    public List<AclDto> listAcls() throws IOException {
        String url = String.format("%s/acl", baseUrl);
        Request req = new Request.Builder().url(url).get().build();
        try (Response resp = httpClient.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("Error listing ACLs: " + resp);
            }
            Type listType = new TypeToken<List<AclDto>>() {}.getType();
            return gson.fromJson(resp.body().string(), listType);
        }
    }

    /**
     * Delete an ACL rule by its ID.
     *
     * @param aclId the ID of the ACL rule to delete
     * @throws IOException if a network error occurs or deletion fails
     */
    public void deleteAcl(int aclId) throws IOException {
        String url = String.format("%s/acl/%d", baseUrl, aclId);
        Request req = new Request.Builder().url(url).delete().build();
        try (Response resp = httpClient.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("Error deleting ACL: " + resp);
            }
        }
    }

    // --------- Client Management ---------

    /**
     * List all connected clients.
     *
     * @return a list of {@link ClientDto}
     * @throws IOException if a network or parsing error occurs
     */
    public List<ClientDto> listClients() throws IOException {
        String url = String.format("%s/clients", baseUrl);
        Request req = new Request.Builder().url(url).get().build();
        try (Response resp = httpClient.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("Error listing clients: " + resp);
            }
            Type listType = new TypeToken<List<ClientDto>>() {}.getType();
            return gson.fromJson(resp.body().string(), listType);
        }
    }

    /**
     * Disconnect a client by client ID.
     *
     * @param clientId the ID of the client to disconnect
     * @throws IOException if a network error occurs or disconnection fails
     */
    public void disconnectClient(String clientId) throws IOException {
        String url = String.format("%s/clients/%s", baseUrl, clientId);
        Request req = new Request.Builder().url(url).delete().build();
        try (Response resp = httpClient.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("Error disconnecting client: " + resp);
            }
        }
    }

    // --------- Session Management ---------

    /**
     * Retrieve session information for a client.
     *
     * @param clientId the ID of the client
     * @return the {@link SessionDto}
     * @throws IOException if a network or parsing error occurs or session not found
     */
    public SessionDto getSession(String clientId) throws IOException {
        String url = String.format("%s/sessions/%s", baseUrl, clientId);
        Request req = new Request.Builder().url(url).get().build();
        try (Response resp = httpClient.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("Error fetching session: " + resp);
            }
            return gson.fromJson(resp.body().string(), SessionDto.class);
        }
    }

    /**
     * Delete a session for a client.
     *
     * @param clientId the ID of the client
     * @throws IOException if a network error occurs or deletion fails
     */
    public void deleteSession(String clientId) throws IOException {
        String url = String.format("%s/sessions/%s", baseUrl, clientId);
        Request req = new Request.Builder().url(url).delete().build();
        try (Response resp = httpClient.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("Error deleting session: " + resp);
            }
        }
    }

    // --------- Subscription Management ---------

    /**
     * List subscriptions for a client.
     *
     * @param clientId the ID of the client
     * @return a list of {@link SubscriptionDto}
     * @throws IOException if a network or parsing error occurs
     */
    public List<SubscriptionDto> listSubscriptions(String clientId) throws IOException {
        String url = String.format("%s/subscriptions/%s", baseUrl, clientId);
        Request req = new Request.Builder().url(url).get().build();
        try (Response resp = httpClient.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("Error listing subscriptions: " + resp);
            }
            Type listType = new TypeToken<List<SubscriptionDto>>() {}.getType();
            return gson.fromJson(resp.body().string(), listType);
        }
    }

    // --------- Node Management ---------

    /**
     * List all cluster nodes.
     *
     * @return a list of {@link NodeDto}
     * @throws IOException if a network or parsing error occurs
     */
    public List<NodeDto> listNodes() throws IOException {
        String url = String.format("%s/nodes", baseUrl);
        Request req = new Request.Builder().url(url).get().build();
        try (Response resp = httpClient.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("Error listing nodes: " + resp);
            }
            Type listType = new TypeToken<List<NodeDto>>() {}.getType();
            return gson.fromJson(resp.body().string(), listType);
        }
    }

    // --------- Builder ---------

    /**
     * Create a new {@link Builder} for configuring and building an {@link EmqxSdkClient}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link EmqxSdkClient}, supporting base URL, authentication, and custom Gson.
     */
    public static class Builder {
        private String      baseUrl;
        private Interceptor authInterceptor;
        private Gson        gson;

        /**
         * Set the EMQX base URL (e.g., http://localhost:8080/api/v5).
         *
         * @param baseUrl the base URL
         * @return this builder
         */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * Override the default Gson instance.
         *
         * @param gson a pre-configured Gson
         * @return this builder
         */
        public Builder gson(Gson gson) {
            this.gson = gson;
            return this;
        }

        /**
         * Use HTTP Basic Auth with API key and secret.
         *
         * @param apiKey    the API key
         * @param apiSecret the API secret
         * @return this builder
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
         *
         * @param token the JWT token
         * @return this builder
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

        /**
         * Build the {@link EmqxSdkClient}.
         *
         * @return a configured {@link EmqxSdkClient}
         * @throws IllegalStateException if baseUrl is not set
         */
        public EmqxSdkClient build() {
            if (baseUrl == null || baseUrl.isBlank()) {
                throw new IllegalStateException("baseUrl must be set");
            }
            return new EmqxSdkClient(this);
        }
    }
}