package com.realteeth.dto.response;

import com.realteeth.imagejob.model.ImageJob;
import com.realteeth.imagejob.model.ImageJobFailure;
import com.realteeth.imagejob.model.ImageJobResult;

import java.time.LocalDateTime;
import java.util.Objects;

public record ImageJobResponse(
        Long imageJobId,
        String sourceImageUrl,
        String status,
        String resultImageUrl,
        LocalDateTime resultAt,
        Integer failureCode,
        String failureMessage,
        LocalDateTime failedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ImageJobResponse from(ImageJob imageJob) {
        Objects.requireNonNull(imageJob, "ImageJob must not be null");

        ImageJobResult result = imageJob.getResult();
        ImageJobFailure failureInfo = imageJob.getFailure();

        return new ImageJobResponse(
                imageJob.getId().getValue(),
                imageJob.getSourceImageUrl(),
                imageJob.getStatus().name(),
                result != null ? result.getValue() : null,
                result != null ? result.getCompletedAt() : null,
                failureInfo != null ? failureInfo.getCode() : null,
                failureInfo != null ? failureInfo.getMessage() : null,
                failureInfo != null ? failureInfo.getFailedAt() : null,
                imageJob.getCreatedAt(),
                imageJob.getUpdatedAt()
        );
    }
}
