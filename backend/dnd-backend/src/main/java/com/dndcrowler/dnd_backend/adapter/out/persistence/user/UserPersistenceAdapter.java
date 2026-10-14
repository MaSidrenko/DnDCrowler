package com.dndcrowler.dnd_backend.adapter.out.persistence.user;

import org.springframework.stereotype.Repository;

import com.dndcrowler.dnd_backend.application.port.out.User.UserRepositoryPort;
import com.dndcrowler.dnd_backend.domain.model.User.User;

@Repository 
public class UserPersistenceAdapter implements UserRepositoryPort {
	private final SpringDataUserRepository repository;

	public UserPersistenceAdapter(SpringDataUserRepository repository) {
		this.repository = repository;
	}


	@Override
	public boolean existsByEmail(String email) {
		return repository.existsByEmail(email);
	}

	@Override
	public User save(User user) {
		return repository.save(new UserJpaEntity(user)).toDomain();
	}
	
}
