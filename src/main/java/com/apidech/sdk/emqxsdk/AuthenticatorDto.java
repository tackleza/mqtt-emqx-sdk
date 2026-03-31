package com.apidech.sdk.emqxsdk;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object for EMQX authenticator descriptors.
 *
 * <p>Used with {@link EmqxSdkClient#listAuthenticators()} and
 * {@link EmqxSdkClient#createBuiltInDbAuthenticator(String, String, int)}.</p>
 */
public class AuthenticatorDto {

	public String id;
	public String mechanism;
	public String backend;
	public Boolean enable;

	// --- Built-in DB specific ---
	public PasswordHashAlgorithm password_hash_algorithm;

	public static class PasswordHashAlgorithm {
		public String name;      // e.g. "bcrypt", "sha256", "md5"
		public Integer salt_rounds;
	}

	// -------------------------------------------------------------------------
	// Request body for creating a built-in DB authenticator
	// -------------------------------------------------------------------------

	/**
	 * Request payload for creating a password-based built-in-database authenticator.
	 */
	public static class BuiltInDb {
		@SerializedName("mechanism") public final String mechanism = "password_based";
		@SerializedName("backend")  public final String backend  = "built_in_database";
		public String name;
		public PasswordHashAlgorithm password_hash_algorithm;

		public BuiltInDb(String name, String hashAlgo, int saltRounds) {
			this.name = name;
			this.password_hash_algorithm = new PasswordHashAlgorithm();
			this.password_hash_algorithm.name = hashAlgo;
			this.password_hash_algorithm.salt_rounds = saltRounds;
		}
	}

	@Override
	public String toString() {
		return "AuthenticatorDto{" +
				"id='" + id + '\'' +
				", mechanism='" + mechanism + '\'' +
				", backend='" + backend + '\'' +
				", enable=" + enable +
				'}';
	}
}
