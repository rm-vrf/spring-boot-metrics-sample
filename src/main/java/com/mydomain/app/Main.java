package com.mydomain.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.ExportMetricWriter;
import org.springframework.boot.actuate.metrics.elasticsearch.ElasticsearchMetricWriter;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class Main {

	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		return builder.setConnectTimeout(10000).setReadTimeout(10000).build();
	}
	
	@Bean
	@ConfigurationProperties("metrics.export")
	@ExportMetricWriter
	public MetricWriter metricWriter() {
		return new ElasticsearchMetricWriter();
	}
	
	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);
	}
	
}
