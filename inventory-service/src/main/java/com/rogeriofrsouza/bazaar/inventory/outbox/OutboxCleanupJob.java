package com.rogeriofrsouza.bazaar.inventory.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class OutboxCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxCleanupJob.class);

    private final Outbox outbox;

    OutboxCleanupJob(Outbox outbox) {
        this.outbox = outbox;
    }

    @Scheduled(fixedDelayString = "${bazaar.outbox.cleanup-delay:1d}")
    void purgeSentMessages() {
        try {
            int deleted = outbox.purgeSent();
            if (deleted > 0) {
                log.info("Purged {} sent outbox messages", deleted);
            }
        } catch (RuntimeException ex) {
            log.warn("Could not purge sent outbox messages", ex);
        }
    }
}
