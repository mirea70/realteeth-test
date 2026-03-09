package com.realteeth.jpa.imagejob.adapter;

import com.realteeth.imagejob.model.ImageJob;
import com.realteeth.imagejob.model.ImageJobId;
import com.realteeth.jpa.imagejob.entity.ImageJobJpaEntity;
import com.realteeth.jpa.imagejob.repository.ImageJobJpaRepository;
import com.realteeth.port.out.ImageJobPersistenceOutport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ImageJobPersistenceAdapter implements ImageJobPersistenceOutport {
    private final ImageJobJpaRepository imageJobJpaRepository;

    public void insert(ImageJob imageJob) {
        imageJobJpaRepository.save(ImageJobJpaEntity.from(imageJob));
    }

    public Optional<ImageJob> loadOne(ImageJobId id) {
        return imageJobJpaRepository.findById(id.getValue())
                .map(ImageJobJpaEntity::toDomain);
    }
}
