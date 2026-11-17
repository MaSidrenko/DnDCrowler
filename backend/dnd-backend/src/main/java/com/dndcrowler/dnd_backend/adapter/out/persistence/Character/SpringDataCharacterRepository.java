package com.dndcrowler.dnd_backend.adapter.out.persistence.Character;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dndcrowler.dnd_backend.domain.model.Character.CharacterClass;
import com.dndcrowler.dnd_backend.domain.model.Character.CharacterVisibility;
import com.dndcrowler.dnd_backend.domain.model.Character.Race;

public interface SpringDataCharacterRepository extends JpaRepository<CharacterJpaEntity, Long> {
	List<CharacterJpaEntity> findByOwnerId(Long ownerId);
	List<CharacterJpaEntity> findByCharacterClassAndCharacterVisibility(
		CharacterClass  characterClass,
		CharacterVisibility characterVisibility
	);

	List<CharacterJpaEntity> findByNameContainingIgnoreCaseAndCharacterVisibility(
		String name,
		CharacterVisibility characterVisibility
	);

	List<CharacterJpaEntity> findByRaceAndCharacterVisibility(
		Race race,
		CharacterVisibility characterVisibility
	);

	List<CharacterJpaEntity> findByCharacterVisibility(
		CharacterVisibility characterVisibility
	);

	
}
