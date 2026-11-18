package com.dndcrowler.dnd_backend.adapter.out.persistence.Character;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.dndcrowler.dnd_backend.domain.model.Character.CharacterVisibility;

public interface SpringDataCharacterRepository
	extends JpaRepository<CharacterJpaEntity, Long>,
	JpaSpecificationExecutor<CharacterJpaEntity> {
	List<CharacterJpaEntity> findByOwnerId(Long ownerId);

	List<CharacterJpaEntity> findByCharacterVisibility(
		CharacterVisibility characterVisibility
	);

	
}
