package com.realteeth.port.out;

import com.realteeth.outbox.OutboxEvent;

public interface OutboxPersistenceOutport {
    void insert(OutboxEvent outboxEvent);
}
