package com.realteeth.port.out;

import com.realteeth.imagejob.model.ImageJob;
import com.realteeth.imagejob.model.ImageJobId;
import com.realteeth.imagejob.model.ImageJobStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ImageJobPersistenceOutport {
    Optional<ImageJob> loadOne(ImageJobId id);
    List<ImageJob> loadAll(int page, int size);
    ImageJob insert(ImageJob imageJob);
    boolean markPublishedDirectly(ImageJobId imageJobId, LocalDateTime updatedAt);
    boolean markDispatchingDirectly(ImageJobId imageJobId, LocalDateTime updatedAt);
    void update(ImageJob imageJob);
    boolean reschedulePoll(ImageJobId imageJobId, LocalDateTime updatedAt);
    boolean markRecoveredToPendingDirectly(ImageJobId imageJobId, ImageJobStatus expectedStatus, LocalDateTime updatedAt);
    List<ImageJob> findStuckJobs(ImageJobStatus status, LocalDateTime threshold);
}
