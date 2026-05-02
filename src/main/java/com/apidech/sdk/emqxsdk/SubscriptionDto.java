package com.apidech.sdk.emqxsdk;

/**
 * Data Transfer Object for MQTT subscriptions.
 */
public class SubscriptionDto {

	public String  clientid;
	public String  topic;
	public Integer qos;
	public String  share; // share group name, null for non-shared subscriptions

	// --- Builder support ---

	public SubscriptionDto() {}

	public SubscriptionDto(String clientid, String topic, Integer qos) {
		this.clientid = clientid;
		this.topic    = topic;
		this.qos      = qos;
	}

	public static Builder builder() { return new Builder(); }

	public static class Builder {
		private final SubscriptionDto dto = new SubscriptionDto();

		public Builder clientid(String v)  { dto.clientid = v;  return this; }
		public Builder topic(String v)    { dto.topic    = v;  return this; }
		public Builder qos(Integer v)     { dto.qos      = v;  return this; }
		public Builder share(String v)   { dto.share    = v;  return this; }

		public SubscriptionDto build() { return dto; }
	}

	@Override
	public String toString() {
		return "SubscriptionDto{" +
				"clientid='" + clientid + '\'' +
				", topic='" + topic + '\'' +
				", qos=" + qos +
				", share=" + share +
				'}';
	}
}
