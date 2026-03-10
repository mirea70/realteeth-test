package com.realteeth.jpa.outbox.adapter;

import com.realteeth.common.DomainType;
import com.realteeth.jpa.outbox.entity.OutboxJpaEntity;
import com.realteeth.outbox.OutboxEvent;
import com.realteeth.outbox.OutboxEventType;
import com.realteeth.outbox.OutboxStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxPersistenceAdapterTest extends PersistenceAdapterJpaTestSupport {

    @Autowired
    private TestEntityManager testEntityManager;

    @DisplayName("Pending 상태인 Outbox 이벤트들을 반환한다.")
    @Test
    void findOnPending() {
        // given
        Long outboxId1 = 1L;
        LocalDateTime now =  LocalDateTime.now();

        OutboxJpaEntity outboxJpaEntity1 = OutboxJpaEntity.builder()
                .id(outboxId1)
                .domainType(DomainType.IMAGE_JOB.name())
                .domainId(1L)
                .type(OutboxEventType.DISPATCH.name())
                .payload("asdasda")
                .status(OutboxStatus.PENDING.name())
                .createdAt(now)
                .publishedAt(null)
                .build();

        Long outboxId2 = 2L;

        OutboxJpaEntity outboxJpaEntity2 = OutboxJpaEntity.builder()
                .id(outboxId2)
                .domainType(DomainType.IMAGE_JOB.name())
                .domainId(1L)
                .type(OutboxEventType.DISPATCH.name())
                .payload("asdasda")
                .status(OutboxStatus.PUBLISHED.name())
                .createdAt(now)
                .publishedAt(null)
                .build();

        entityManager.persist(outboxJpaEntity1);
        entityManager.persist(outboxJpaEntity2);

        // when
        List<OutboxEvent> results = outboxPersistenceAdapter.findOnPending(5);

        // then
        assertThat(results).hasSize(1)
                .extracting(OutboxEvent::getId)
                .containsExactly(outboxId1);
    }

    @DisplayName("PENDING 상태의 Outbox를 PUBLISHING 상태로 변경한다.")
    @Test
    void markPublishing_success() {
        // given
        Long outboxId = 1L;
        LocalDateTime createdAt = LocalDateTime.now().minusMinutes(1);

        OutboxJpaEntity entity = OutboxJpaEntity.builder()
                .id(outboxId)
                .domainType(DomainType.IMAGE_JOB.name())
                .domainId(100L)
                .type(OutboxEventType.DISPATCH.name())
                .payload("{\"jobId\":100}")
                .status(OutboxStatus.PENDING.name())
                .createdAt(createdAt)
                .publishedAt(null)
                .build();

        testEntityManager.persist(entity);
        testEntityManager.flush();
        testEntityManager.clear();

        // when
        boolean result = outboxPersistenceAdapter.markPublishing(outboxId);

        // then
        assertThat(result).isTrue();

        OutboxJpaEntity updated = testEntityManager.find(OutboxJpaEntity.class, outboxId);
        assertThat(updated).isNotNull();
        assertThat(updated.getStatus()).isEqualTo(OutboxStatus.PUBLISHING.name());
        assertThat(updated.getPublishedAt()).isNotNull();
    }

    @DisplayName("PENDING 이 아닌 상태의 Outbox는 PUBLISHING 상태로 변경되지 않는다.")
    @Test
    void markPublishing_fail_when_status_is_not_pending() {
        // given
        Long outboxId = 2L;

        OutboxJpaEntity entity = OutboxJpaEntity.builder()
                .id(outboxId)
                .domainType(DomainType.IMAGE_JOB.name())
                .domainId(101L)
                .type(OutboxEventType.DISPATCH.name())
                .payload("{\"jobId\":101}")
                .status(OutboxStatus.PUBLISHED.name())
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .publishedAt(null)
                .build();

        testEntityManager.persist(entity);
        testEntityManager.flush();
        testEntityManager.clear();

        // when
        boolean result = outboxPersistenceAdapter.markPublishing(outboxId);

        // then
        assertThat(result).isFalse();

        OutboxJpaEntity notUpdated = testEntityManager.find(OutboxJpaEntity.class, outboxId);
        assertThat(notUpdated).isNotNull();
        assertThat(notUpdated.getStatus()).isEqualTo(OutboxStatus.PUBLISHED.name());
    }

    @DisplayName("PUBLISHING 상태의 Outbox를 PUBLISHED 상태로 변경한다.")
    @Test
    void markPublished_success() {
        // given
        Long outboxId = 3L;

        OutboxJpaEntity entity = OutboxJpaEntity.builder()
                .id(outboxId)
                .domainType(DomainType.IMAGE_JOB.name())
                .domainId(102L)
                .type(OutboxEventType.DISPATCH.name())
                .payload("{\"jobId\":102}")
                .status(OutboxStatus.PUBLISHING.name())
                .createdAt(LocalDateTime.now().minusMinutes(2))
                .publishedAt(null)
                .build();

        testEntityManager.persist(entity);
        testEntityManager.flush();
        testEntityManager.clear();

        // when
        boolean result = outboxPersistenceAdapter.markPublished(outboxId);

        // then
        assertThat(result).isTrue();

        OutboxJpaEntity updated = testEntityManager.find(OutboxJpaEntity.class, outboxId);
        assertThat(updated).isNotNull();
        assertThat(updated.getStatus()).isEqualTo(OutboxStatus.PUBLISHED.name());
        assertThat(updated.getPublishedAt()).isNotNull();
    }

    @DisplayName("PUBLISHING 이 아닌 상태의 Outbox는 PUBLISHED 상태로 변경되지 않는다.")
    @Test
    void markPublished_fail_when_status_is_not_publishing() {
        // given
        Long outboxId = 4L;

        OutboxJpaEntity entity = OutboxJpaEntity.builder()
                .id(outboxId)
                .domainType(DomainType.IMAGE_JOB.name())
                .domainId(103L)
                .type(OutboxEventType.DISPATCH.name())
                .payload("{\"jobId\":103}")
                .status(OutboxStatus.PENDING.name())
                .createdAt(LocalDateTime.now().minusMinutes(2))
                .publishedAt(null)
                .build();

        testEntityManager.persist(entity);
        testEntityManager.flush();
        testEntityManager.clear();

        // when
        boolean result = outboxPersistenceAdapter.markPublished(outboxId);

        // then
        assertThat(result).isFalse();

        OutboxJpaEntity notUpdated = testEntityManager.find(OutboxJpaEntity.class, outboxId);
        assertThat(notUpdated).isNotNull();
        assertThat(notUpdated.getStatus()).isEqualTo(OutboxStatus.PENDING.name());
        assertThat(notUpdated.getPublishedAt()).isNull();
    }

    @DisplayName("PUBLISHING 상태의 Outbox를 다시 PENDING 상태로 변경한다.")
    @Test
    void markPendingAgain_success() {
        // given
        Long outboxId = 5L;

        OutboxJpaEntity entity = OutboxJpaEntity.builder()
                .id(outboxId)
                .domainType(DomainType.IMAGE_JOB.name())
                .domainId(104L)
                .type(OutboxEventType.DISPATCH.name())
                .payload("{\"jobId\":104}")
                .status(OutboxStatus.PUBLISHING.name())
                .createdAt(LocalDateTime.now().minusMinutes(3))
                .publishedAt(null)
                .build();

        testEntityManager.persist(entity);
        testEntityManager.flush();
        testEntityManager.clear();

        // when
        boolean result = outboxPersistenceAdapter.markPendingAgain(outboxId);

        // then
        assertThat(result).isTrue();

        OutboxJpaEntity updated = testEntityManager.find(OutboxJpaEntity.class, outboxId);
        assertThat(updated).isNotNull();
        assertThat(updated.getStatus()).isEqualTo(OutboxStatus.PENDING.name());
        assertThat(updated.getPublishedAt()).isNotNull();
    }

    @DisplayName("PUBLISHING 이 아닌 상태의 Outbox는 다시 PENDING 상태로 변경되지 않는다.")
    @Test
    void markPendingAgain_fail_when_status_is_not_publishing() {
        // given
        Long outboxId = 6L;

        OutboxJpaEntity entity = OutboxJpaEntity.builder()
                .id(outboxId)
                .domainType(DomainType.IMAGE_JOB.name())
                .domainId(105L)
                .type(OutboxEventType.DISPATCH.name())
                .payload("{\"jobId\":105}")
                .status(OutboxStatus.PUBLISHED.name())
                .createdAt(LocalDateTime.now().minusMinutes(3))
                .publishedAt(null)
                .build();

        testEntityManager.persist(entity);
        testEntityManager.flush();
        testEntityManager.clear();

        // when
        boolean result = outboxPersistenceAdapter.markPendingAgain(outboxId);

        // then
        assertThat(result).isFalse();

        OutboxJpaEntity notUpdated = testEntityManager.find(OutboxJpaEntity.class, outboxId);
        assertThat(notUpdated).isNotNull();
        assertThat(notUpdated.getStatus()).isEqualTo(OutboxStatus.PUBLISHED.name());
    }
}