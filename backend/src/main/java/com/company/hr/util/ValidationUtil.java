package com.company.hr.util;

import com.company.hr.exception.UnprocessableEntityException;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Small, reused validation guards for ShiftSwapServiceImpl (kept here rather than duplicated inline). */
@Component
public class ValidationUtil {

    public void requireDifferent(UUID a, UUID b, String code, String message) {
        if (a.equals(b)) {
            throw new UnprocessableEntityException(code, message);
        }
    }

    public void requireTrue(boolean condition, String code, String message) {
        if (!condition) {
            throw new UnprocessableEntityException(code, message);
        }
    }
}
