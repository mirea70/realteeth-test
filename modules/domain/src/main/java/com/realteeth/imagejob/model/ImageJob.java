package com.realteeth.imagejob.model;

import com.realteeth.error.exception.DomainException;
import com.realteeth.error.info.ImageJobErrorInfo;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDateTime;

import static com.realteeth.imagejob.model.ImageJobStatus.*;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ImageJob {
    private final ImageJobId id;
    private final String sourceImageUrl;
    private ImageJobStatus status;
    private String workerJobId;
    private ImageJobResult result;
    private ImageJobFailure failure;
    private Integer dispatchAttemptCount;
    private Integer pollAttemptCount;
    private LocalDateTime nextPollAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ImageJob createNew(Long id, String sourceImageUrl, LocalDateTime now) {
        return ImageJob.builder()
                .id(new ImageJobId(id))
                .sourceImageUrl(sourceImageUrl)
                .status(ACCEPTED)
                .workerJobId(null)
                .result(null)
                .failure(null)
                .dispatchAttemptCount(0)
                .pollAttemptCount(0)
                .nextPollAt(null)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public static ImageJob fromOutside(Long id, String sourceImageUrl, String status, String workerJobId, String resultImageUrl, LocalDateTime resultAt, Integer failureCode, String failureMessage, LocalDateTime failAt, Integer dispatchAttemptCount, Integer pollAttemptCount, LocalDateTime nextPollAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return ImageJob.builder()
                .id(new ImageJobId(id))
                .sourceImageUrl(sourceImageUrl)
                .status(ImageJobStatus.from(status))
                .workerJobId(workerJobId)
                .result(ImageJobResult.of(resultImageUrl, resultAt))
                .failure(ImageJobFailure.of(failureCode, failureMessage, failAt))
                .dispatchAttemptCount(dispatchAttemptCount)
                .pollAttemptCount(pollAttemptCount)
                .nextPollAt(nextPollAt)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    public void markPublishPending() {
        transitionTo(PUBLISH_PENDING, LocalDateTime.now());
    }

    private void transitionTo(ImageJobStatus next, LocalDateTime now) {
        if (!isAllowed(status, next)) {
            throw new DomainException(ImageJobErrorInfo.NOT_ALLOW_TRANSITION, status, next);
        }
        this.status = next;
        this.updatedAt = now;
    }

    private boolean isAllowed(ImageJobStatus current, ImageJobStatus next) {
        return switch (current) {
            case ACCEPTED -> next == PUBLISH_PENDING;
            case PUBLISH_PENDING -> next == PUBLISHED || next == FAILED;
            case PUBLISHED -> next == DISPATCHING || next == FAILED;
            case DISPATCHING -> next == PROCESSING || next == PUBLISH_PENDING || next == FAILED;
            case PROCESSING -> next == SUCCEEDED || next == FAILED;
            case SUCCEEDED, FAILED -> false;
        };
    }
}
