package com.realteeth.port.out;

import com.realteeth.common.DomainType;
import com.realteeth.outbox.OutboxEventType;

public interface MessagePublisher {
    void publish(DomainType domainType, OutboxEventType eventType, String payload);
}
