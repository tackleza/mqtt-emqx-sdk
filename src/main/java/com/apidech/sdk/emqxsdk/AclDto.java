package com.apidech.sdk.emqxsdk;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object for ACL rules.
 */
public class AclDto {
	public int       id;
	public String    username;
	public String    topic;
	public AclAction action; // publish or subscribe
	public boolean   allow;

    /**
     * Actions permitted by the ACL rule.
     */
    public enum AclAction {
        @SerializedName("publish") PUBLISH,
        @SerializedName("subscribe") SUBSCRIBE
    }
}
