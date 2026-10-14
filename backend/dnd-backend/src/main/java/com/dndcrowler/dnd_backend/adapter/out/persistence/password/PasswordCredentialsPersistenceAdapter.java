package com.dndcrowler.dnd_backend.adapter.out.persistence.password;

import org.springframework.stereotype.Repository;

import com.dndcrowler.dnd_backend.application.port.out.Password.PasswordCredentialsRepositoryPort;
import com.dndcrowler.dnd_backend.domain.model.Password.PasswordCredentials;

@Repository
public class PasswordCredentialsPersistenceAdapter implements PasswordCredentialsRepositoryPort {
    private final SpringDataPasswordCredentialsRepository repository;

    public PasswordCredentialsPersistenceAdapter(
            SpringDataPasswordCredentialsRepository repository) {
        this.repository = repository;
    }

    @Override
    public PasswordCredentials save(PasswordCredentials credentials) {
        return repository.save(new PasswordCredentialsJpaEntity(credentials))
                .toDomain();
    }

}
