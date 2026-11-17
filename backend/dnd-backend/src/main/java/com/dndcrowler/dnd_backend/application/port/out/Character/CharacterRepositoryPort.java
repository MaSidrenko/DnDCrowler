package com.dndcrowler.dnd_backend.application.port.out.Character;

import java.util.List;
import java.util.Optional;

import com.dndcrowler.dnd_backend.domain.model.Character.Character;
import com.dndcrowler.dnd_backend.domain.model.Character.CharacterClass;
import com.dndcrowler.dnd_backend.domain.model.Character.CharacterVisibility;
import com.dndcrowler.dnd_backend.domain.model.Character.Race;

public interface CharacterRepositoryPort {
	Character save(Character newCharacter);

	Optional<Character> findById(Long id);

	List<Character> findByOwnerId(Long ownerId);

	List<Character> findAll();

	List<Character> findCharacterByClassAndVisibility(CharacterClass characterClass, CharacterVisibility visibility);

	List<Character> findCharacterByNameAndVisibility(String name, CharacterVisibility visibility);

	List<Character> findCharacterByRaceAndVisibility(Race race, CharacterVisibility visibility);

	List<Character> findCharacterByVisibility(CharacterVisibility visibility);
}
