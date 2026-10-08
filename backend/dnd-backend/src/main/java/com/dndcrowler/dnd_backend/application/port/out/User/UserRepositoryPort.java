package com.dndcrowler.dnd_backend.application.port.out.User;

import com.dndcrowler.dnd_backend.domain.model.User.User;

public interface UserRepositoryPort {
	boolean existsByEmail(String email);

	User save(User user);
	
}
