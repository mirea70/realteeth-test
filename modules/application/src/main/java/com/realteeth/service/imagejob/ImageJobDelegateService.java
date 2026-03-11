package com.realteeth.service.imagejob;

import com.realteeth.common.DomainType;
import com.realteeth.error.exception.BusinessException;
import com.realteeth.error.info.ImageJobErrorInfo;
import com.realteeth.imagejob.model.ImageJob;
import com.realteeth.imagejob.model.ImageJobId;
import com.realteeth.outbox.OutboxEvent;
import com.realteeth.outbox.OutboxEventType;
import com.realteeth.port.out.*;
import com.realteeth.worker.WorkerPollEventPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ImageJobDelegateService {

    private static final Duration pollDelay = Duration.ofSeconds(5);

    private final ImageJobPersistenceOutport imageJobPersistenceOutport;
    private final OutboxPersistenceOutport outboxPersistenceOutport;
    private final WorkerOutport workerOutport;
    private final IdGenerator idGenerator;
    private final DataSerializerOutPort dataSerializerOutPort;

    @Transactional
    public void dispatch(Long imageJobId) {
        ImageJob imageJob = imageJobPersistenceOutport.loadOne(new ImageJobId(imageJobId))
                .orElseThrow(() -> new BusinessException(ImageJobErrorInfo.NOT_FOUND));

        if(!imageJob.isDispatchable()){
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        boolean claimed = imageJobPersistenceOutport.markDispatchingDirectly(imageJob.getId(), now);
        if(!claimed){
            return;
        }

        // MockWorker에 작업 위임
        String apiKey = workerOutport.getApiKey();
        String workerJobId = workerOutport.processStart(apiKey, imageJob.getSourceImageUrl());


        // 작업 정보 다시 가져오기
        ImageJob dispatchingImageJob = imageJobPersistenceOutport.loadOne(new ImageJobId(imageJobId))
                .orElseThrow(() -> new BusinessException(ImageJobErrorInfo.NOT_FOUND));

        // Processing으로 변경
        dispatchingImageJob.markProcessing(workerJobId, now.plus(pollDelay));
        imageJobPersistenceOutport.update(dispatchingImageJob);

        // Poll 이벤트 발행
        outboxPersistenceOutport.insert(
                OutboxEvent.createNew(
                        idGenerator.nextId(),
                        DomainType.IMAGE_JOB,
                        imageJobId,
                        OutboxEventType.POLL,
                        dataSerializerOutPort.serialize(
                                new WorkerPollEventPayload(
                                        imageJobId,
                                        workerJobId
                                )
                        ),
                        now
                )
        );
    }
}
