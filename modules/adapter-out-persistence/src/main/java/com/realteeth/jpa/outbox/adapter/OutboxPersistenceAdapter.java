package com.realteeth.jpa.outbox.adapter;

import com.realteeth.jpa.outbox.entity.OutboxJpaEntity;
import com.realteeth.jpa.outbox.repository.OutboxJpaRepository;
import com.realteeth.outbox.OutboxEvent;
import com.realteeth.port.out.OutboxPersistenceOutport;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPersistenceAdapter implements OutboxPersistenceOutport {
    private final OutboxJpaRepository outboxJpaRepository;
    private final EntityManager entityManager;

    @Override
    public void insert(OutboxEvent outboxEvent) {
        entityManager.persist(outboxEvent);
    }

    @Override
    public List<OutboxEvent> findOnPending(int limit) {
        return outboxJpaRepository.findOnPending(PageRequest.of(0, limit))
                .stream()
                .map(OutboxJpaEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public boolean markPublishingDirectly(Long outboxId) {
        log.info("markPublishing called. outboxId={}", outboxId);
        return outboxJpaRepository.markPublishing(outboxId);
    }

    @Override
    @Transactional
    public boolean markPublishedDirectly(Long outboxId) {
        return outboxJpaRepository.markPublished(outboxId);
    }

    @Override
    @Transactional
    public boolean markPendingAgainDirectly(Long outboxId) {
        return outboxJpaRepository.markPendingAgain(outboxId);
    }
}
