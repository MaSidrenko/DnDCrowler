package com.dndcrowler.dnd_backend.application.port.in;

import java.util.List;

import com.dndcrowler.dnd_backend.application.port.in.command.Character.CreateCharacterCommand;
import com.dndcrowler.dnd_backend.application.port.in.command.Character.UpdateCharacterCommand;
import com.dndcrowler.dnd_backend.application.port.in.query.CharacterSearchCriteria;
import com.dndcrowler.dnd_backend.domain.model.Character.Character;
import com.dndcrowler.dnd_backend.domain.model.Character.CharacterVisibility;

public interface CharacterUseCase {
	List<Character> getAllCharacter();

	List<Character> searchPublicCharacters(CharacterSearchCriteria characterSearchCriteria);

	List<Character> getCharacterByVisibility(CharacterVisibility visibility);

	Character createCharacter(CreateCharacterCommand command);

	Character getCharacterById(Long id, Long currentUserId);

	List<Character> getMyCharacters(Long ownerId);

	List<Character> getPublicCharacters();

	Character updateCharacter(Long id, UpdateCharacterCommand command);
}
