package com.mvp.v1.dandionna.export_job.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mvp.v1.dandionna.export_job.entity.ExportJob;

public interface ExportJobRepository extends JpaRepository<ExportJob, UUID> {

	Optional<ExportJob> findByRequestHashAndActiveTrue(String requestHash);

	boolean existsByRequestHashAndActiveTrue(String requestHash);
}
