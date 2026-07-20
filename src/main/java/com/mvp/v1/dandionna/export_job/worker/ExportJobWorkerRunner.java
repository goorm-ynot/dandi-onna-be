package com.mvp.v1.dandionna.export_job.worker;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
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
	private static final long RETRY_DELAY_MS = 1000L;
	private static final long SHUTDOWN_TIMEOUT_SECONDS = 5L;

	private final ExportJobDispatchWorker exportJobDispatchWorker;
	private final ExecutorService executorService = Executors.newSingleThreadExecutor();
	private final AtomicBoolean shutdownInitiated = new AtomicBoolean(false);
	private volatile boolean running = true;

	@Override
	public void run(ApplicationArguments args) {
		executorService.submit(() -> {
			log.info("🚀 Export Job Worker Started!");
			while (running) {
				try {
					exportJobDispatchWorker.processOnce();
				} catch (Exception e) {
					if (!running) {
						break;
					}
					log.error("❌ Export Worker Error: ", e);
					if (!sleepBeforeRetry()) {
						return;
					}
				}
			}
		});
	}

	@PreDestroy
	public void stop() {
		initiateShutdown();
		awaitShutdown();
		log.info("🛑 Export Job Worker Stopped.");
	}

	@EventListener(ContextClosedEvent.class)
	public void onContextClosed() {
		initiateShutdown();
	}

	private boolean sleepBeforeRetry() {
		try {
			Thread.sleep(RETRY_DELAY_MS);
			return true;
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	private void awaitShutdown() {
		try {
			if (!executorService.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
				log.warn("Export worker executor did not terminate within {} seconds.", SHUTDOWN_TIMEOUT_SECONDS);
			}
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
	}

	private void initiateShutdown() {
		if (shutdownInitiated.compareAndSet(false, true)) {
			this.running = false;
			executorService.shutdownNow();
		}
	}
}
