package org.springframework.boot.actuate.metrics.elasticsearch;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.writer.Delta;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

public class ElasticsearchMetricWriter implements MetricWriter {
	
	private static final ThreadLocal<DateFormat> TIME_FORMAT = new ThreadLocal<DateFormat>() {
		public DateFormat get() {
			return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		};	
	};
	
	private static final int DEFAULT_CONNECT_TIMEOUT = 10000;
	
	private static final int DEFAULT_READ_TIMEOUT = 30000;
	
	private static final Log logger = LogFactory.getLog(ElasticsearchMetricWriter.class);
	
	private RestOperations restTemplate;
	
	private String url = "http://localhost:9200";
	
	private String indexName = "metrics-{yyyy-MM-dd}";
	
	private String typeName = "data";

	private int bufferSize = 64;

	private final List<ElasticsearchData> buffer = new ArrayList<ElasticsearchData>(
			this.bufferSize);
	
	private ElasticsearchNamingStrategy namingStrategy = new DefaultElasticsearchNamingStrategy();

	public ElasticsearchMetricWriter() {
		this(DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
	}

	public ElasticsearchMetricWriter(int connectTimeout, int readTimeout) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(connectTimeout);
		requestFactory.setReadTimeout(readTimeout);
		this.restTemplate = new RestTemplate(requestFactory);
	}
	
	public RestOperations getRestTemplate() {
		return this.restTemplate;
	}

	public void setRestTemplate(RestOperations restTemplate) {
		this.restTemplate = restTemplate;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	//public void setMediaType(MediaType mediaType) {
	//	this.mediaType = mediaType;
	//}

	public void setNamingStrategy(ElasticsearchNamingStrategy namingStrategy) {
		this.namingStrategy = namingStrategy;
	}

	@Override
	public void set(Metric<?> value) {
		ElasticsearchData data = new ElasticsearchData(this.namingStrategy.getName(value.getName()),
				value.getValue().doubleValue(), value.getTimestamp().getTime());
		synchronized (this.buffer) {
			this.buffer.add(data);
			if (this.buffer.size() >= this.bufferSize) {
				flush();
			}
		}
	}

	@SuppressWarnings("rawtypes")
	public void flush() {
		List<ElasticsearchData> snapshot = getBufferSnapshot();
		if (snapshot.isEmpty()) {
			return;
		}
		
		StringBuilder data = buildData(snapshot);
		logger.debug(data.toString());
		
		ResponseEntity<Map> response = this.restTemplate.postForEntity(this.url + "/_bulk",
				new HttpEntity<String>(data.toString()), Map.class);
		if (!response.getStatusCode().is2xxSuccessful()) {
			logger.warn("Cannot write metrics (discarded " + snapshot.size() 
				+ " values): " + response.getBody());
		}
	}

	@Override
	public void increment(Delta<?> delta) {
		set(delta);
	}

	@Override
	public void reset(String metricName) {
		// pass
	}

	private StringBuilder buildData(List<ElasticsearchData> datas) {
		String title = String.format("{\"index\":{\"_index\":\"%s\",\"_type\":\"%s\"}}\n", 
				buildIndex(indexName),
				buildIndex(typeName));
		
		StringBuilder s = new StringBuilder(256 * datas.size());
		
		for (ElasticsearchData data : datas) {
			String name = data.getMetric();
			Number value = data.getValue();
			String timestamp = TIME_FORMAT.get().format(new Date(data.getTimestamp()));
			String tags = JSONObject.toJSONString(data.getTags());
			
			s.append(title)
				.append("{\"metric\":\"")
				.append(name)
				.append("\",\"timestamp\":\"")
				.append(timestamp)
				.append("\",\"value\":")
				.append(value)
				.append(",\"tags\":")
				.append(tags)
				.append("}\n");
		}
		
		return s;
	}
	
	private String buildIndex(String s) {
		int pos1 = s.indexOf('{');
		int pos2 = s.indexOf('}');
		
		if (pos1 >=0 && pos2 > pos1) {
			String format = s.substring(pos1 + 1, pos2);
			String date = new SimpleDateFormat(format).format(new Date());
			return s.substring(0, pos1) + date + s.substring(pos2 + 1);
		} else {
			return s;
		}
	}

	private List<ElasticsearchData> getBufferSnapshot() {
		synchronized (this.buffer) {
			if (this.buffer.isEmpty()) {
				return Collections.emptyList();
			}
			List<ElasticsearchData> snapshot = new ArrayList<ElasticsearchData>(this.buffer);
			this.buffer.clear();
			return snapshot;
		}
	}
	
}
