package com.mvp.v1.dandionna.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mvp.v1.dandionna.auth.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {
	boolean existsByLoginId(String loginId);
	Optional<User> findByLoginId(String loginId);
}
