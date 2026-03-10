package com.realteeth.jpa.outbox.adapter;

import com.realteeth.jpa.outbox.entity.OutboxJpaEntity;
import com.realteeth.jpa.outbox.repository.OutboxJpaRepository;
import com.realteeth.outbox.OutboxEvent;
import com.realteeth.port.out.OutboxPersistenceOutport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxPersistenceAdapter implements OutboxPersistenceOutport {
    private final OutboxJpaRepository outboxJpaRepository;


    @Override
    public void insert(OutboxEvent outboxEvent) {
        outboxJpaRepository.save(OutboxJpaEntity.from(outboxEvent));
    }
}
