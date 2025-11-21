package com.mvp.v1.dandionna.fcm.repository;

import java.util.List;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mvp.v1.dandionna.fcm.entity.Platform;
import com.mvp.v1.dandionna.fcm.entity.PushToken;

public interface PushTokenRepository extends JpaRepository<PushToken, UUID> {

	@Query("select p from PushToken p where p.user.id = :userId and p.platform = :platform and p.deviceId = :deviceId")
	Optional<PushToken> findByUserPlatformAndDevice(@Param("userId") UUID userId,
		@Param("platform") Platform platform,
		@Param("deviceId") String deviceId);

	@Query("select p from PushToken p where p.fcmToken = :fcmToken")
	Optional<PushToken> findByFcmToken(@Param("fcmToken") String fcmToken);

	@Query("select p from PushToken p where p.user.id = :userId")
	List<PushToken> findAllByUserId(@Param("userId") UUID userId);

	@Modifying
	@Query("delete from PushToken p where p.fcmToken = :fcmToken")
	void deleteByFcmToken(@Param("fcmToken") String fcmToken);

	@Modifying
	@Query("delete from PushToken p where p.user.id = :userId and p.deviceId = :deviceId")
	void deleteByUserIdAndDeviceId(@Param("userId") UUID userId, @Param("deviceId") String deviceId);
}
