package com.realteeth.jpa.imagejob.entity;

import com.realteeth.imagejob.model.ImageJob;
import com.realteeth.imagejob.model.ImageJobFailure;
import com.realteeth.imagejob.model.ImageJobResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "image_job")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ImageJobJpaEntity {

    @Id
    private Long imageJobId;

    @Column(nullable = false)
    private String sourceImageUrl;

    @Column(nullable = false)
    private String status;

    private String workerJobId;

    private String resultImageUrl;

    private LocalDateTime resultAt;

    private Integer failureCode;

    private String failureMessage;

    private LocalDateTime failedAt;

    @Column(nullable = false)
    private Integer dispatchAttemptCount;

    @Column(nullable = false)
    private Integer pollAttemptCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Builder
    ImageJobJpaEntity(Long imageJobId, String sourceImageUrl, String status, String workerJobId, String resultImageUrl, LocalDateTime resultAt, Integer failureCode, String failureMessage, LocalDateTime failedAt, Integer dispatchAttemptCount, Integer pollAttemptCount, LocalDateTime nextPollAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.imageJobId = imageJobId;
        this.sourceImageUrl = sourceImageUrl;
        this.status = status;
        this.workerJobId = workerJobId;
        this.resultImageUrl = resultImageUrl;
        this.resultAt = resultAt;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
        this.failedAt = failedAt;
        this.dispatchAttemptCount = dispatchAttemptCount;
        this.pollAttemptCount = pollAttemptCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ImageJobJpaEntity from(ImageJob imageJob) {
        ImageJobResult imageJobResult = imageJob.getResult();
        ImageJobFailure imageJobFailure = imageJob.getFailure();

        return ImageJobJpaEntity.builder()
                .imageJobId(imageJob.getId().getValue())
                .sourceImageUrl(imageJob.getSourceImageUrl())
                .status(imageJob.getStatus().name())
                .workerJobId(imageJob.getWorkerJobId())
                .resultImageUrl(imageJobResult != null ? imageJobResult.getValue() : null)
                .resultAt(imageJobResult != null ? imageJobResult.getCompletedAt() : null)
                .failureCode(imageJobFailure != null ? imageJobFailure.getCode() : null)
                .failureMessage(imageJobFailure != null ? imageJobFailure.getMessage() : null)
                .failedAt(imageJobFailure != null ? imageJobFailure.getFailedAt() : null)
                .dispatchAttemptCount(imageJob.getDispatchAttemptCount())
                .pollAttemptCount(imageJob.getPollAttemptCount())
                .createdAt(imageJob.getCreatedAt())
                .updatedAt(imageJob.getUpdatedAt())
                .build();
    }

    public void apply(ImageJob imageJob) {
        ImageJobResult imageJobResult = imageJob.getResult();
        ImageJobFailure imageJobFailure = imageJob.getFailure();

        this.sourceImageUrl = imageJob.getSourceImageUrl();
        this.status = imageJob.getStatus().name();
        this.workerJobId = imageJob.getWorkerJobId();

        this.resultImageUrl = imageJobResult != null ? imageJobResult.getValue() : null;
        this.resultAt = imageJobResult != null ? imageJobResult.getCompletedAt() : null;

        this.failureCode = imageJobFailure != null ? imageJobFailure.getCode() : null;
        this.failureMessage = imageJobFailure != null ? imageJobFailure.getMessage() : null;
        this.failedAt = imageJobFailure != null ? imageJobFailure.getFailedAt() : null;

        this.dispatchAttemptCount = imageJob.getDispatchAttemptCount();
        this.pollAttemptCount = imageJob.getPollAttemptCount();

        this.createdAt = imageJob.getCreatedAt();
        this.updatedAt = imageJob.getUpdatedAt();
    }

    public ImageJob toDomain() {
        return ImageJob.fromOutside(
                imageJobId,
                sourceImageUrl,
                status,
                workerJobId,
                resultImageUrl,
                resultAt,
                failureCode,
                failureMessage,
                failedAt,
                dispatchAttemptCount,
                pollAttemptCount,
                createdAt,
                updatedAt
        );
    }
}
