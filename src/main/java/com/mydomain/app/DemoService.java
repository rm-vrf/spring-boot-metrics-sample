package com.mydomain.app;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class DemoService {

	private static final Logger LOG = LoggerFactory.getLogger(DemoService.class);
	
	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private GaugeService gaugeService;
	
	@Autowired
	private CounterService counterService;
	
	@Scheduled(fixedDelay = 3000)
	public void sleep() throws Exception {
		int n = new Random().nextInt(3000);
		Thread.sleep(n);
		
		gaugeService.submit("sleep.time", n);
		counterService.increment("sleep.count");
	}
	
	public void baidu() throws InterruptedException {
		String html = restTemplate.getForObject("https://www.baidu.com", String.class);
		LOG.debug("html: {}", html);
	}

}
