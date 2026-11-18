package com.dndcrowler.dnd_backend.adapter.in.web.character.DTO.Response;

import com.dndcrowler.dnd_backend.adapter.in.web.character.DTO.EffectType;
import com.dndcrowler.dnd_backend.domain.model.Character.Stat;
import com.dndcrowler.dnd_backend.domain.model.Skills.DamageBonus;
import com.dndcrowler.dnd_backend.domain.model.Skills.Effect;
import com.dndcrowler.dnd_backend.domain.model.Skills.StatBonus;

public record EffectResponse(
		EffectType type,
		Stat stat,
		int amount) {
	public static EffectResponse from(Effect effect) {
		return switch (effect) {
			case StatBonus bonus -> new EffectResponse(
					EffectType.STAT_BONUS,
					bonus.stat(),
					bonus.amount());

			case DamageBonus bonus -> new EffectResponse(
					EffectType.DAMAGE_BONUS,
					null,
					bonus.amount());
		};
	}
}
