package com.dndcrowler.dnd_backend.adapter.out.persistence.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface  SpringDataUserRepository extends JpaRepository<UserJpaEntity, Long> {
	boolean existsByEmail(String email);
	Optional<UserJpaEntity> findByEmail(String email);
}
