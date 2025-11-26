package com.mvp.v1.dandionna.notification.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mvp.v1.dandionna.notification.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
}
