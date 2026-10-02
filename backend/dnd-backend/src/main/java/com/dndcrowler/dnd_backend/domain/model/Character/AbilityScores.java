package com.dndcrowler.dnd_backend.domain.model.Character;

import com.dndcrowler.dnd_backend.domain.exception.character.InvalidArgumentScoreException;

public record AbilityScores(int strength, int dexterity, int constitution,
			 int intelligence, int wisdom, int charisma) {
	public AbilityScores {
		if(strength < 1 || dexterity < 1 || constitution < 1 || intelligence < 1
			 || wisdom < 1 || charisma < 1) {
			throw new InvalidArgumentScoreException();
		}
	}

	public static AbilityScores create(int strength, int dexterity, int constitution,
				int intelligence, int wisdom, int charisma) {
			return new AbilityScores(strength, dexterity, constitution,
					intelligence, wisdom, charisma);
		}
}
