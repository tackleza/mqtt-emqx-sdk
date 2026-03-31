package com.apidech.sdk.emqxsdk;

/**
 * Data Transfer Object for cluster nodes.
 */
public class NodeDto {

	public String  node;
	public String  version;
	public String  status;
	public Long    uptime_ms;
	public Integer memory_total;
	public Integer memory_used;
	public Integer process_memory;
	public Integer max_fds;
	public Integer threads;
	public Integer socks_total;
	public Boolean live_connections;

	// --- Builder support ---

	public NodeDto() {}

	public static Builder builder() { return new Builder(); }

	public static class Builder {
		private final NodeDto dto = new NodeDto();

		public Builder node(String v)               { dto.node             = v;  return this; }
		public Builder version(String v)           { dto.version          = v;  return this; }
		public Builder status(String v)            { dto.status           = v;  return this; }
		public Builder uptime_ms(Long v)           { dto.uptime_ms        = v;  return this; }
		public Builder memory_total(Integer v)     { dto.memory_total     = v;  return this; }
		public Builder memory_used(Integer v)      { dto.memory_used      = v;  return this; }
		public Builder process_memory(Integer v)   { dto.process_memory   = v;  return this; }
		public Builder max_fds(Integer v)          { dto.max_fds          = v;  return this; }
		public Builder threads(Integer v)          { dto.threads          = v;  return this; }
		public Builder socks_total(Integer v)      { dto.socks_total      = v;  return this; }
		public Builder live_connections(Boolean v) { dto.live_connections = v;  return this; }

		public NodeDto build() { return dto; }
	}

	@Override
	public String toString() {
		return "NodeDto{" +
				"node='" + node + '\'' +
				", version='" + version + '\'' +
				", status='" + status + '\'' +
				", uptime_ms=" + uptime_ms +
				", memory_used=" + memory_used +
				"/" + memory_total +
				", threads=" + threads +
				", live_connections=" + live_connections +
				'}';
	}
}
