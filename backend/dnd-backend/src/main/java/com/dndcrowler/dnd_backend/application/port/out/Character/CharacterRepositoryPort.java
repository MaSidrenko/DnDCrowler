package com.dndcrowler.dnd_backend.application.port.out.Character;

import java.util.List;
import java.util.Optional;

import com.dndcrowler.dnd_backend.application.port.in.query.CharacterSearchCriteria;
import com.dndcrowler.dnd_backend.domain.model.Character.Character;
import com.dndcrowler.dnd_backend.domain.model.Character.CharacterVisibility;

public interface CharacterRepositoryPort {
	Character save(Character newCharacter);

	Optional<Character> findById(Long id);

	List<Character> findByOwnerId(Long ownerId);

	List<Character> findAll();

	List<Character> findPublic(CharacterSearchCriteria characterSearchCriteria);

	List<Character> findCharacterByVisibility(CharacterVisibility visibility);
}
