package com.dndcrowler.dnd_backend.application.port.in.command;

import java.util.List;

import com.dndcrowler.dnd_backend.domain.exception.skills.InvalidSkillEffectsException;
import com.dndcrowler.dnd_backend.domain.model.Skills.ActivationType;
import com.dndcrowler.dnd_backend.domain.model.Skills.Effect;
import com.dndcrowler.dnd_backend.domain.model.Skills.UsagePolicy;

public record CreateSkillCommand(
	String name,
	String description,
	ActivationType activationType,
	List<Effect> effects,
	UsagePolicy usagePolicy
) {
	public CreateSkillCommand {
		if(effects == null || effects.stream().anyMatch(s -> s == null))
			throw new InvalidSkillEffectsException();

		effects = List.copyOf(effects);
	}
}
