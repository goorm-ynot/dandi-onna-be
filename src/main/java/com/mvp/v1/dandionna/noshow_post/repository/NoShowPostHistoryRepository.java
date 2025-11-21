package com.mvp.v1.dandionna.noshow_post.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mvp.v1.dandionna.noshow_post.entity.NoShowPostHistory;

public interface NoShowPostHistoryRepository extends JpaRepository<NoShowPostHistory, Long> {
}
