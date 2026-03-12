package com.realteeth.jpa.imagejob.repository;

import com.realteeth.imagejob.model.ImageJobStatus;
import com.realteeth.jpa.imagejob.entity.ImageJobJpaEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface ImageJobJpaQueryRepository {
    boolean markPublished(Long imageJobId, LocalDateTime updatedAt);
    boolean markDispatching(Long imageJobId, LocalDateTime updatedAt);
    boolean reschedulePoll(Long imageJobId, LocalDateTime updatedAt);
    boolean markRecoveredToPending(Long imageJobId, ImageJobStatus expectedStatus, LocalDateTime updatedAt);
    List<ImageJobJpaEntity> findStuckJobs(ImageJobStatus status, LocalDateTime threshold);
}
