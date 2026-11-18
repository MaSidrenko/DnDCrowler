package com.dndcrowler.dnd_backend.adapter.in.web.character.DTO.Response;

import java.util.List;

import com.dndcrowler.dnd_backend.domain.model.Skills.ActivationType;
import com.dndcrowler.dnd_backend.domain.model.Skills.Skills;
import com.dndcrowler.dnd_backend.domain.model.Skills.UsagePolicy;

public record SkillResponse(
	Long id,
	String name,
	String description,
	ActivationType activationType,
	List<EffectResponse> effects,
	UsagePolicy usagePolicy
) {
	public static SkillResponse from(Skills skill) {
		return  new SkillResponse(
			skill.id(), 
			skill.name(), 
			skill.description(), 
			skill.activationType(), 
			skill.effects().stream()
				.map(EffectResponse::from)
				.toList(), 
			skill.usagePolicy()	
		);
	}
}
