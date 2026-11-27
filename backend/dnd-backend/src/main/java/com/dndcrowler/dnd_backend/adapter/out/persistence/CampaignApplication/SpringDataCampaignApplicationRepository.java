package com.dndcrowler.dnd_backend.adapter.out.persistence.CampaignApplication;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dndcrowler.dnd_backend.domain.model.CampaignApplication.CampaignApplicationStatus;

import jakarta.persistence.LockModeType;

public interface SpringDataCampaignApplicationRepository
		extends JpaRepository<CampaignApplicationJpaEntity, Long> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select application
			from CampaignApplicationJpaEntity application
			where application.id = :applicationId
			""")
	Optional<CampaignApplicationJpaEntity> findByIdForUpdate(
			@Param("applicationId") Long applicationId);

	boolean existsByCampaignIdAndApplicantIdAndStatus(
			Long campaignId,
			Long applicantId,
			CampaignApplicationStatus status);

	List<CampaignApplicationJpaEntity> findByApplicantIdOrderByCreatedAtDesc(Long applicantId);

	List<CampaignApplicationJpaEntity> findByCampaignIdOrderByCreatedAtDesc(Long campaignId);
}
