package com.apidech.sdk.emqxsdk;

import java.util.List;

/**
 * Generic wrapper for paginated EMQX list responses.
 *
 * <p>EMQX v5 returns list results as {@code {"data": [...], "meta": {...}}}.
 * Use {@link #data()} to get the actual list.</p>
 *
 * @param <T> the element type
 */
public class PaginatedResponse<T> {

	public List<T> data;
	public Meta    meta;

	public static class Meta {
		public int    count;
		public int    limit;
		public int    page;
		public boolean hasnext;
	}

	public List<T> data() { return data; }
}
