package com.mvp.v1.dandionna.export_job.worker;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mvp.v1.dandionna.export_job.entity.ExportJob;
import com.mvp.v1.dandionna.export_job.entity.ExportJobStatus;
import com.mvp.v1.dandionna.export_job.repository.ExportJobRepository;
import com.mvp.v1.dandionna.noshow_order.entity.NoShowOrder;
import com.mvp.v1.dandionna.noshow_order.entity.NoShowOrderItem;
import com.mvp.v1.dandionna.noshow_order.repository.NoShowOrderItemRepository;
import com.mvp.v1.dandionna.noshow_order.repository.NoShowOrderRepository;
import com.mvp.v1.dandionna.s3.service.UploadService;

/**
 * Redis Stream 기반 엑셀 내보내기 워커.
 * - Stream key: export:queue
 * - 엑셀 생성 후 S3/MinIO 업로드, DB 상태 업데이트 수행
 */
@Component
public class ExportJobDispatchWorker {

	private static final Logger log = LoggerFactory.getLogger(ExportJobDispatchWorker.class);
	private static final String STREAM_KEY = "export:queue";
	private static final String GROUP = "export-workers";
	private static final String CONSUMER = "worker-1";
	private static final ZoneId ZONE_KST = ZoneId.of("Asia/Seoul");
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
	private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
		.withZone(ZONE_KST);
	private static final String CONTENT_TYPE_XLSX =
		"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	private final StringRedisTemplate redisTemplate;
	private final ExportJobRepository exportJobRepository;
	private final NoShowOrderRepository noShowOrderRepository;
	private final NoShowOrderItemRepository noShowOrderItemRepository;
	private final UploadService uploadService;

	@Value("${app.export.file-ttl-days:7}")
	private long fileTtlDays;

	public ExportJobDispatchWorker(StringRedisTemplate redisTemplate,
		ExportJobRepository exportJobRepository,
		NoShowOrderRepository noShowOrderRepository,
		NoShowOrderItemRepository noShowOrderItemRepository,
		UploadService uploadService) {
		this.redisTemplate = redisTemplate;
		this.exportJobRepository = exportJobRepository;
		this.noShowOrderRepository = noShowOrderRepository;
		this.noShowOrderItemRepository = noShowOrderItemRepository;
		this.uploadService = uploadService;
		ensureGroup();
	}

	private void ensureGroup() {
		try {
			redisTemplate.opsForStream().createGroup(STREAM_KEY, GROUP);
		} catch (Exception ignored) {
			// group already exists or stream absent; will be created on first add
		}
	}

	@Transactional
	public void processOnce() {
		List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
			org.springframework.data.redis.connection.stream.Consumer.from(GROUP, CONSUMER),
			org.springframework.data.redis.connection.stream.StreamReadOptions.empty().count(5).block(Duration.ofSeconds(2)),
			StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed())
		);
		if (records == null || records.isEmpty()) {
			return;
		}

		for (MapRecord<String, Object, Object> record : records) {
			ExportPayload payload = null;
			ExportJob job = null;
			try {
				Map<Object, Object> values = record.getValue();
				log.debug("Export record received id={} values={}", record.getId(), values);
				payload = ExportPayload.from(values);
				if (payload == null) {
					log.warn("Export record ignored (missing jobId). recordId={}", record.getId());
					continue;
				}
				job = exportJobRepository.findById(payload.jobId()).orElse(null);
				if (job == null || !job.isActive()) {
					log.warn("Export job not found or inactive. jobId={}", payload.jobId());
					continue;
				}
				if (job.getStatus() == ExportJobStatus.DONE || job.getStatus() == ExportJobStatus.EXPIRED) {
					log.debug("Export job already finished. jobId={} status={}", job.getId(), job.getStatus());
					continue;
				}
				log.debug("Export job start. jobId={} status={}", job.getId(), job.getStatus());
				handle(job, payload.attempt());
			} catch (Throwable t) {
				log.error("Failed to process export record id={}: {}", record.getId(), t.getMessage(), t);
				if (job != null) {
					job.setErrorMessage(t.getMessage() != null ? t.getMessage() : "EXPORT_FAILED");
					job.setStatus(ExportJobStatus.FAILED);
					exportJobRepository.save(job);
				}
			} finally {
				redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP, record.getId());
			}
		}
	}

	private void handle(ExportJob job, int attempt) {
		try {
			job.setStatus(ExportJobStatus.PROCESSING);
			exportJobRepository.save(job);

			OffsetDateTime startAt = job.getStartDate().atStartOfDay(ZONE_KST).toOffsetDateTime();
			OffsetDateTime endAt = job.getEndDate().plusDays(1).atStartOfDay(ZONE_KST).toOffsetDateTime();
			List<NoShowOrder> orders = noShowOrderRepository
				.findByStoreIdAndCreatedAtBetween(job.getStoreId(), startAt, endAt)
				.stream()
				.sorted(Comparator.comparing(NoShowOrder::getCreatedAt).reversed())
				.toList();

			List<NoShowOrderItem> items = job.isIncludeDetail()
				? loadItems(orders)
				: List.of();

			byte[] bytes = buildWorkbook(orders, items, job.isIncludeDetail());
			String fileKey = buildFileKey(job);

			uploadService.uploadBytes(fileKey, bytes, CONTENT_TYPE_XLSX);

			job.setFileKey(fileKey);
			job.setRowCount(orders.size());
			job.setFileExpiresAt(OffsetDateTime.now().plusDays(fileTtlDays));
			job.setErrorMessage(null);
			job.setStatus(ExportJobStatus.DONE);
			exportJobRepository.save(job);
		} catch (Exception e) {
			int nextAttempt = attempt + 1;
			String message = e.getMessage() != null ? e.getMessage() : "EXPORT_FAILED";
			job.setErrorMessage(message);
			if (nextAttempt > 3) {
				job.setStatus(ExportJobStatus.FAILED);
				exportJobRepository.save(job);
				return;
			}
			job.setStatus(ExportJobStatus.QUEUED);
			exportJobRepository.save(job);
			requeue(job.getId(), nextAttempt);
		}
	}

	private List<NoShowOrderItem> loadItems(List<NoShowOrder> orders) {
		if (orders.isEmpty()) {
			return List.of();
		}
		List<UUID> orderIds = orders.stream().map(NoShowOrder::getId).toList();
		return noShowOrderItemRepository.findWithOrderByOrderIdIn(orderIds);
	}

	private byte[] buildWorkbook(List<NoShowOrder> orders, List<NoShowOrderItem> items, boolean includeDetail) {
		try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
			ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			Sheet summary = workbook.createSheet("주문요약");
			writeSummaryHeader(summary);
			writeSummaryRows(summary, orders);

			if (includeDetail) {
				Sheet detail = workbook.createSheet("주문상세");
				writeDetailHeader(detail);
				writeDetailRows(detail, orders, items);
			}

			workbook.write(output);
			workbook.dispose();
			return output.toByteArray();
		} catch (Exception e) {
			throw new IllegalStateException("엑셀 생성 실패", e);
		}
	}

	private void writeSummaryHeader(Sheet sheet) {
		Row row = sheet.createRow(0);
		row.createCell(0).setCellValue("주문번호");
		row.createCell(1).setCellValue("결제일시");
		row.createCell(2).setCellValue("원래 예약시간");
		row.createCell(3).setCellValue("주문유형");
		row.createCell(4).setCellValue("메뉴명(요약)");
		row.createCell(5).setCellValue("최종 결제 금액");
		row.createCell(6).setCellValue("결제 수단");
		row.createCell(7).setCellValue("주문 상태");
		row.createCell(8).setCellValue("공급가액");
		row.createCell(9).setCellValue("부가세");
	}

	private void writeSummaryRows(Sheet sheet, List<NoShowOrder> orders) {
		int rowIdx = 1;
		for (NoShowOrder order : orders) {
			Row row = sheet.createRow(rowIdx++);
			row.createCell(0).setCellValue(order.getOrderNo());
			row.createCell(1).setCellValue(formatDateTime(order.getPaidAt()));
			row.createCell(2).setCellValue(formatDateTime(order.getVisitTime()));
			row.createCell(3).setCellValue("NO_SHOW");
			row.createCell(4).setCellValue(order.getMenuNames() != null ? order.getMenuNames() : "");
			row.createCell(5).setCellValue(order.getPaidAmount());
			row.createCell(6).setCellValue(order.getPaymentMethod());
			row.createCell(7).setCellValue(order.getStatus().name());

			int supply = supplyAmount(order.getPaidAmount());
			int vat = order.getPaidAmount() - supply;
			row.createCell(8).setCellValue(supply);
			row.createCell(9).setCellValue(vat);
		}
	}

	private void writeDetailHeader(Sheet sheet) {
		Row row = sheet.createRow(0);
		row.createCell(0).setCellValue("주문번호");
		row.createCell(1).setCellValue("결제일시");
		row.createCell(2).setCellValue("원래 예약시간");
		row.createCell(3).setCellValue("주문유형");
		row.createCell(4).setCellValue("메뉴명");
		row.createCell(5).setCellValue("수량");
		row.createCell(6).setCellValue("할인율(%)");
		row.createCell(7).setCellValue("원가(단가)");
		row.createCell(8).setCellValue("결제 수단");
		row.createCell(9).setCellValue("주문 상태");
	}

	private void writeDetailRows(Sheet sheet, List<NoShowOrder> orders, List<NoShowOrderItem> items) {
		Map<UUID, NoShowOrder> orderMap = orders.stream()
			.collect(Collectors.toMap(NoShowOrder::getId, o -> o));
		int rowIdx = 1;
		for (NoShowOrderItem item : items) {
			NoShowOrder order = orderMap.get(item.getOrder().getId());
			if (order == null) {
				continue;
			}
			Row row = sheet.createRow(rowIdx++);
			row.createCell(0).setCellValue(order.getOrderNo());
			row.createCell(1).setCellValue(formatDateTime(order.getPaidAt()));
			row.createCell(2).setCellValue(formatDateTime(order.getVisitTime()));
			row.createCell(3).setCellValue("NO_SHOW");
			row.createCell(4).setCellValue(item.getMenuName());
			row.createCell(5).setCellValue(item.getQuantity());
			row.createCell(6).setCellValue(item.getDiscountPercent());
			row.createCell(7).setCellValue(item.getUnitPrice());
			row.createCell(8).setCellValue(order.getPaymentMethod());
			row.createCell(9).setCellValue(order.getStatus().name());
		}
	}

	private String buildFileKey(ExportJob job) {
		String start = job.getStartDate().format(DATE_FORMAT);
		String end = job.getEndDate().format(DATE_FORMAT);
		return "exports/owner/" + job.getStoreId() + "/sales_" + start + "_" + end + "_" + job.getId() + ".xlsx";
	}

	private String formatDateTime(OffsetDateTime dateTime) {
		if (dateTime == null) {
			return "";
		}
		return DATETIME_FORMAT.format(dateTime);
	}

	private int supplyAmount(int paidAmount) {
		return (int)Math.round(paidAmount / 11.0 * 10);
	}

	private void requeue(UUID jobId, int attempt) {
		Map<String, String> map = Map.of(
			"jobId", jobId.toString(),
			"attempt", String.valueOf(attempt)
		);
		redisTemplate.opsForStream().add(STREAM_KEY, map);
	}

	private record ExportPayload(UUID jobId, int attempt) {
		static ExportPayload from(Map<Object, Object> map) {
			if (map.get("jobId") == null) {
				return null;
			}
			UUID jobId = UUID.fromString(String.valueOf(map.get("jobId")));
			int attempt = 0;
			if (map.containsKey("attempt")) {
				try {
					attempt = Integer.parseInt(String.valueOf(map.get("attempt")));
				} catch (NumberFormatException ignored) {
					attempt = 0;
				}
			}
			return new ExportPayload(jobId, attempt);
		}
	}
}
