package com.realteeth.port.out;

import com.realteeth.outbox.OutboxEvent;

import java.util.List;

public interface OutboxPersistenceOutport {
    void insert(OutboxEvent outboxEvent);
    List<OutboxEvent> findOnPending(int limit);
    boolean markPublishingDirectly(Long outboxId);
    boolean markPublishedDirectly(Long outboxId);
    boolean markPendingAgainDirectly(Long outboxId);
}
