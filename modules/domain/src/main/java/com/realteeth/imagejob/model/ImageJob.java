package com.realteeth.imagejob.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

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
                .status(ImageJobStatus.ACCEPTED)
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
}
