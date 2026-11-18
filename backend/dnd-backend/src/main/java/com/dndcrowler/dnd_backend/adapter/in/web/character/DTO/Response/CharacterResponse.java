package com.dndcrowler.dnd_backend.adapter.in.web.character.DTO.Response;

import java.util.List;

import com.dndcrowler.dnd_backend.domain.model.Character.Character;
import com.dndcrowler.dnd_backend.domain.model.Character.CharacterClass;
import com.dndcrowler.dnd_backend.domain.model.Character.CharacterVisibility;
import com.dndcrowler.dnd_backend.domain.model.Character.Race;

public record CharacterResponse(
		Long id,
		Long ownerId,
		String name,
		String description,
		Race race,
		int level,
		AbilityScoreResponse abilityScores,
		List<SkillResponse> skills,
		CharacterClass characterClass,
		CharacterVisibility visibility) {
	public static CharacterResponse from(Character character) {
		return new CharacterResponse(
				character.id(),
				character.ownerID(),
				character.name(),
				character.description(),
				character.race(),
				character.level(),
				AbilityScoreResponse.from(character.abilityScores()),
				character.skills().stream()
					.map(SkillResponse::from)
					.toList(),
				character.characterClass(),
				character.characterVisibility());
	}
}
