package com.dndcrowler.dnd_backend.adapter.out.persistence.user;

import java.time.Instant;

import com.dndcrowler.dnd_backend.domain.model.User.User;
import com.dndcrowler.dnd_backend.domain.model.User.UserRole;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserJpaEntity {
	@Id
	@GeneratedValue(strategy= GenerationType.IDENTITY)
	private Long id;
	private String username;
	private String email;
	@Enumerated(EnumType.STRING)
	private UserRole role;
	private Instant createdAt;
	private Instant updatedAt;
	private Instant emailVerifiedAt;
	protected UserJpaEntity() {
		
	}
	public UserJpaEntity(User user) {
		this.id = user.id();
		this.username = user.username();
		this.email = user.email();
		this.role = user.role();
		this.createdAt = user.createdAt();
		this.updatedAt = user.updatedAt();
		this.emailVerifiedAt = user.emailVerifiedAt();
	}

	public User toDomain() {
		return new User(id, username, email, role,
				createdAt, updatedAt, emailVerifiedAt);
	}
}
