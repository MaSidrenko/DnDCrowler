package com.dndcrowler.dnd_backend.adapter.in.web.character;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.dndcrowler.dnd_backend.adapter.in.web.character.DTO.Request.CharacterCreateRequest;
import com.dndcrowler.dnd_backend.adapter.in.web.character.DTO.Request.UpdateCharacterRequest;
import com.dndcrowler.dnd_backend.adapter.in.web.character.DTO.Response.CharacterResponse;
import com.dndcrowler.dnd_backend.adapter.in.web.security.AuthenticatedUserPrincipal;
import com.dndcrowler.dnd_backend.application.port.in.CharacterUseCase;
import com.dndcrowler.dnd_backend.application.port.in.command.Character.CreateCharacterCommand;
import com.dndcrowler.dnd_backend.application.port.in.command.Character.UpdateCharacterCommand;
import com.dndcrowler.dnd_backend.application.port.in.query.CharacterSearchCriteria;
import com.dndcrowler.dnd_backend.domain.model.Character.Character;
import com.dndcrowler.dnd_backend.domain.model.Character.CharacterClass;
import com.dndcrowler.dnd_backend.domain.model.Character.Race;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/characters")
public class CharacterController {
	private final CharacterUseCase useCase;

	public CharacterController(CharacterUseCase useCase) {
		this.useCase = useCase;
	}

	@PostMapping()
	@ResponseStatus(HttpStatus.CREATED)
	public CharacterResponse create(
			@Valid @RequestBody CharacterCreateRequest request,
			@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
		CreateCharacterCommand command = request.toCommand(principal.userId());
		Character character = useCase.createCharacter(command);

		return CharacterResponse.from(character);
	}

	@PutMapping("/{characterId}")
	public CharacterResponse update(
			@PathVariable Long characterId,
			@Valid @RequestBody UpdateCharacterRequest request,
			@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
		UpdateCharacterCommand command = request.toCommand(principal.userId());

		return CharacterResponse.from(
				useCase.updateCharacter(characterId, command));
	}

	@GetMapping("/me")
	public List<CharacterResponse> getMyCharacters(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
		return useCase.getMyCharacters(principal.userId())
				.stream()
				.map(CharacterResponse::from)
				.toList();
	}

	@GetMapping("/public")
	public List<CharacterResponse> getPublicCharactersByRequestParams(@RequestParam(required = false) String name,
			@RequestParam(required = false) Race race,
			@RequestParam(required = false) CharacterClass characterClass) {
		CharacterSearchCriteria criteria = new CharacterSearchCriteria(name, race, characterClass);

		return useCase.searchPublicCharacters(criteria)
				.stream()
				.map(CharacterResponse::from)
				.toList();
	}

	@GetMapping("/{characterId}")
	public CharacterResponse getCharacterById(@PathVariable Long characterId,
			@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
		return CharacterResponse.from(
				useCase.getCharacterById(characterId,
						principal.userId()));
	}

}
