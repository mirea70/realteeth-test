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

    public static OutboxEvent createNew(Long id, DomainType domainType, Long domainId, OutboxEventType type, String payload, LocalDateTime now) {
        return new OutboxEvent(
                id,
                domainType,
                domainId,
                type,
                payload,
                OutboxStatus.PENDING,
                now,
                null
        );
    }

    public static OutboxEvent fromOutside(Long id, DomainType domainType, Long domainId, OutboxEventType type, String payload, OutboxStatus status, LocalDateTime createdAt, LocalDateTime publishedAt) {
        return new OutboxEvent(
                id,
                domainType,
                domainId,
                type,
                payload,
                status,
                createdAt,
                publishedAt
        );
    }

    public void markPublished(LocalDateTime now) {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = now;
    }
}
