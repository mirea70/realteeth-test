package com.realteeth.jpa.imagejob.adapter;

import com.realteeth.error.exception.BusinessException;
import com.realteeth.error.info.SystemErrorInfo;
import com.realteeth.imagejob.model.ImageJob;
import com.realteeth.imagejob.model.ImageJobId;
import com.realteeth.imagejob.model.ImageJobStatus;
import com.realteeth.jpa.imagejob.entity.ImageJobJpaEntity;
import com.realteeth.jpa.outbox.adapter.PersistenceAdapterJpaTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class ImageJobPersistenceAdapterTest extends PersistenceAdapterJpaTestSupport {

    @Test
    @DisplayName("PUBLISH_PENDING 상태이면 PUBLISHED로 변경된다")
    void markPublishedDirectly_success() {
        // given
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 12, 13, 0);

        ImageJobJpaEntity entity = createImageJobEntity(
                4L,
                ImageJobStatus.PUBLISH_PENDING,
                0,
                createdAt
        );

        entityManager.persist(entity);
        entityManager.flush();
        entityManager.clear();

        LocalDateTime updatedAt = LocalDateTime.of(2026, 3, 12, 14, 0);

        // when
        boolean result = imageJobPersistenceAdapter.markPublishedDirectly(
                new ImageJobId(4L),
                updatedAt
        );

        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(result).isTrue();

        ImageJobJpaEntity saved = entityManager.find(ImageJobJpaEntity.class, 4L);
        assertThat(saved.getStatus()).isEqualTo(ImageJobStatus.PUBLISHED.name());
        assertThat(saved.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(saved.getDispatchAttemptCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("PUBLISH_PENDING 상태가 아니면 PUBLISHED로 변경되지 않고 false를 반환한다")
    void markPublishedDirectly_fail_whenStatusIsNotPublishPending() {
        // given
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 12, 13, 30);

        ImageJobJpaEntity entity = createImageJobEntity(
                5L,
                ImageJobStatus.ACCEPTED,
                0,
                createdAt
        );

        entityManager.persist(entity);
        entityManager.flush();
        entityManager.clear();

        LocalDateTime updatedAt = LocalDateTime.of(2026, 3, 12, 14, 30);

        // when
        boolean result = imageJobPersistenceAdapter.markPublishedDirectly(
                new ImageJobId(5L),
                updatedAt
        );

        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(result).isFalse();

        ImageJobJpaEntity saved = entityManager.find(ImageJobJpaEntity.class, 5L);
        assertThat(saved.getStatus()).isEqualTo(ImageJobStatus.ACCEPTED.name());
        assertThat(saved.getUpdatedAt()).isEqualTo(createdAt);
        assertThat(saved.getDispatchAttemptCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("PUBLISHED 상태이면 DISPATCHING으로 변경되고 dispatchAttemptCount가 1 증가한다")
    void markDispatchingDirectly_success() {
        // given
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 11, 10, 0);

        ImageJobJpaEntity entity = createImageJobEntity(
                1L,
                ImageJobStatus.PUBLISHED,
                0,
                createdAt
        );

        entityManager.persist(entity);
        entityManager.flush();
        entityManager.clear();

        LocalDateTime updatedAt = LocalDateTime.of(2026, 3, 11, 11, 0);

        // when
        boolean result = imageJobPersistenceAdapter.markDispatchingDirectly(
                new ImageJobId(1L),
                updatedAt
        );

        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(result).isTrue();

        ImageJobJpaEntity saved = entityManager.find(ImageJobJpaEntity.class, 1L);
        assertThat(saved.getStatus()).isEqualTo(ImageJobStatus.DISPATCHING.name());
        assertThat(saved.getDispatchAttemptCount()).isEqualTo(1);
        assertThat(saved.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("PUBLISHED 상태가 아니면 변경되지 않고 false를 반환한다")
    void markDispatchingDirectly_fail_whenStatusIsNotPublished() {
        // given
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 11, 10, 0);

        ImageJobJpaEntity entity = createImageJobEntity(
                2L,
                ImageJobStatus.PROCESSING,
                3,
                createdAt
        );

        entityManager.persist(entity);
        entityManager.flush();
        entityManager.clear();

        LocalDateTime updatedAt = LocalDateTime.of(2026, 3, 11, 12, 0);

        // when
        boolean result = imageJobPersistenceAdapter.markDispatchingDirectly(
                new ImageJobId(2L),
                updatedAt
        );

        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(result).isFalse();

        ImageJobJpaEntity saved = entityManager.find(ImageJobJpaEntity.class, 2L);
        assertThat(saved.getStatus()).isEqualTo(ImageJobStatus.PROCESSING.name());
        assertThat(saved.getDispatchAttemptCount()).isEqualTo(3);
        assertThat(saved.getUpdatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("update 호출 시 dirty checking으로 엔티티 값이 변경된다")
    void update_success() {
        // given
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 11, 10, 0);

        ImageJobJpaEntity entity = createImageJobEntity(
                3L,
                ImageJobStatus.PROCESSING,
                1,
                createdAt
        );

        entityManager.persist(entity);
        entityManager.flush();
        entityManager.clear();

        LocalDateTime updatedAt = LocalDateTime.of(2026, 3, 11, 12, 0);

        ImageJob imageJob = getImageJob(createdAt, updatedAt);

        // when
        imageJobPersistenceAdapter.update(imageJob);

        entityManager.flush();
        entityManager.clear();

        // then
        ImageJobJpaEntity saved = entityManager.find(ImageJobJpaEntity.class, 3L);

        assertThat(saved.getStatus()).isEqualTo(ImageJobStatus.SUCCEEDED.name());
        assertThat(saved.getDispatchAttemptCount()).isEqualTo(2);
        assertThat(saved.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("존재하지 않는 Job이면 시스템 예외가 발생한다")
    void update_fail_whenJobNotFound() {
        // given
        ImageJob imageJob = getImageJob(
                LocalDateTime.now()
                , LocalDateTime.now().plusDays(1)
        );

        // when & then
        assertThatThrownBy(() -> imageJobPersistenceAdapter.update(imageJob))
                .isInstanceOf(BusinessException.class)
                .extracting("errorInfo")
                .isEqualTo(SystemErrorInfo.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("reschedulePoll - PROCESSING 상태이면 pollAttemptCount 증가 및 nextPollAt 갱신")
    void reschedulePoll_success() {
        // given
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 12, 10, 0);
        LocalDateTime nextPollAt = createdAt.plusMinutes(5);
        LocalDateTime updatedAt = createdAt.plusMinutes(1);

        ImageJobJpaEntity entity = createImageJobEntity(
                1L,
                ImageJobStatus.PROCESSING,
                1,
                createdAt
        );

        entityManager.persist(entity);
        entityManager.flush();
        entityManager.clear();

        // when
        boolean result = imageJobPersistenceAdapter.reschedulePoll(
                new ImageJobId(1L),
                nextPollAt,
                updatedAt
        );

        // then
        assertTrue(result);

        entityManager.flush();
        entityManager.clear();

        ImageJobJpaEntity updated = entityManager.find(ImageJobJpaEntity.class, 1L);

        assertEquals(1, updated.getDispatchAttemptCount());
        assertEquals(1, updated.getPollAttemptCount()); // 기존 0 -> 1
        assertEquals(nextPollAt, updated.getNextPollAt());
        assertEquals(updatedAt, updated.getUpdatedAt());
    }

    @Test
    @DisplayName("reschedulePoll - PROCESSING 상태가 아니면 업데이트되지 않고 false 반환")
    void reschedulePoll_fail_whenNotProcessing() {
        // given
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 12, 10, 0);
        LocalDateTime nextPollAt = createdAt.plusMinutes(5);
        LocalDateTime updatedAt = createdAt.plusMinutes(1);

        ImageJobJpaEntity entity = createImageJobEntity(
                2L,
                ImageJobStatus.SUCCEEDED,
                1,
                createdAt
        );

        entityManager.persist(entity);
        entityManager.flush();
        entityManager.clear();

        // when
        boolean result = imageJobPersistenceAdapter.reschedulePoll(
                new ImageJobId(2L),
                nextPollAt,
                updatedAt
        );

        // then
        assertFalse(result);

        entityManager.flush();
        entityManager.clear();

        ImageJobJpaEntity notUpdated = entityManager.find(ImageJobJpaEntity.class, 2L);

        assertEquals(0, notUpdated.getPollAttemptCount());
        assertNull(notUpdated.getNextPollAt());
        assertEquals(createdAt, notUpdated.getUpdatedAt());
    }

    @Test
    @DisplayName("reschedulePoll - 존재하지 않는 ImageJob이면 false 반환")
    void reschedulePoll_fail_whenNotExist() {
        // given
        LocalDateTime nextPollAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();

        // when
        boolean result = imageJobPersistenceAdapter.reschedulePoll(
                new ImageJobId(999L),
                nextPollAt,
                updatedAt
        );

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("loadAll - page, size 기준으로 ImageJob 목록을 조회한다")
    void loadAll_success() {
        // given
        LocalDateTime baseTime = LocalDateTime.of(2026, 3, 12, 9, 0);

        ImageJobJpaEntity entity1 = createImageJobEntity(
                10L,
                ImageJobStatus.ACCEPTED,
                0,
                baseTime
        );

        ImageJobJpaEntity entity2 = createImageJobEntity(
                11L,
                ImageJobStatus.PUBLISHED,
                1,
                baseTime.plusMinutes(1)
        );

        ImageJobJpaEntity entity3 = createImageJobEntity(
                12L,
                ImageJobStatus.PROCESSING,
                2,
                baseTime.plusMinutes(2)
        );

        entityManager.persist(entity1);
        entityManager.persist(entity2);
        entityManager.persist(entity3);
        entityManager.flush();
        entityManager.clear();

        // when
        var result = imageJobPersistenceAdapter.loadAll(0, 2);

        // then
        assertThat(result).hasSize(2);

        assertThat(result)
                .extracting(imageJob -> imageJob.getId().getValue())
                .containsExactlyInAnyOrder(10L, 11L);

        assertThat(result)
                .extracting(ImageJob::getStatus)
                .containsExactlyInAnyOrder(
                        ImageJobStatus.ACCEPTED,
                        ImageJobStatus.PUBLISHED
                );
    }

    @Test
    @DisplayName("loadAll - 저장된 데이터가 없으면 빈 리스트를 반환한다")
    void loadAll_returnEmptyList_whenNoData() {
        // when
        var result = imageJobPersistenceAdapter.loadAll(0, 10);

        // then
        assertThat(result).isEmpty();
    }

    private ImageJobJpaEntity createImageJobEntity(
            Long id,
            ImageJobStatus status,
            Integer dispatchAttemptCount,
            LocalDateTime createdAt
    ) {
        return ImageJobJpaEntity.builder()
                .imageJobId(id)
                .sourceImageUrl("https://example.com/image.jpg")
                .status(status.name())
                .dispatchAttemptCount(dispatchAttemptCount)
                .pollAttemptCount(0)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }

    private ImageJob getImageJob(LocalDateTime createdAt, LocalDateTime updatedAt) {
        return ImageJob.fromOutside(
                3L,
                "https://example.com/updated.jpg",
                ImageJobStatus.SUCCEEDED.name(),
                null,
                null,
                null,
                null,
                null,
                null,
                2,
                0,
                null,
                createdAt,
                updatedAt
        );
    }
}