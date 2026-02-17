package com.Omnibus.infrastructure.scheduling;

import com.Omnibus.application.port.out.IdempotencyStorePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically purges expired idempotency keys from the database.
 * <p>
 * Runs every hour. Keys older than the configured TTL (default 24h)
 * are cleaned up to prevent unbounded table growth.
 */
@Component
public class IdempotencyKeyCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyKeyCleanupScheduler.class);

    private final IdempotencyStorePort idempotencyStore;

    public IdempotencyKeyCleanupScheduler(IdempotencyStorePort idempotencyStore) {
        this.idempotencyStore = idempotencyStore;
    }

    /**
     * Purge expired idempotency keys every hour.
     */
    @Scheduled(fixedRateString = "${app.idempotency.cleanup-interval-ms:3600000}")
    public void purgeExpiredKeys() {
        log.info("Running idempotency key cleanup...");
        int deleted = idempotencyStore.deleteExpired();
        if (deleted > 0) {
            log.info("Purged {} expired idempotency key(s)", deleted);
        } else {
            log.debug("No expired idempotency keys to purge");
        }
    }
}
