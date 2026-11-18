package com.dndcrowler.dnd_backend.adapter.in.web.character.DTO.Request;

import java.util.List;

import com.dndcrowler.dnd_backend.application.port.in.command.Character.CreateSkillCommand;
import com.dndcrowler.dnd_backend.domain.model.Skills.ActivationType;
import com.dndcrowler.dnd_backend.domain.model.Skills.UsagePolicy;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSkillRequest(
	@NotBlank 
	String name,
	@NotBlank 
	String description,
	@NotNull 
	ActivationType activationType,
	@NotNull 
	List<@NotNull @Valid  EffectRequest> effects,
	@NotNull 
	UsagePolicy usagePolicy
) {
	public CreateSkillCommand toCommand() {
		return new CreateSkillCommand(
			name, 
			description, 
			activationType, 
			effects.stream()
				.map(EffectRequest::toDomain)
				.toList(), 
			usagePolicy	
		);
	}	
}
