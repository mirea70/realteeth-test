package com.realteeth.jpa.outbox.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.realteeth.jpa.outbox.entity.OutboxJpaEntity;
import com.realteeth.jpa.outbox.entity.QOutboxJpaEntity;
import com.realteeth.outbox.OutboxStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class OutboxJpaQueryRepositoryImpl implements OutboxJpaQueryRepository {
    private final JPAQueryFactory queryFactory;
    private final QOutboxJpaEntity outboxEvent = QOutboxJpaEntity.outboxJpaEntity;

    @Override
    public List<OutboxJpaEntity> findOnPending(PageRequest pageRequest) {
        return queryFactory
                .selectFrom(outboxEvent)
                .where(outboxEvent.status.eq(OutboxStatus.PENDING.name()))
                .orderBy(outboxEvent.createdAt.asc())
                .offset(pageRequest.getOffset())
                .limit(pageRequest.getPageSize())
                .fetch();
    }

    @Override
    public boolean markPublishing(Long outboxId) {
        long updated = queryFactory
                .update(outboxEvent)
                .set(outboxEvent.status, OutboxStatus.PUBLISHING.name())
                .set(outboxEvent.publishedAt, LocalDateTime.now())
                .where(
                        outboxEvent.id.eq(outboxId),
                        outboxEvent.status.eq(OutboxStatus.PENDING.name())
                )
                .execute();

        return updated == 1L;
    }

    @Override
    public boolean markPublished(Long outboxId) {
        long updated = queryFactory
                .update(outboxEvent)
                .set(outboxEvent.status, OutboxStatus.PUBLISHED.name())
                .set(outboxEvent.publishedAt, LocalDateTime.now())
                .where(
                        outboxEvent.id.eq(outboxId),
                        outboxEvent.status.eq(OutboxStatus.PUBLISHING.name())
                )
                .execute();

        return updated == 1L;
    }

    @Override
    public boolean markPendingAgain(Long outboxId) {
        long updated = queryFactory
                .update(outboxEvent)
                .set(outboxEvent.status, OutboxStatus.PENDING.name())
                .set(outboxEvent.publishedAt, LocalDateTime.now())
                .where(
                        outboxEvent.id.eq(outboxId),
                        outboxEvent.status.eq(OutboxStatus.PUBLISHING.name())
                )
                .execute();

        return updated == 1L;
    }
}
