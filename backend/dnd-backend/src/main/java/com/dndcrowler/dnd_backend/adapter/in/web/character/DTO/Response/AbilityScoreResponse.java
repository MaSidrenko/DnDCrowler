package com.dndcrowler.dnd_backend.adapter.in.web.character.DTO.Response;

import com.dndcrowler.dnd_backend.domain.model.Character.AbilityScores;

public record AbilityScoreResponse(
		int strength,
		int dexterity,
		int constitution,
		int intelligence,
		int wisdom,
		int charisma) {
	public static AbilityScoreResponse from(AbilityScores abilityScores) {
		return new AbilityScoreResponse(
				abilityScores.strength(),
				abilityScores.dexterity(),
				abilityScores.constitution(),
				abilityScores.intelligence(),
				abilityScores.wisdom(),
				abilityScores.charisma());
	}
}
