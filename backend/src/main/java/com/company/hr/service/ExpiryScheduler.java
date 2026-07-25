package com.company.hr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Auto-expires stale PENDING_* swap requests daily (FR-10). See ShiftSwapService#expireStale. */
@Component
public class ExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExpiryScheduler.class);

    private final ShiftSwapService shiftSwapService;

    public ExpiryScheduler(ShiftSwapService shiftSwapService) {
        this.shiftSwapService = shiftSwapService;
    }

    @Scheduled(cron = "0 0 3 * * *") // 03:00 server time daily — low-traffic window
    public void expireStaleRequests() {
        log.info("Running scheduled shift-swap-request expiry scan");
        shiftSwapService.expireStale();
    }
}
