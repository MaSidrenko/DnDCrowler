package com.dndcrowler.dnd_backend.adapter.out.persistence.Email;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

public interface SpringDataEmailVerificationRepository extends JpaRepository<EmailVerificationJpaEntity, Long> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<EmailVerificationJpaEntity> findFirstByEmailOrderByIdDesc(String email);
}
