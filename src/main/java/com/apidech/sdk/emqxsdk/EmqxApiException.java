package com.apidech.sdk.emqxsdk;

import java.io.IOException;

/**
 * Exception thrown when an EMQX API call fails.
 * Carries the HTTP status code and the parsed error message from EMQX.
 */
public class EmqxApiException extends IOException {

	private static final long serialVersionUID = 3854815691844028975L;
	private final int    httpStatus;
	private final String emqxMessage;

	public EmqxApiException(int httpStatus, String emqxMessage) {
		super(String.format("EMQX API error %d: %s", httpStatus, emqxMessage));
		this.httpStatus   = httpStatus;
		this.emqxMessage  = emqxMessage;
	}

	public int    httpStatus()  { return httpStatus; }
	public String emqxMessage() { return emqxMessage; }
}
