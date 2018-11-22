package com.mydomain.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import org.apache.tomcat.util.http.fileupload.IOUtils;
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
	public void copyFile() throws Exception {
		
		long size = 0L;
		int count = 0;
		try {
			String tmp = System.getProperty("java.io.tmpdir");
			LOG.debug("copy file: {}", tmp);
			File file = new File(tmp);
			long beginTime = System.currentTimeMillis();
			File[] children = file.listFiles();
			for (File child : children) {
				LOG.debug("child: {}", child.getName());
				if (child.isFile() && new Random().nextInt(1000) > 600) {
					File newFile = new File("/dev/null");
					copyFile(child, newFile);
					size += child.length();
					count ++;
				}
			}
			Thread.sleep(new Random().nextInt(3000));
			long costTime = System.currentTimeMillis() - beginTime;
			
			LOG.info("copy.file.time: {}, copy.file.count: {}, copy.file.size: {}", costTime, count, size);
			
			gaugeService.submit("copy.file.time", costTime);
			gaugeService.submit("copy.file.count", count);
			gaugeService.submit("copy.file.size", size);
			counterService.increment("copy.file.ok");
		} catch (Exception e) {
			counterService.increment("copy.file.error");
			throw e;
		}
	}
	
	public void baidu() throws InterruptedException {
		String html = restTemplate.getForObject("https://www.baidu.com", String.class);
		LOG.debug("html: {}", html);
	}
	
	private void copyFile(File srcFile, File newFile) throws IOException {
		if (!newFile.exists()) {
			newFile.createNewFile();
		}
		
		InputStream in = null;
		OutputStream out = null;
		try {
			in = new FileInputStream(srcFile);
			out = new FileOutputStream(newFile);
			IOUtils.copyLarge(in, out);
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
		}
	}
}
