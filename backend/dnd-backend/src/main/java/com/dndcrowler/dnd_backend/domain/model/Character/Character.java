package com.dndcrowler.dnd_backend.domain.model.Character;

import java.util.List;
import java.util.Objects;

import com.dndcrowler.dnd_backend.domain.exception.character.InvalidCharacterAbilityScoresException;
import com.dndcrowler.dnd_backend.domain.exception.character.InvalidCharacterClassException;
import com.dndcrowler.dnd_backend.domain.exception.character.InvalidCharacterDescriptionException;
import com.dndcrowler.dnd_backend.domain.exception.character.InvalidCharacterLevelException;
import com.dndcrowler.dnd_backend.domain.exception.character.InvalidCharacterNameException;
import com.dndcrowler.dnd_backend.domain.exception.character.InvalidCharacterOwnerIDException;
import com.dndcrowler.dnd_backend.domain.exception.character.InvalidCharacterRaceArgumentException;
import com.dndcrowler.dnd_backend.domain.exception.character.InvalidCharacterSkillsException;
import com.dndcrowler.dnd_backend.domain.exception.character.InvalidCharacterVisibilityException;
import com.dndcrowler.dnd_backend.domain.model.Skills.Skills;

public record Character(Long id, String name, String description, Long ownerID, int level, 
		Race race, AbilityScores abilityScores, List<Skills> skills, CharacterClass characterClass, CharacterVisibility characterVisibility) {
	public Character {
		if(name == null || name.isBlank()) throw new InvalidCharacterNameException();

		if(description == null || description.isBlank()) throw new InvalidCharacterDescriptionException();

		if(race == null) throw new InvalidCharacterRaceArgumentException();

		if(ownerID == null) throw new InvalidCharacterOwnerIDException();

		if(abilityScores == null) throw new InvalidCharacterAbilityScoresException();

		if(level < 1) throw new InvalidCharacterLevelException();

		if(skills == null || skills.stream().anyMatch(Objects::isNull))
			throw new InvalidCharacterSkillsException();

		if(characterClass == null) throw new InvalidCharacterClassException();

		if(characterVisibility == null) throw new InvalidCharacterVisibilityException();
		
		skills = List.copyOf(skills);
	}

	public static Character create(String name,String description, Long ownerID, Race race,
			 AbilityScores abilityScores, List<Skills> skills, CharacterClass characterClass,
				CharacterVisibility characterVisibility) {
		return new Character(null, name, description, ownerID, 1, race, 
				abilityScores, skills, characterClass, characterVisibility);
	}
}
