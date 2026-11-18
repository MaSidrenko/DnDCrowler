package com.dndcrowler.dnd_backend.adapter.in.web.character.DTO.Request;

import com.dndcrowler.dnd_backend.adapter.in.web.character.DTO.EffectType;
import com.dndcrowler.dnd_backend.domain.model.Character.Stat;
import com.dndcrowler.dnd_backend.domain.model.Skills.DamageBonus;
import com.dndcrowler.dnd_backend.domain.model.Skills.Effect;
import com.dndcrowler.dnd_backend.domain.model.Skills.StatBonus;

import jakarta.validation.constraints.NotNull;

public record EffectRequest(
	@NotNull 
	EffectType type,
	Stat stat,
	int amount
) {
	public Effect toDomain() {
		return  switch(type) {
			case STAT_BONUS -> new StatBonus(stat, amount);
			case DAMAGE_BONUS -> new DamageBonus(amount);
		};
	}
}


