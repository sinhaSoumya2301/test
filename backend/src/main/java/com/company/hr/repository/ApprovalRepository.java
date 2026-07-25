package com.company.hr.repository;

import com.company.hr.model.entity.Approval;
import com.company.hr.model.entity.ApprovalStage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApprovalRepository extends JpaRepository<Approval, UUID> {

    List<Approval> findByShiftSwapRequestIdOrderByCreatedAtAsc(UUID shiftSwapRequestId);

    Optional<Approval> findByShiftSwapRequestIdAndStage(UUID shiftSwapRequestId, ApprovalStage stage);
}
