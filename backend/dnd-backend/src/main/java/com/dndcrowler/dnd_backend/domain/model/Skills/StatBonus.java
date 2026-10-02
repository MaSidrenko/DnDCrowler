package com.dndcrowler.dnd_backend.domain.model.Skills;

import com.dndcrowler.dnd_backend.domain.exception.skills.InvalidStatBonusStatException;
import com.dndcrowler.dnd_backend.domain.model.Character.Stat;

public record StatBonus(Stat stat, int amount) implements Effect {
	public StatBonus {
		if (stat == null) throw new InvalidStatBonusStatException();
	}

	public static StatBonus create(Stat stat, int amount) {
		return new StatBonus(stat, amount);
	}
}
