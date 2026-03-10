package com.realteeth.jpa.outbox.repository;

import com.realteeth.jpa.outbox.entity.OutboxJpaEntity;
import org.springframework.data.domain.PageRequest;

import java.util.List;

public interface OutboxJpaQueryRepository {
    List<OutboxJpaEntity> findOnPending(PageRequest pageRequest);
    boolean markPublishing(Long outboxId);
    boolean markPublished(Long outboxId);
    boolean markPendingAgain(Long outboxId);
}
