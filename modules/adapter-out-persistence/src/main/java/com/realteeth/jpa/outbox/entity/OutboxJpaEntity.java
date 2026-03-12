package com.realteeth.jpa.outbox.entity;

import com.realteeth.common.DomainType;
import com.realteeth.outbox.OutboxEvent;
import com.realteeth.outbox.OutboxEventType;
import com.realteeth.outbox.OutboxStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Builder
@Getter
public class OutboxJpaEntity {

    @Id
    private Long id;

    @Column(nullable = false)
    private String domainType;

    @Column(nullable = false)
    private Long domainId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime publishedAt;

    @Column(nullable = false)
    private LocalDateTime availableAt;

    public static OutboxJpaEntity from(OutboxEvent outboxEvent) {
        return new OutboxJpaEntity(
                outboxEvent.getId(),
                outboxEvent.getDomainType().name(),
                outboxEvent.getDomainId(),
                outboxEvent.getType().name(),
                outboxEvent.getPayload(),
                outboxEvent.getStatus().name(),
                outboxEvent.getCreatedAt(),
                outboxEvent.getPublishedAt(),
                outboxEvent.getAvailableAt()
        );
    }

    public OutboxEvent toDomain() {
        return OutboxEvent.fromOutside(
                id,
                DomainType.from(domainType),
                domainId,
                OutboxEventType.from(type),
                payload,
                OutboxStatus.from(status),
                createdAt,
                publishedAt,
                availableAt
        );
    }
}
