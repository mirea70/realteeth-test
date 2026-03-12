package com.realteeth.jpa.imagejob.repository;

import java.time.LocalDateTime;

public interface ImageJobJpaQueryRepository {
    boolean markPublished(Long imageJobId, LocalDateTime updatedAt);
    boolean markDispatching(Long imageJobId, LocalDateTime updatedAt);
    boolean reschedulePoll(Long imageJobId, LocalDateTime nextPollAt, LocalDateTime updatedAt);
}
