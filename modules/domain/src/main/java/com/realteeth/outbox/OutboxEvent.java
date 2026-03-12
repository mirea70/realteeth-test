package com.realteeth.outbox;

import com.realteeth.common.DomainType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OutboxEvent {

    private Long id;

    private DomainType domainType;

    private Long domainId;

    private OutboxEventType type;

    private String payload;

    private OutboxStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime publishedAt;

    private LocalDateTime availableAt;

    public static OutboxEvent createNew(Long id, DomainType domainType, Long domainId, OutboxEventType type, String payload, LocalDateTime now) {
        return new OutboxEvent(
                id,
                domainType,
                domainId,
                type,
                payload,
                OutboxStatus.PENDING,
                now,
                null,
                now
        );
    }

    public static OutboxEvent createScheduled(Long id, DomainType domainType, Long domainId, OutboxEventType type, String payload, LocalDateTime now, LocalDateTime availableAt) {
        return new OutboxEvent(
                id,
                domainType,
                domainId,
                type,
                payload,
                OutboxStatus.PENDING,
                now,
                null,
                availableAt
        );
    }

    public static OutboxEvent fromOutside(Long id, DomainType domainType, Long domainId, OutboxEventType type, String payload, OutboxStatus status, LocalDateTime createdAt, LocalDateTime publishedAt, LocalDateTime availableAt) {
        return new OutboxEvent(
                id,
                domainType,
                domainId,
                type,
                payload,
                status,
                createdAt,
                publishedAt,
                availableAt != null ? availableAt : createdAt
        );
    }

    public void markPublished(LocalDateTime now) {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = now;
    }
}
