package com.dndcrowler.dnd_backend.adapter.out.persistence.password;

import java.time.Instant;

import com.dndcrowler.dnd_backend.domain.model.Password.PasswordCredentials;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
@Entity 
@Table(name = "password_credentials")
public class PasswordCredentialsJpaEntity {
	@Id 
	private Long userId;
	private String passwordHash;
	private Instant passwordChangedAt;

	protected PasswordCredentialsJpaEntity() {}

	public PasswordCredentialsJpaEntity(PasswordCredentials credentials) {
		this.userId = credentials.userId();
		this.passwordHash = credentials.passwordHash();
		this.passwordChangedAt = credentials.passwordChangedAt();
	}

	public PasswordCredentials toDomain() {
		return new PasswordCredentials(userId, passwordHash, passwordChangedAt);
	}
}
