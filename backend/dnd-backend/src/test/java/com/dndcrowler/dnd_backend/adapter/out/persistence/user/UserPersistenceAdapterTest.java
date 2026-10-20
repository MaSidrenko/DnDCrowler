package com.dndcrowler.dnd_backend.adapter.out.persistence.user;

import java.sql.SQLException;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.dndcrowler.dnd_backend.application.service.exception.EmailAlreadyRegisteredException;
import com.dndcrowler.dnd_backend.domain.model.User.User;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPersistenceAdapterTest {
	@Mock
	private SpringDataUserRepository repository;

	@Test
	void translatesOnlyEmailUniqueConstraintToRegistrationConflict() {
		DataIntegrityViolationException duplicateEmail = violation("UK_users_email");
		when(repository.saveAndFlush(any(UserJpaEntity.class))).thenThrow(duplicateEmail);

		assertThrows(EmailAlreadyRegisteredException.class,
				() -> new UserPersistenceAdapter(repository).save(User.create("alice", "alice@example.com")));
	}

	@Test
	void preservesOtherIntegrityViolations() {
		DataIntegrityViolationException otherViolation = violation("some_other_constraint");
		when(repository.saveAndFlush(any(UserJpaEntity.class))).thenThrow(otherViolation);

		DataIntegrityViolationException actual = assertThrows(DataIntegrityViolationException.class,
				() -> new UserPersistenceAdapter(repository).save(User.create("alice", "alice@example.com")));
		assertSame(otherViolation, actual);
	}

	private DataIntegrityViolationException violation(String constraintName) {
		return new DataIntegrityViolationException("Constraint violation",
				new ConstraintViolationException("Constraint violation", new SQLException(), constraintName));
	}
}
