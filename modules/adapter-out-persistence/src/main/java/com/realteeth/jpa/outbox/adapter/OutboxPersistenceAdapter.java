package com.realteeth.jpa.outbox.adapter;

import com.realteeth.jpa.outbox.entity.OutboxJpaEntity;
import com.realteeth.jpa.outbox.repository.OutboxJpaRepository;
import com.realteeth.outbox.OutboxEvent;
import com.realteeth.port.out.OutboxPersistenceOutport;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxPersistenceAdapter implements OutboxPersistenceOutport {
    private final OutboxJpaRepository outboxJpaRepository;

    @Override
    public void insert(OutboxEvent outboxEvent) {
        outboxJpaRepository.save(OutboxJpaEntity.from(outboxEvent));
    }

    @Override
    public List<OutboxEvent> findOnPending(int limit) {
        return outboxJpaRepository.findOnPending(PageRequest.of(0, limit))
                .stream()
                .map(OutboxJpaEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public boolean markPublishing(Long outboxId) {
        return outboxJpaRepository.markPublishing(outboxId);
    }

    @Override
    @Transactional
    public boolean markPublished(Long outboxId) {
        return outboxJpaRepository.markPublished(outboxId);
    }

    @Override
    @Transactional
    public boolean markPendingAgain(Long outboxId) {
        return outboxJpaRepository.markPendingAgain(outboxId);
    }
}
