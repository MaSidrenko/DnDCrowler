package com.dndcrowler.dnd_backend.adapter.out.persistence.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface  SpringDataUserRepository extends JpaRepository<UserJpaEntity, Long> {
	boolean existsByEmail(String email);
	Optional<UserJpaEntity> findByEmail(String email);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT u FROM UserJpaEntity u WHERE u.email = :email")
	Optional<UserJpaEntity> findByEmailForUpdate(@Param("email") String email);
}
