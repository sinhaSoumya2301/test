package com.company.hr.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Shift-date/time helpers used by ShiftSwapServiceImpl's "must be a future shift" validation (FR-2). */
@Component
public class DateUtil {

    public boolean isFuture(LocalDate date, LocalTime time) {
        return LocalDateTime.of(date, time).isAfter(LocalDateTime.now());
    }
}
