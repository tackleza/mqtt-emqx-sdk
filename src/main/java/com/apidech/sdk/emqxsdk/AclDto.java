package com.apidech.sdk.emqxsdk;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object for ACL rules.
 */
public class AclDto {

	public Integer id;
	public String  username;
	public String  topic;
	public AclAction action; // publish or subscribe
	public Boolean  allow;

	// --- Bulk creation response ---
	public List<AclDto> rules;

	/**
	 * Actions permitted by the ACL rule.
	 */
	public enum AclAction {
		@SerializedName("publish")   PUBLISH,
		@SerializedName("subscribe") SUBSCRIBE
	}

	// --- Builder support ---

	public AclDto() {}

	public AclDto(String username, String topic, AclAction action, boolean allow) {
		this.username = username;
		this.topic    = topic;
		this.action   = action;
		this.allow    = allow;
	}

	public static Builder builder() { return new Builder(); }

	public static class Builder {
		private final AclDto dto = new AclDto();

		public Builder id(Integer v)              { dto.id       = v;  return this; }
		public Builder username(String v)         { dto.username = v;  return this; }
		public Builder topic(String v)            { dto.topic    = v;  return this; }
		public Builder action(AclAction v)         { dto.action   = v;  return this; }
		public Builder allow(Boolean v)           { dto.allow    = v;  return this; }
		public Builder rules(List<AclDto> v)      { dto.rules    = v;  return this; }

		public AclDto build() { return dto; }
	}

	@Override
	public String toString() {
		return "AclDto{" +
				"id=" + id +
				", username='" + username + '\'' +
				", topic='" + topic + '\'' +
				", action=" + action +
				", allow=" + allow +
				'}';
	}
}
