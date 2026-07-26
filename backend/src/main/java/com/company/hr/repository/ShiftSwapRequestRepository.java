package com.company.hr.repository;

import com.company.hr.model.entity.ShiftSwapRequest;
import com.company.hr.model.entity.SwapStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ShiftSwapRequestRepository extends JpaRepository<ShiftSwapRequest, UUID> {

    @Query("""
            select r from ShiftSwapRequest r
            where r.requester.id = :employeeId or r.targetEmployee.id = :employeeId
            order by r.createdAt desc
            """)
    Page<ShiftSwapRequest> findAllForEmployee(@Param("employeeId") UUID employeeId, Pageable pageable);

    // The MANAGER stage is authorized by the requester's *current* manager, resolved via the
    // Employee relationship rather than a snapshotted field — see ShiftSwapRequest's class doc.
    @Query("""
            select r from ShiftSwapRequest r
            where r.requester.manager.id = :managerId
            order by r.createdAt desc
            """)
    Page<ShiftSwapRequest> findAllForManager(@Param("managerId") UUID managerId, Pageable pageable);

    List<ShiftSwapRequest> findByStatusInAndExpiresAtBefore(List<SwapStatus> statuses, Instant cutoff);
}
