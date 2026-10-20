package com.dndcrowler.dnd_backend.adapter.out.persistence.user;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.dndcrowler.dnd_backend.application.port.out.User.UserRepositoryPort;
import com.dndcrowler.dnd_backend.application.service.exception.EmailAlreadyRegisteredException;
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
		try {
			return repository.saveAndFlush(new UserJpaEntity(user)).toDomain();
		} catch (DataIntegrityViolationException exception) {
			if (isDuplicateEmail(exception)) {
				throw new EmailAlreadyRegisteredException();
			}
			throw exception;
		}
	}

	private boolean isDuplicateEmail(DataIntegrityViolationException exception) {
		for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
			if (cause instanceof ConstraintViolationException violation
					&& "UK_users_email".equalsIgnoreCase(violation.getConstraintName())) {
				return true;
			}
		}
		return false;
	}
}
