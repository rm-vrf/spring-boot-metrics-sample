package org.springframework.boot.actuate.metrics.elasticsearch;

import java.util.LinkedHashMap;
import java.util.Map;

public class ElasticsearchName {

	private String metric;

	private Map<String, String> tags = new LinkedHashMap<String, String>();

	protected ElasticsearchName() {
	}

	public ElasticsearchName(String metric) {
		this.metric = metric;
	}

	public String getMetric() {
		return this.metric;
	}

	public void setMetric(String metric) {
		this.metric = metric;
	}

	public Map<String, String> getTags() {
		return this.tags;
	}

	public void setTags(Map<String, String> tags) {
		this.tags.putAll(tags);
	}

	public void tag(String name, String value) {
		this.tags.put(name, value);
	}
}
