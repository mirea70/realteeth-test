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

class ImageJobPersistenceAdapterTest extends PersistenceAdapterJpaTestSupport {

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