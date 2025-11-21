package com.mvp.v1.dandionna.consumer.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mvp.v1.dandionna.consumer.entity.ConsumerProfile;

public interface ConsumerProfileRepository extends JpaRepository<ConsumerProfile, UUID> {

	List<ConsumerProfile> findByUserIdIn(Collection<UUID> userIds);
}
