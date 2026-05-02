package com.apidech.sdk.emqxsdk;

/**
 * Data Transfer Object for users.
 *
 * <p>EMQX uses {@code user_id} as the identifier field (not "username").
 * The {@code username} field is an alias kept for deserialization compatibility
 * only — prefer {@link #user_id} when building requests.</p>
 */
public class UserDto {

	// --- Request fields (used when creating/updating users) ---
	public String user_id;   // EMQX user identifier
	public String password;

	// --- Response fields ---
	public Boolean is_superuser;
	public Long   created_at;  // Unix epoch seconds (set by EMQX on create)

	// Alias: some EMQX contexts return "username" instead of "user_id"
	public String username;

	/**
	 * Returns the user identifier, falling back to {@code username} if null.
	 * Use this when you need a displayable identifier.
	 */
	public String getUserId() {
		return user_id != null ? user_id : username;
	}

	// --- Builder support ---

	public UserDto() {}

	public UserDto(String user_id, String password) {
		this.user_id  = user_id;
		this.password = password;
	}

	public static Builder builder() { return new Builder(); }

	public static class Builder {
		private final UserDto dto = new UserDto();

		public Builder user_id(String v)     { dto.user_id  = v;  return this; }
		public Builder password(String v)    { dto.password = v;  return this; }
		public Builder is_superuser(Boolean v) { dto.is_superuser = v; return this; }
		public Builder created_at(Long v)    { dto.created_at = v;  return this; }
		public Builder username(String v)    { dto.username = v;  return this; }

		public UserDto build() { return dto; }
	}

	@Override
	public String toString() {
		return "UserDto{" +
				"user_id='" + user_id + '\'' +
				", username='" + username + '\'' +
				", is_superuser=" + is_superuser +
				", created_at=" + created_at +
				'}';
	}
}
