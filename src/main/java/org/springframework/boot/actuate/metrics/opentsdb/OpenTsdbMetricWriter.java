package org.springframework.boot.actuate.metrics.opentsdb;

import org.springframework.boot.actuate.metrics.writer.Delta;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;

public class OpenTsdbMetricWriter extends OpenTsdbGaugeWriter implements MetricWriter {

	@Override
	public void increment(Delta<?> delta) {
		set(delta);
	}

	@Override
	public void reset(String metricName) {
		// pass
	}

}
