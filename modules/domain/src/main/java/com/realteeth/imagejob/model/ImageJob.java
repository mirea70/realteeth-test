package com.realteeth.imagejob.model;

import com.realteeth.error.exception.DomainException;
import com.realteeth.error.info.ImageJobErrorInfo;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

import static com.realteeth.imagejob.model.ImageJobStatus.*;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ImageJob {
    private final ImageJobId id;
    private final String sourceImageUrl;
    private ImageJobStatus status;
    private String workerJobId;
    private ImageJobResult result;
    private ImageJobFailure failure;
    private Integer dispatchAttemptCount;
    private Integer pollAttemptCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ImageJob createNew(Long id, String sourceImageUrl, LocalDateTime now) {
        return new ImageJob(
                new ImageJobId(id),
                sourceImageUrl,
                ACCEPTED,
                null,
                null,
                null,
                0,
                0,
                now,
                now
        );
    }

    public static ImageJob fromOutside(Long id, String sourceImageUrl, String status, String workerJobId, String resultImageUrl, LocalDateTime resultAt, Integer failureCode, String failureMessage, LocalDateTime failAt, Integer dispatchAttemptCount, Integer pollAttemptCount, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new ImageJob(
                new ImageJobId(id),
                sourceImageUrl,
                ImageJobStatus.from(status),
                workerJobId,
                ImageJobResult.of(resultImageUrl, resultAt),
                ImageJobFailure.of(failureCode, failureMessage, failAt),
                dispatchAttemptCount,
                pollAttemptCount,
                createdAt,
                updatedAt
        );
    }

    public void markPublishPending(LocalDateTime updatedAt) {
        transitionTo(PUBLISH_PENDING, updatedAt);
    }

    public void markProcessing(String workerJobId, LocalDateTime updatedAt) {
        transitionTo(PROCESSING, updatedAt);
        this.workerJobId = workerJobId;
    }

    public void markSucceeded(String resultImageUrl, LocalDateTime updatedAt) {
        transitionTo(SUCCEEDED, updatedAt);
        this.result = ImageJobResult.of(resultImageUrl, updatedAt);
    }

    public void markFailed(Integer code, String message, LocalDateTime updatedAt) {
        transitionTo(FAILED, updatedAt);
        this.failure = ImageJobFailure.of(code, message, updatedAt);
    }

    public boolean isDispatchable() {
        return isAllowed(status, DISPATCHING);
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
            case PUBLISHED -> next == DISPATCHING || next == PUBLISH_PENDING || next == FAILED;
            case DISPATCHING -> next == PROCESSING || next == PUBLISH_PENDING || next == FAILED;
            case PROCESSING -> next == SUCCEEDED || next == FAILED;
            case SUCCEEDED, FAILED -> false;
        };
    }
}
