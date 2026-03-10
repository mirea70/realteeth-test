package com.realteeth.port.out;

import com.realteeth.outbox.OutboxEvent;

import java.util.List;

public interface OutboxPersistenceOutport {
    void insert(OutboxEvent outboxEvent);
    List<OutboxEvent> findOnPending(int limit);
    boolean markPublishing(Long outboxId);
    boolean markPublished(Long outboxId);
    boolean markPendingAgain(Long outboxId);
}
