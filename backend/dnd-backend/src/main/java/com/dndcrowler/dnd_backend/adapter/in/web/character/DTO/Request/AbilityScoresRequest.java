package com.dndcrowler.dnd_backend.adapter.in.web.character.DTO.Request;

import com.dndcrowler.dnd_backend.domain.model.Character.AbilityScores;

public record AbilityScoresRequest(
	int strength,
	int dexterity,
	int constitution,
	int intelligence,
	int wisdom,
	int charisma
) {
	public AbilityScores toDomain() {
		return  AbilityScores.create(
			strength, 
			dexterity, 
			constitution, 
			intelligence, 
			wisdom, 
			charisma
		);
	}
}
