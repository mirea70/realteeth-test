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
    public boolean markPublished(Long imageJobId, LocalDateTime updatedAt) {
        long updated = queryFactory
                .update(imageJob)
                .set(imageJob.status, ImageJobStatus.PUBLISHED.name())
                .set(imageJob.updatedAt, updatedAt)
                .where(
                        imageJob.imageJobId.eq(imageJobId),
                        imageJob.status.eq(ImageJobStatus.PUBLISH_PENDING.name())
                )
                .execute();

        return updated == 1L;
    }

    @Override
    public boolean markDispatching(Long imageJobId, LocalDateTime updatedAt) {
        long updated = queryFactory
                .update(imageJob)
                .set(imageJob.status, ImageJobStatus.DISPATCHING.name())
                .set(imageJob.dispatchAttemptCount, imageJob.dispatchAttemptCount.add(1))
                .set(imageJob.updatedAt, updatedAt)
                .where(
                        imageJob.imageJobId.eq(imageJobId),
                        imageJob.status.eq(ImageJobStatus.PUBLISHED.name())
                )
                .execute();

        return updated == 1L;
    }

    @Override
    public boolean reschedulePoll(Long imageJobId, LocalDateTime nextPollAt, LocalDateTime updatedAt) {
        long updated = queryFactory
                .update(imageJob)
                .set(imageJob.pollAttemptCount, imageJob.pollAttemptCount.add(1))
                .set(imageJob.nextPollAt, nextPollAt)
                .set(imageJob.updatedAt, updatedAt)
                .where(
                        imageJob.imageJobId.eq(imageJobId),
                        imageJob.status.eq(ImageJobStatus.PROCESSING.name())
                )
                .execute();

        return updated == 1L;
    }
}
