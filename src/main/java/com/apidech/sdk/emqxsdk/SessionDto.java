package com.apidech.sdk.emqxsdk;

/**
 * Data Transfer Object for MQTT sessions.
 */
public class SessionDto {
	
	public boolean existing; // true if an existing session was resumed
	public long    expiry;   // session expiry interval in seconds
}
