package com.apidech.sdk.emqxsdk;

/**
 * Enumeration of built-in EMQX authenticator IDs.
 * <p>
 * Each constant corresponds to a default EMQX authentication mechanism.
 */
public enum AuthenticatorId {

    /**
     * Default authenticator: Username/password against EMQX built-in database.
     */
    DEFAULT("password_based:built_in_database"),

    /**
     * Username/password against EMQX built-in database.
     */
    PASSWORD_BASED_BUILT_IN_DATABASE("password_based:built_in_database"),

    /**
     * Username/password against MySQL backend.
     */
    PASSWORD_BASED_MYSQL("password_based:mysql"),

    /**
     * Username/password against PostgreSQL backend.
     */
    PASSWORD_BASED_POSTGRESQL("password_based:postgresql"),

    /**
     * Username/password against MongoDB backend.
     */
    PASSWORD_BASED_MONGODB("password_based:mongodb"),

    /**
     * Username/password against Redis backend.
     */
    PASSWORD_BASED_REDIS("password_based:redis"),

    /**
     * Username/password against LDAP backend.
     */
    PASSWORD_BASED_LDAP("password_based:ldap"),

    /**
     * Username/password against HTTP service.
     */
    PASSWORD_BASED_HTTP("password_based:http"),

    /**
     * JSON Web Token authentication.
     */
    JWT("jwt"),

    /**
     * SCRAM against EMQX built-in database.
     */
    SCRAM_BUILT_IN_DATABASE("scram:built_in_database"),

    /**
     * SCRAM against HTTP service.
     */
    SCRAM_HTTP("scram:http"),

    /**
     * Kerberos GSSAPI authentication.
     */
    GSSAPI("gssapi"),

    /**
     * Client info based rule authentication.
     */
    CLIENT_INFO("client_info");

    private final String id;

    AuthenticatorId(String id) {
        this.id = id;
    }

    /**
     * Returns the string ID used by EMQX for this authenticator.
     *
     * @return the EMQX authenticator ID
     */
    @Override
    public String toString() {
        return id;
    }
}