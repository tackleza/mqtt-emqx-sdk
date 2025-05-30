package com.apidech.sdk.emqxsdk;

/**
 * Data Transfer Object for MQTT subscriptions.
 */
public class SubscriptionDto {
	
	public String clientid;
    public String topic;
    public int    qos;
}
