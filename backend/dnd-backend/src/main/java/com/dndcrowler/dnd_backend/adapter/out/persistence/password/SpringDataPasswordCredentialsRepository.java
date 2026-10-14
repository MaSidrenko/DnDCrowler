package com.dndcrowler.dnd_backend.adapter.out.persistence.password;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataPasswordCredentialsRepository extends JpaRepository<PasswordCredentialsJpaEntity, Long> {

}
