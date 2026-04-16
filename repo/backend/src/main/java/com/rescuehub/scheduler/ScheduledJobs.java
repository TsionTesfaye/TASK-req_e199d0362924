package com.rescuehub.scheduler;

import com.rescuehub.service.BackupService;
import com.rescuehub.service.IdempotencyService;
import com.rescuehub.service.RetentionService;
import com.rescuehub.service.SamplingService;
import com.rescuehub.repository.OrganizationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledJobs {

    private static final Logger log = LoggerFactory.getLogger(ScheduledJobs.class);

    private final BackupService backupService;
    private final RetentionService retentionService;
    private final IdempotencyService idempotencyService;
    private final OrganizationRepository orgRepo;

    public ScheduledJobs(BackupService backupService, RetentionService retentionService,
                          IdempotencyService idempotencyService, OrganizationRepository orgRepo) {
        this.backupService = backupService;
        this.retentionService = retentionService;
        this.idempotencyService = idempotencyService;
        this.orgRepo = orgRepo;
    }

    // Nightly backup at 2:00 AM
    @Scheduled(cron = "0 0 2 * * *")
    public void nightlyBackup() {
        log.info("Running nightly backup...");
        orgRepo.findAll().forEach(org -> {
            try {
                backupService.runBackup(org.getId(), "SYSTEM", null);
                backupService.cleanupExpired(org.getId());
            } catch (Exception e) {
                log.error("Backup failed for org {}: {}", org.getId(), e.getMessage());
            }
        });
    }

    // Monthly restore-test reminder on 1st at 3:00 AM
    @Scheduled(cron = "0 0 3 1 * *")
    public void monthlyRestoreTestReminder() {
        log.info("Monthly restore-test reminder: Operators should record a restore test this month.");
    }

    // Retention archive at 4:00 AM
    @Scheduled(cron = "0 0 4 * * *")
    public void retentionArchive() {
        log.info("Running retention archive...");
        orgRepo.findAll().forEach(org -> {
            try {
                int count = retentionService.archiveEligible(org.getId());
                if (count > 0) log.info("Archived {} patients for org {}", count, org.getId());
            } catch (Exception e) {
                log.error("Retention archive failed for org {}: {}", org.getId(), e.getMessage());
            }
        });
    }

    // Weekly sampling on Monday at 5:00 AM
    @Scheduled(cron = "0 0 5 * * 1")
    public void weeklySampling() {
        log.info("Weekly sampling job triggered. Sampling runs created on demand via API.");
    }

    // Idempotency key cleanup hourly
    @Scheduled(cron = "0 0 * * * *")
    public void idempotencyCleanup() {
        try {
            idempotencyService.cleanup();
        } catch (Exception e) {
            log.error("Idempotency cleanup failed: {}", e.getMessage());
        }
    }
}
