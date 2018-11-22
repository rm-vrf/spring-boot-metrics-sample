package org.springframework.boot.actuate.metrics.elasticsearch;

public interface ElasticsearchNamingStrategy {

	ElasticsearchName getName(String metricName);

}
