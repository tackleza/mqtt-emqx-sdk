package com.apidech.sdk.emqxsdk;

import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for MQTT sessions.
 */
public class SessionDto {

	public String           clientid;
	public Map<String, Object> waiting_acks;
	public List<SubscriptionDto> subscriptions;
	public List<String>     inflight;
	public Integer         max_inflight;
	public Integer         mqueue_len;
	public Integer         mqueue_len_max;
	public Long             dropped;
	public Boolean          forbidden;

	// legacy / simplified fields (kept for compatibility)
	public Boolean         existing; // true if an existing session was resumed
	public Long            expiry;   // session expiry interval in seconds

	// --- Builder support ---

	public SessionDto() {}

	public static Builder builder() { return new Builder(); }

	public static class Builder {
		private final SessionDto dto = new SessionDto();

		public Builder clientid(String v)              { dto.clientid       = v;  return this; }
		public Builder waiting_acks(Map<String, Object> v) { dto.waiting_acks  = v;  return this; }
		public Builder subscriptions(List<SubscriptionDto> v) { dto.subscriptions = v; return this; }
		public Builder inflight(List<String> v)        { dto.inflight       = v;  return this; }
		public Builder max_inflight(Integer v)         { dto.max_inflight   = v;  return this; }
		public Builder mqueue_len(Integer v)           { dto.mqueue_len     = v;  return this; }
		public Builder mqueue_len_max(Integer v)       { dto.mqueue_len_max = v;  return this; }
		public Builder dropped(Long v)                 { dto.dropped        = v;  return this; }
		public Builder forbidden(Boolean v)            { dto.forbidden      = v;  return this; }
		public Builder existing(Boolean v)             { dto.existing       = v;  return this; }
		public Builder expiry(Long v)                  { dto.expiry         = v;  return this; }

		public SessionDto build() { return dto; }
	}

	@Override
	public String toString() {
		return "SessionDto{" +
				"clientid='" + clientid + '\'' +
				", existing=" + existing +
				", expiry=" + expiry +
				", max_inflight=" + max_inflight +
				", mqueue_len=" + mqueue_len +
				", forbidden=" + forbidden +
				", dropped=" + dropped +
				'}';
	}
}
