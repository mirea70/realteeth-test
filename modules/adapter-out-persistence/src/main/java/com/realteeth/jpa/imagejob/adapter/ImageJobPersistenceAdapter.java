package com.realteeth.jpa.imagejob.adapter;

import com.realteeth.error.exception.BusinessException;
import com.realteeth.error.info.SystemErrorInfo;
import com.realteeth.imagejob.model.ImageJob;
import com.realteeth.imagejob.model.ImageJobId;
import com.realteeth.imagejob.model.ImageJobStatus;
import com.realteeth.jpa.imagejob.entity.ImageJobJpaEntity;
import com.realteeth.jpa.imagejob.repository.ImageJobJpaRepository;
import com.realteeth.port.out.ImageJobPersistenceOutport;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ImageJobPersistenceAdapter implements ImageJobPersistenceOutport {
    private final ImageJobJpaRepository imageJobJpaRepository;
    private final EntityManager entityManager;

    @Override
    public ImageJob insert(ImageJob imageJob) {
        entityManager.persist(ImageJobJpaEntity.from(imageJob));
        return imageJob;
    }

    @Override
    public boolean markPublishedDirectly(ImageJobId imageJobId, LocalDateTime updatedAt) {
        boolean result = imageJobJpaRepository.markPublished(imageJobId.getValue(), updatedAt);
        entityManager.flush();
        entityManager.clear();
        return result;
    }

    @Override
    public Optional<ImageJob> loadOne(ImageJobId id) {
        return imageJobJpaRepository.findById(id.getValue())
                .map(ImageJobJpaEntity::toDomain);
    }

    @Override
    public List<ImageJob> loadAll(int page, int size) {
        return imageJobJpaRepository.findAll(PageRequest.of(page, size))
                .stream().map(ImageJobJpaEntity::toDomain)
                .toList();
    }

    @Override
    public boolean markDispatchingDirectly(ImageJobId imageJobId, LocalDateTime updatedAt) {
        boolean result = imageJobJpaRepository.markDispatching(imageJobId.getValue(), updatedAt);
        entityManager.flush();
        entityManager.clear();
        return result;
    }

    @Transactional
    public void update(ImageJob imageJob) {
        ImageJobJpaEntity loadedJobJpaEntity = imageJobJpaRepository.findById(imageJob.getId().getValue())
                        .orElseThrow(() -> new BusinessException(SystemErrorInfo.INTERNAL_SERVER_ERROR));

        loadedJobJpaEntity.apply(imageJob);
    }

    @Override
    public boolean reschedulePoll(ImageJobId imageJobId, LocalDateTime updatedAt) {
        boolean result = imageJobJpaRepository.reschedulePoll(imageJobId.getValue(), updatedAt);
        entityManager.flush();
        entityManager.clear();
        return result;
    }

    @Override
    public boolean markRecoveredToPendingDirectly(ImageJobId imageJobId, ImageJobStatus expectedStatus, LocalDateTime updatedAt) {
        boolean result = imageJobJpaRepository.markRecoveredToPending(imageJobId.getValue(), expectedStatus, updatedAt);
        entityManager.flush();
        entityManager.clear();
        return result;
    }

    @Override
    public List<ImageJob> findStuckJobs(ImageJobStatus status, LocalDateTime threshold) {
        return imageJobJpaRepository.findStuckJobs(status, threshold)
                .stream()
                .map(ImageJobJpaEntity::toDomain)
                .toList();
    }
}
