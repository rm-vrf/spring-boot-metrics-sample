package org.springframework.boot.actuate.metrics.elasticsearch;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.ObjectUtils;

public class DefaultElasticsearchNamingStrategy implements ElasticsearchNamingStrategy {

	/**
	 * The domain key.
	 */
	public static final String DOMAIN_KEY = "domain";

	/**
	 * The process key.
	 */
	public static final String PROCESS_KEY = "process";

	/**
	 * Tags to apply to every metric. Open TSDB requires at least one tag, so a "prefix"
	 * tag is added for you by default.
	 */
	private Map<String, String> tags = new LinkedHashMap<String, String>();

	private Map<String, ElasticsearchName> cache = new HashMap<String, ElasticsearchName>();

	public DefaultElasticsearchNamingStrategy() {
		this.tags.put(DOMAIN_KEY, "org.springframework.metrics");
		this.tags.put(PROCESS_KEY, ObjectUtils.getIdentityHexString(this));
	}

	public void setTags(Map<String, String> staticTags) {
		this.tags.putAll(staticTags);
	}

	@Override
	public ElasticsearchName getName(String name) {
		if (this.cache.containsKey(name)) {
			return this.cache.get(name);
		}
		ElasticsearchName value = new ElasticsearchName(name);
		value.setTags(this.tags);
		this.cache.put(name, value);
		return value;
	}
}
