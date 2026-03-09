package com.realteeth.imagejob.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

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

        assertThat(imageJob.getStatus()).isEqualTo(ImageJobStatus.ACCEPTED);

        assertThat(imageJob.getWorkerJobId()).isNull();
        assertThat(imageJob.getResult()).isNull();
        assertThat(imageJob.getFailure()).isNull();

        assertThat(imageJob.getDispatchAttemptCount()).isZero();
        assertThat(imageJob.getPollAttemptCount()).isZero();

        assertThat(imageJob.getNextPollAt()).isNull();

        assertThat(imageJob.getCreatedAt()).isEqualTo(now);
        assertThat(imageJob.getUpdatedAt()).isEqualTo(now);
    }
}