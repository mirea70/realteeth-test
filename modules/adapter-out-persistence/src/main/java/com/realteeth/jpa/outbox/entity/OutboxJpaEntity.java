package com.realteeth.jpa.outbox.entity;

import com.realteeth.outbox.OutboxEvent;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
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

    @Column(nullable = false)
    private LocalDateTime publishedAt;

    public static OutboxJpaEntity from(OutboxEvent outboxEvent) {
        return new OutboxJpaEntity(
                outboxEvent.getId(),
                outboxEvent.getDomainType().name(),
                outboxEvent.getDomainId(),
                outboxEvent.getType(),
                outboxEvent.getPayload(),
                outboxEvent.getStatus().name(),
                outboxEvent.getCreatedAt(),
                outboxEvent.getPublishedAt()
        );
    }
}
