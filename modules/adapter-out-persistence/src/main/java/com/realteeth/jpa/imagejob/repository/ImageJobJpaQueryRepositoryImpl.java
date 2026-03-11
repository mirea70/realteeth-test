package com.realteeth.jpa.imagejob.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.realteeth.imagejob.model.ImageJobStatus;
import com.realteeth.jpa.imagejob.entity.QImageJobJpaEntity;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class ImageJobJpaQueryRepositoryImpl implements ImageJobJpaQueryRepository {
    private final JPAQueryFactory queryFactory;
    private final QImageJobJpaEntity imageJob = QImageJobJpaEntity.imageJobJpaEntity;


    @Override
    public boolean markDispatching(Long imageJobId, LocalDateTime updateTime) {
        long updated = queryFactory
                .update(imageJob)
                .set(imageJob.status, ImageJobStatus.DISPATCHING.name())
                .set(imageJob.dispatchAttemptCount, imageJob.dispatchAttemptCount.add(1))
                .set(imageJob.updatedAt, updateTime)
                .where(
                        imageJob.imageJobId.eq(imageJobId),
                        imageJob.status.eq(ImageJobStatus.PUBLISHED.name())
                )
                .execute();

        return updated == 1L;
    }
}
