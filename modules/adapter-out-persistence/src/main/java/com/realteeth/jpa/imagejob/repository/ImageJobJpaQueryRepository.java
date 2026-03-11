package com.realteeth.jpa.imagejob.repository;

import java.time.LocalDateTime;

public interface ImageJobJpaQueryRepository {
    boolean markDispatching(Long outboxId, LocalDateTime updateTime);
}
