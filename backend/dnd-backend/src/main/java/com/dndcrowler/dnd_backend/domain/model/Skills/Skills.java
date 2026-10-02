package com.dndcrowler.dnd_backend.domain.model.Skills;

import java.util.List;
import java.util.Objects;

import com.dndcrowler.dnd_backend.domain.exception.skills.InvalidSkillActivationTypeException;
import com.dndcrowler.dnd_backend.domain.exception.skills.InvalidSkillDescriptionException;
import com.dndcrowler.dnd_backend.domain.exception.skills.InvalidSkillEffectsException;
import com.dndcrowler.dnd_backend.domain.exception.skills.InvalidSkillNameException;
import com.dndcrowler.dnd_backend.domain.exception.skills.InvalidSkillUsagePolicyException;

public record Skills(Long id, String name, String description,
		 ActivationType activationType, List<Effect> effects, UsagePolicy usagePolicy) {
	public Skills {

		if(name == null || name.isBlank()) throw new InvalidSkillNameException();

		if(description == null || description.isBlank()) throw new InvalidSkillDescriptionException();

		if(activationType == null) throw new InvalidSkillActivationTypeException();

		if(effects == null || effects.stream().anyMatch(Objects::isNull)) 
			throw new InvalidSkillEffectsException();

		if(usagePolicy == null) throw new InvalidSkillUsagePolicyException();

		effects = List.copyOf(effects);
	}

	public static Skills create(String name, String description, ActivationType activationType, 
			List<Effect> effects, UsagePolicy usagePolicy) {
				return new Skills(null, name, description, activationType, effects, usagePolicy);
	}

}
