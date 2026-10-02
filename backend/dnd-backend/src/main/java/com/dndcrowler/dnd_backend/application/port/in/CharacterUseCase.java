package com.dndcrowler.dnd_backend.application.port.in;

import java.util.List;

import com.dndcrowler.dnd_backend.application.port.in.command.CreateCharacterCommand;
import com.dndcrowler.dnd_backend.application.port.in.command.UpdateCharacterCommand;
import com.dndcrowler.dnd_backend.domain.model.Character.Character;
import com.dndcrowler.dnd_backend.domain.model.Character.CharacterClass;
import com.dndcrowler.dnd_backend.domain.model.Character.CharacterVisibility;
import com.dndcrowler.dnd_backend.domain.model.Character.Race;

public interface CharacterUseCase {
	List<Character> getAllCharacter();

	List<Character> getCharacterByName(String name);

	List<Character> getCharacterByRace(Race race);

	List<Character> getCharacterByClass(CharacterClass characterClass);

	List<Character> getCharacterByVisibility(CharacterVisibility visibility);

	Character createCharacter(CreateCharacterCommand command);

	Character getCharacterById(Long id);

	List<Character> getMyCharacter(Long ownerId);

	List<Character> getPublicCharacters();

	Character updateCharacter(Long id, UpdateCharacterCommand command);
}
