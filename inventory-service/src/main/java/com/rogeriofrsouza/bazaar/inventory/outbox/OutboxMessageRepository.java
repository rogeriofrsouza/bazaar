package com.rogeriofrsouza.bazaar.inventory.outbox;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

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
}
