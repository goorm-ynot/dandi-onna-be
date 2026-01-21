package com.mvp.v1.dandionna.owner.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mvp.v1.dandionna.owner.entity.OwnerProfile;

public interface OwnerProfileRepository extends JpaRepository<OwnerProfile, UUID> {

	Optional<OwnerProfile> findByUserId(UUID userId);
}
