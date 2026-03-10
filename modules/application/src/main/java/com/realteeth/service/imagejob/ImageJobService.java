package com.realteeth.service.imagejob;

import com.realteeth.common.DomainType;
import com.realteeth.dto.response.ImageJobResponse;
import com.realteeth.error.exception.BusinessException;
import com.realteeth.error.info.ImageJobErrorInfo;
import com.realteeth.imagejob.model.ImageJob;
import com.realteeth.imagejob.model.ImageJobId;
import com.realteeth.outbox.OutboxEvent;
import com.realteeth.port.in.ImageJobUseCase;
import com.realteeth.port.out.DataSerializerOutPort;
import com.realteeth.port.out.IdGenerator;
import com.realteeth.port.out.ImageJobPersistenceOutport;
import com.realteeth.port.out.OutboxPersistenceOutport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class ImageJobService implements ImageJobUseCase {
    private final ImageJobPersistenceOutport imageJobPersistenceOutport;
    private final IdGenerator idGenerator;
    private final DataSerializerOutPort dataSerializerOutPort;
    private final OutboxPersistenceOutport outboxPersistenceOutport;

    @Transactional(readOnly = true)
    public ImageJobResponse readOne(Long requestImageJobId) {
        ImageJob imageJob = imageJobPersistenceOutport.loadOne(new ImageJobId(requestImageJobId))
                .orElseThrow(() -> new BusinessException(ImageJobErrorInfo.NOT_FOUND));

        return ImageJobResponse.from(imageJob);
    }

    public ImageJobResponse register(String sourceImageUrl) {
        Long imageJobIdValue = idGenerator.nextId();
        LocalDateTime now = LocalDateTime.now();

        ImageJob imageJob = ImageJob.createNew(
                imageJobIdValue,
                sourceImageUrl,
                now
        );

        imageJob.markPublishPending();
        ImageJob result = imageJobPersistenceOutport.insert(imageJob);
        outboxPersistenceOutport.insert(
                OutboxEvent.createNew(
                        idGenerator.nextId(),
                        DomainType.IMAGE_JOB,
                        imageJobIdValue,
                        "IMAGEGOB_DISPATCH",
                        dataSerializerOutPort.serialize(imageJob),
                        now
                )
        );

        return ImageJobResponse.from(result);
    }
}
