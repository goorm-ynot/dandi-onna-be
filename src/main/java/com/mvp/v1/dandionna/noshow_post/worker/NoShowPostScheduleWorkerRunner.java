package com.mvp.v1.dandionna.noshow_post.worker;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NoShowPostScheduleWorkerRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(NoShowPostScheduleWorkerRunner.class);
	private static final long RETRY_DELAY_MS = 1000L;
	private static final long SHUTDOWN_TIMEOUT_SECONDS = 5L;

	private final NoShowPostScheduleDispatchWorker dispatchWorker;
	private final ExecutorService executorService = Executors.newSingleThreadExecutor();
	private final AtomicBoolean shutdownInitiated = new AtomicBoolean(false);
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
					if (!sleepSafely(Math.max(pollIntervalMs, 100))) {
						return;
					}
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					if (!running) {
						break;
					}
					return;
				} catch (Exception e) {
					if (!running) {
						break;
					}
					log.error("❌ NoShow Schedule Worker Error: ", e);
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
		log.info("🛑 NoShow Schedule Worker Stopped.");
	}

	@EventListener(ContextClosedEvent.class)
	public void onContextClosed() {
		initiateShutdown();
	}

	private boolean sleepSafely(long sleepMs) throws InterruptedException {
		if (!running) {
			return false;
		}
		Thread.sleep(sleepMs);
		return true;
	}

	private boolean sleepBeforeRetry() {
		try {
			return sleepSafely(RETRY_DELAY_MS);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	private void awaitShutdown() {
		try {
			if (!executorService.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
				log.warn("No-show schedule worker executor did not terminate within {} seconds.", SHUTDOWN_TIMEOUT_SECONDS);
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
