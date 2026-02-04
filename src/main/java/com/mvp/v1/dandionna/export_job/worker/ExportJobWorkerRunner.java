package com.mvp.v1.dandionna.export_job.worker;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;

/**
 * 애플리케이션 시작 시 ExportJob 워커를 백그라운드로 구동한다.
 */
@Component
@RequiredArgsConstructor
public class ExportJobWorkerRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(ExportJobWorkerRunner.class);

	private final ExportJobDispatchWorker exportJobDispatchWorker;
	private final ExecutorService executorService = Executors.newSingleThreadExecutor();
	private volatile boolean running = true;

	@Override
	public void run(ApplicationArguments args) {
		executorService.submit(() -> {
			log.info("🚀 Export Job Worker Started!");
			while (running) {
				try {
					exportJobDispatchWorker.processOnce();
				} catch (Exception e) {
					log.error("❌ Export Worker Error: ", e);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
					}
				}
			}
		});
	}

	@PreDestroy
	public void stop() {
		this.running = false;
		executorService.shutdown();
		log.info("🛑 Export Job Worker Stopped.");
	}
}
