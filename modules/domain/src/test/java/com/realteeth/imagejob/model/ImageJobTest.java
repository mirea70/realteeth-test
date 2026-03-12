package com.realteeth.imagejob.model;

import com.realteeth.error.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static com.realteeth.imagejob.model.ImageJobStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class ImageJobTest {
    @Test
    @DisplayName("createNew: 새로운 ImageJob을 설정한 초기값을 반영해 정상적으로 생성한다")
    void createNew_success() {
        // given
        Long id = 1L;
        String sourceImageUrl = "https://example.com/image.png";
        LocalDateTime now = LocalDateTime.now();

        // when
        ImageJob imageJob = ImageJob.createNew(id, sourceImageUrl, now);

        // then
        assertThat(imageJob).isNotNull();

        assertThat(imageJob.getId().getValue()).isEqualTo(id.longValue());
        assertThat(imageJob.getSourceImageUrl()).isEqualTo(sourceImageUrl);

        assertThat(imageJob.getStatus()).isEqualTo(ACCEPTED);

        assertThat(imageJob.getWorkerJobId()).isNull();
        assertThat(imageJob.getResult()).isNull();
        assertThat(imageJob.getFailure()).isNull();

        assertThat(imageJob.getDispatchAttemptCount()).isZero();
        assertThat(imageJob.getPollAttemptCount()).isZero();

        assertThat(imageJob.getNextPollAt()).isNull();

        assertThat(imageJob.getCreatedAt()).isEqualTo(now);
        assertThat(imageJob.getUpdatedAt()).isEqualTo(now);
    }

    @DisplayName("PublishPending로 상태 전이 시, ACCEPTED 상태에서 PUBLISH_PENDING 상태로 변경된다.")
    @Test
    void markPublishPending_success() {
        // given
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 10, 12, 0, 0);

        ImageJob imageJob = ImageJob.createNew(
                1L,
                "https://example.com/image.png",
                createdAt
        );

        assertThat(imageJob.getStatus()).isEqualTo(ACCEPTED);
        assertThat(imageJob.getUpdatedAt()).isEqualTo(createdAt);

        LocalDateTime beforeCall = LocalDateTime.now();

        // when
        imageJob.markPublishPending(beforeCall);
        LocalDateTime afterCall = LocalDateTime.now();

        // then
        assertThat(imageJob.getStatus()).isEqualTo(PUBLISH_PENDING);
        assertThat(imageJob.getUpdatedAt()).isAfterOrEqualTo(beforeCall);
        assertThat(imageJob.getUpdatedAt()).isBeforeOrEqualTo(afterCall);
    }

    @Test
    @DisplayName("PublishPending로 상태 전이 시 허용되지 않은 상태라면 DomainException이 발생한다")
    void markPublishPending_fail_whenTransitionNotAllowed() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 3, 10, 12, 0, 0);

        ImageJob imageJob = ImageJob.fromOutside(
                1L,
                "https://example.com/image.png",
                "SUCCEEDED",
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                0,
                null,
                now,
                now
        );

        // when & then
        assertThatThrownBy(() -> imageJob.markPublishPending(now))
                .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("markProcessing - DISPATCHING 상태에서 PROCESSING으로 전이하며 workerJobId와 nextPollAt을 저장한다")
    void markProcessing_success() {
        // given
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 12, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 3, 12, 10, 5);
        LocalDateTime nextPollAt = LocalDateTime.of(2026, 3, 12, 10, 10);

        ImageJob imageJob = ImageJob.fromOutside(
                1L,
                "https://source-image.com/a.jpg",
                DISPATCHING.name(),
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                0,
                null,
                createdAt,
                createdAt
        );

        // when
        imageJob.markProcessing("worker-job-123", nextPollAt, updatedAt);

        // then
        assertEquals(PROCESSING, imageJob.getStatus());
        assertEquals("worker-job-123", imageJob.getWorkerJobId());
        assertEquals(nextPollAt, imageJob.getNextPollAt());
        assertEquals(updatedAt, imageJob.getUpdatedAt());
    }

    @Test
    @DisplayName("markProcessing - 허용되지 않은 상태에서는 예외가 발생한다")
    void markProcessing_fail_whenInvalidState() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 3, 12, 10, 0);

        ImageJob imageJob = ImageJob.fromOutside(
                1L,
                "https://source-image.com/source.jpg",
                ACCEPTED.name(),
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                0,
                null,
                now,
                now
        );

        // when & then
        assertThrows(DomainException.class,
                () -> imageJob.markProcessing("worker-job-123", now.plusSeconds(5), now));
    }

    @Test
    @DisplayName("markSucceeded - PROCESSING 상태에서 SUCCEEDED로 전이하며 결과를 저장한다")
    void markSucceeded_success() {
        // given
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 12, 10, 0);
        LocalDateTime processingAt = LocalDateTime.of(2026, 3, 12, 10, 1);
        LocalDateTime succeededAt = LocalDateTime.of(2026, 3, 12, 10, 2);

        ImageJob imageJob = ImageJob.fromOutside(
                1L,
                "https://source-image.com/source.jpg",
                PROCESSING.name(),
                "worker-job-123",
                null,
                null,
                null,
                null,
                null,
                1,
                0,
                processingAt.plusSeconds(5),
                createdAt,
                processingAt
        );

        // when
        imageJob.markSucceeded("https://result-image.com/result.jpg", succeededAt);

        // then
        assertEquals(SUCCEEDED, imageJob.getStatus());
        assertNotNull(imageJob.getResult());
        assertEquals("https://result-image.com/result.jpg", imageJob.getResult().getValue());
        assertEquals(succeededAt, imageJob.getResult().getCompletedAt());
        assertEquals(succeededAt, imageJob.getUpdatedAt());
    }

    @Test
    @DisplayName("markSucceeded - PROCESSING 상태가 아니면 예외가 발생한다")
    void markSucceeded_fail_whenInvalidState() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 3, 12, 10, 0);

        ImageJob imageJob = ImageJob.fromOutside(
                1L,
                "https://source-image.com/source.jpg",
                PUBLISHED.name(),
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                0,
                null,
                now,
                now
        );

        // when & then
        assertThrows(DomainException.class,
                () -> imageJob.markSucceeded("https://result-image.com/result.jpg", now.plusMinutes(1)));
    }

    @Test
    @DisplayName("markFailed - PROCESSING 상태에서 FAILED로 전이하며 실패 정보를 저장한다")
    void markFailed_success_fromProcessing() {
        // given
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 12, 10, 0);
        LocalDateTime failedAt = LocalDateTime.of(2026, 3, 12, 10, 3);

        ImageJob imageJob = ImageJob.fromOutside(
                1L,
                "https://source-image.com/source.jpg",
                PROCESSING.name(),
                "worker-job-123",
                null,
                null,
                null,
                null,
                null,
                1,
                2,
                createdAt.plusSeconds(5),
                createdAt,
                createdAt.plusMinutes(1)
        );

        // when
        imageJob.markFailed(400, "처리 실패", failedAt);

        // then
        assertEquals(FAILED, imageJob.getStatus());
        assertNotNull(imageJob.getFailure());
        assertEquals(400, imageJob.getFailure().getCode());
        assertEquals("처리 실패", imageJob.getFailure().getMessage());
        assertEquals(failedAt, imageJob.getFailure().getFailedAt());
        assertEquals(failedAt, imageJob.getUpdatedAt());
    }

    @Test
    @DisplayName("markFailed - PUBLISH_PENDING 상태에서도 FAILED로 전이할 수 있다")
    void markFailed_success_fromPublishPending() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 3, 12, 10, 0);

        ImageJob imageJob = ImageJob.fromOutside(
                1L,
                "https://source-image.com/source.jpg",
                PUBLISH_PENDING.name(),
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                0,
                null,
                now,
                now
        );

        // when
        imageJob.markFailed(500, "발행 실패", now.plusMinutes(1));

        // then
        assertEquals(FAILED, imageJob.getStatus());
        assertNotNull(imageJob.getFailure());
        assertEquals(500, imageJob.getFailure().getCode());
        assertEquals("발행 실패", imageJob.getFailure().getMessage());
    }

    @Test
    @DisplayName("markFailed - 허용되지 않은 상태에서는 예외가 발생한다")
    void markFailed_fail_whenInvalidState() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 3, 12, 10, 0);

        ImageJob imageJob = ImageJob.fromOutside(
                1L,
                "https://source-image.com/source.jpg",
                ACCEPTED.name(),
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                0,
                null,
                now,
                now
        );

        // when & then
        assertThrows(DomainException.class,
                () -> imageJob.markFailed(500, "실패", now.plusMinutes(1)));
    }

    @Test
    @DisplayName("isDispatchable - 현재 상태가 PUBLISHED면 true를 반환한다")
    void isDispatchable_true() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 3, 12, 10, 0);

        ImageJob imageJob = ImageJob.fromOutside(
                1L,
                "https://source-image.com/source.jpg",
                PUBLISHED.name(),
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                0,
                null,
                now,
                now
        );

        // when
        boolean result = imageJob.isDispatchable();

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("isDispatchable - 현재 상태가 PUBLISHED가 아니면 false를 반환한다")
    void isDispatchable_false() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 3, 12, 10, 0);

        ImageJob imageJob = ImageJob.fromOutside(
                1L,
                "https://source-image.com/source.jpg",
                PROCESSING.name(),
                "worker-job-123",
                null,
                null,
                null,
                null,
                null,
                1,
                1,
                now.plusSeconds(5),
                now,
                now
        );

        // when
        boolean result = imageJob.isDispatchable();

        // then
        assertFalse(result);
    }
}