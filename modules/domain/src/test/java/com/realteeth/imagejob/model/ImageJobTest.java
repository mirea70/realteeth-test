package com.realteeth.imagejob.model;

import com.realteeth.error.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static com.realteeth.imagejob.model.ImageJobStatus.ACCEPTED;
import static com.realteeth.imagejob.model.ImageJobStatus.PUBLISH_PENDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        imageJob.markPublishPending();
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
        assertThatThrownBy(imageJob::markPublishPending)
                .isInstanceOf(DomainException.class);
    }
}