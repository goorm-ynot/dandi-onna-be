package com.mvp.v1.dandionna.noshow_post.worker;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NoShowPostScheduleWorkerRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(NoShowPostScheduleWorkerRunner.class);

	private final NoShowPostScheduleDispatchWorker dispatchWorker;
	private final ExecutorService executorService = Executors.newSingleThreadExecutor();
	private volatile boolean running = true;

	@Value("${app.noshow.schedule.poll-interval-ms:1000}")
	private long pollIntervalMs;

	@Override
	public void run(ApplicationArguments args) {
		executorService.submit(() -> {
			log.info("🚀 NoShow Schedule Worker Started!");
			while (running) {
				try {
					dispatchWorker.processOnce();
					Thread.sleep(Math.max(pollIntervalMs, 100));
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					return;
				} catch (Exception e) {
					log.error("❌ NoShow Schedule Worker Error: ", e);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						return;
					}
				}
			}
		});
	}

	@PreDestroy
	public void stop() {
		this.running = false;
		executorService.shutdown();
		log.info("🛑 NoShow Schedule Worker Stopped.");
	}
}

