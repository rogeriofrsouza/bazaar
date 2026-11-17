package com.rogeriofrsouza.bazaar.order.outbox;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxMessageRepository extends ListCrudRepository<OutboxMessage, UUID> {

    @Query("""
            SELECT * FROM outbox_message
            WHERE sent_at IS NULL
            ORDER BY created_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED""")
    List<OutboxMessage> findPendingForUpdate(int limit);

    @Modifying
    @Query("""
            DELETE FROM outbox_message
            WHERE id IN (
                SELECT id FROM outbox_message
                WHERE sent_at < :cutoff
                ORDER BY sent_at
                LIMIT :limit
                FOR UPDATE SKIP LOCKED)""")
    int deleteSentBefore(Instant cutoff, int limit);
}
