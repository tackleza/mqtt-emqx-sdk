package com.apidech.sdk.emqxsdk;

/**
 * Data Transfer Object for connected clients.
 */
public class ClientDto {

	public String  clientid;
	public String  username;
	public String  ip_address;
	public Integer port;
	public Boolean is_bridge;
	public String  proto_ver;
	public String  conn_state;
	public Long    connected_at;  // Unix epoch seconds
	public Integer keepalive;
	public Long    recv_cnt;
	public Long    send_cnt;

	// --- Builder support ---

	public ClientDto() {}

	public static Builder builder() { return new Builder(); }

	public static class Builder {
		private final ClientDto dto = new ClientDto();

		public Builder clientid(String v)     { dto.clientid    = v;  return this; }
		public Builder username(String v)     { dto.username    = v;  return this; }
		public Builder ip_address(String v)  { dto.ip_address  = v;  return this; }
		public Builder port(Integer v)        { dto.port        = v;  return this; }
		public Builder is_bridge(Boolean v)  { dto.is_bridge   = v;  return this; }
		public Builder proto_ver(String v)    { dto.proto_ver   = v;  return this; }
		public Builder conn_state(String v)  { dto.conn_state  = v;  return this; }
		public Builder connected_at(Long v)  { dto.connected_at = v; return this; }
		public Builder keepalive(Integer v)   { dto.keepalive   = v;  return this; }
		public Builder recv_cnt(Long v)       { dto.recv_cnt    = v;  return this; }
		public Builder send_cnt(Long v)       { dto.send_cnt    = v;  return this; }

		public ClientDto build() { return dto; }
	}

	@Override
	public String toString() {
		return "ClientDto{" +
				"clientid='" + clientid + '\'' +
				", username='" + username + '\'' +
				", ip_address='" + ip_address + '\'' +
				", port=" + port +
				", conn_state='" + conn_state + '\'' +
				", proto_ver='" + proto_ver + '\'' +
				'}';
	}
}
