package com.realteeth.jpa.imagejob.repository;

import java.time.LocalDateTime;

public interface ImageJobJpaQueryRepository {
    boolean markDispatching(Long imageJobId, LocalDateTime updateTime);
    boolean reschedulePoll(Long imageJobId, LocalDateTime nextPollAt, LocalDateTime updateTime);
}
