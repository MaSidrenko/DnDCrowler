package com.dndcrowler.dnd_backend.domain.model.CampaignMember;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.dndcrowler.dnd_backend.domain.exception.campaignMember.InvalidCampaignMemberCampaignIdException;
import com.dndcrowler.dnd_backend.domain.exception.campaignMember.InvalidCampaignMemberCharacterIdException;
import com.dndcrowler.dnd_backend.domain.exception.campaignMember.InvalidCampaignMemberJoinedAtException;
import com.dndcrowler.dnd_backend.domain.exception.campaignMember.InvalidCampaignMemberUserIdException;

class CampaignMemberTest {
	@Test
	void createsCampaignMember() {
		CampaignMember member = CampaignMember.create(10L, 20L, 30L);

		assertAll(
				() -> assertNull(member.id()),
				() -> assertEquals(10L, member.campaignId()),
				() -> assertEquals(20L, member.userId()),
				() -> assertEquals(30L, member.characterId()),
				() -> assertNotNull(member.joinedAt()));
	}

	@Test
	void validatesRequiredFields() {
		Instant now = Instant.parse("2026-01-01T00:00:00Z");

		assertAll(
				() -> assertThrows(InvalidCampaignMemberCampaignIdException.class,
						() -> new CampaignMember(1L, 0L, 2L, 3L, now)),
				() -> assertThrows(InvalidCampaignMemberUserIdException.class,
						() -> new CampaignMember(1L, 1L, null, 3L, now)),
				() -> assertThrows(InvalidCampaignMemberCharacterIdException.class,
						() -> new CampaignMember(1L, 1L, 2L, -1L, now)),
				() -> assertThrows(InvalidCampaignMemberJoinedAtException.class,
						() -> new CampaignMember(1L, 1L, 2L, 3L, null)));
	}
}
