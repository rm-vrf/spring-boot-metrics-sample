package org.springframework.boot.actuate.metrics.elasticsearch;

import java.util.Map;

public class ElasticsearchData {

	private ElasticsearchName name;

	private Long timestamp;

	private Double value;

	protected ElasticsearchData() {
		this.name = new ElasticsearchName();
	}

	public ElasticsearchData(String metric, Double value) {
		this(metric, value, System.currentTimeMillis());
	}

	public ElasticsearchData(String metric, Double value, Long timestamp) {
		this(new ElasticsearchName(metric), value, timestamp);
	}

	public ElasticsearchData(ElasticsearchName name, Double value, Long timestamp) {
		this.name = name;
		this.value = value;
		this.timestamp = timestamp;
	}

	public String getMetric() {
		return this.name.getMetric();
	}

	public void setMetric(String metric) {
		this.name.setMetric(metric);
	}

	public Long getTimestamp() {
		return this.timestamp;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	public Double getValue() {
		return this.value;
	}

	public void setValue(Double value) {
		this.value = value;
	}

	public Map<String, String> getTags() {
		return this.name.getTags();
	}

	public void setTags(Map<String, String> tags) {
		this.name.setTags(tags);
	}
}
