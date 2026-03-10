package com.realteeth.outbox;

import com.realteeth.common.DomainType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class OutboxEventTest {

    @DisplayName("Outbox 이벤트가 생성될 때, 설정한 초기값대로 잘 생성된다.")
    @Test
    void factory() {
        // given
        Long id = 1L;
        DomainType domainType = DomainType.IMAGE_JOB;
        Long domainId = 33L;
        OutboxEventType type = OutboxEventType.DISPATCH;
        String payload = "{example:31}";
        LocalDateTime now = LocalDateTime.now();

        // when
        OutboxEvent result = OutboxEvent.createNew(
                id,
                domainType,
                domainId,
                type,
                payload,
                now
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);

        assertThat(result.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(result.getCreatedAt()).isEqualTo(now);
        assertThat(result.getPublishedAt()).isNull();

    }

    @DisplayName("이벤트 발행 시, 객체의 상태변경이 잘 수행된다.")
    @Test
    void markPublished() {
        // given
        Long id = 1L;
        DomainType domainType = DomainType.IMAGE_JOB;
        Long domainId = 33L;
        OutboxEventType type = OutboxEventType.DISPATCH;
        String payload = "{example:31}";
        LocalDateTime created = LocalDateTime.now();

        OutboxEvent before = OutboxEvent.createNew(
                id,
                domainType,
                domainId,
                type,
                payload,
                created
        );

        LocalDateTime now = LocalDateTime.now();

        // before
        assertThat(before.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(before.getPublishedAt()).isNull();

        // when
        before.markPublished(now);

        // then
        assertThat(before.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(before.getPublishedAt()).isNotNull();
        assertThat(before.getPublishedAt()).isEqualTo(now);
    }
}