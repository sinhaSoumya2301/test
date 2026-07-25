package com.company.hr.repository;

import com.company.hr.model.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ShiftRepository extends JpaRepository<Shift, UUID> {

    List<Shift> findByEmployeeIdAndShiftDateBetweenOrderByShiftDateAscStartTimeAsc(
            UUID employeeId, LocalDate from, LocalDate to);
}
