package com.dndcrowler.dnd_backend.application.port.out.Password;

import com.dndcrowler.dnd_backend.domain.model.Password.PasswordCredentials;

public interface PasswordCredentialsRepositoryPort {
	PasswordCredentials save(PasswordCredentials passwordCredentials);
}
