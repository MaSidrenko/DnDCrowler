package com.dndcrowler.dnd_backend.application.port.out.User;

import java.util.Optional;

import com.dndcrowler.dnd_backend.domain.model.User.User;

public interface UserRepositoryPort {
	boolean existsByEmail(String email);

	User save(User user);
	Optional<User> findById(Long id);

	Optional<User> findByEmail(String email);
	Optional<User> findByEmailForUpdate(String email);
}
