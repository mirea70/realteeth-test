package com.realteeth.jpa.imagejob.adapter;

import com.realteeth.error.exception.BusinessException;
import com.realteeth.error.info.SystemErrorInfo;
import com.realteeth.imagejob.model.ImageJob;
import com.realteeth.imagejob.model.ImageJobId;
import com.realteeth.jpa.imagejob.entity.ImageJobJpaEntity;
import com.realteeth.jpa.imagejob.repository.ImageJobJpaRepository;
import com.realteeth.port.out.ImageJobPersistenceOutport;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    public Optional<ImageJob> loadOne(ImageJobId id) {
        return imageJobJpaRepository.findById(id.getValue())
                .map(ImageJobJpaEntity::toDomain);
    }

    @Override
    public boolean markDispatchingDirectly(ImageJobId imageJobId, LocalDateTime updatedAt) {
        return imageJobJpaRepository.markDispatching(imageJobId.getValue(), updatedAt);
    }

    @Transactional
    public void update(ImageJob imageJob) {
        ImageJobJpaEntity loadedJobJpaEntity = imageJobJpaRepository.findById(imageJob.getId().getValue())
                        .orElseThrow(() -> new BusinessException(SystemErrorInfo.INTERNAL_SERVER_ERROR));

        loadedJobJpaEntity.apply(imageJob);
    }
}
