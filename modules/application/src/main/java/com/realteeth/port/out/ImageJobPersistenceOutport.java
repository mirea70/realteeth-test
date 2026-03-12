package com.realteeth.port.out;

import com.realteeth.imagejob.model.ImageJob;
import com.realteeth.imagejob.model.ImageJobId;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ImageJobPersistenceOutport {
    Optional<ImageJob> loadOne(ImageJobId id);
    ImageJob insert(ImageJob imageJob);
    boolean markDispatchingDirectly(ImageJobId imageJobId, LocalDateTime updatedAt);
    void update(ImageJob imageJob);
    boolean reschedulePoll(ImageJobId imageJobId, LocalDateTime nextPollAt, LocalDateTime updatedAt);
}
