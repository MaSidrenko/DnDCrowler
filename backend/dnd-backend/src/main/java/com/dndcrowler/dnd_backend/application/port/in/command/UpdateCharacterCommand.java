package com.dndcrowler.dnd_backend.application.port.in.command;

import java.util.List;

import com.dndcrowler.dnd_backend.domain.exception.character.InvalidCharacterSkillsException;
import com.dndcrowler.dnd_backend.domain.model.Character.AbilityScores;
import com.dndcrowler.dnd_backend.domain.model.Character.CharacterClass;
import com.dndcrowler.dnd_backend.domain.model.Character.CharacterVisibility;
import com.dndcrowler.dnd_backend.domain.model.Character.Race;

public record UpdateCharacterCommand(
	Long currentUserId,
	String name,
	String description,
	int level,
	Race race,
	AbilityScores abilityScores,
	List<CreateSkillCommand> skills,
	CharacterClass characterClass,
	CharacterVisibility visibility
) {
	public UpdateCharacterCommand {
		if(skills == null || skills.stream().anyMatch(s -> s == null))
			throw new InvalidCharacterSkillsException();

		skills = List.copyOf(skills);
	}
}
