package com.realteeth.service.imagejob;

import com.realteeth.common.DomainType;
import com.realteeth.imagejob.model.ImageJob;
import com.realteeth.imagejob.model.ImageJobStatus;
import com.realteeth.outbox.OutboxEvent;
import com.realteeth.outbox.OutboxEventType;
import com.realteeth.port.out.DataSerializerOutPort;
import com.realteeth.port.out.IdGenerator;
import com.realteeth.port.out.ImageJobPersistenceOutport;
import com.realteeth.port.out.OutboxPersistenceOutport;
import com.realteeth.worker.WorkerDispatchEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageJobRecoveryService {

    private final ImageJobPersistenceOutport imageJobPersistenceOutport;
    private final OutboxPersistenceOutport outboxPersistenceOutport;
    private final ImageJobDelegateService imageJobDelegateService;
    private final IdGenerator idGenerator;
    private final DataSerializerOutPort dataSerializerOutPort;

    @Transactional
    public void recoverDispatchingJobs() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minusMinutes(2);

        List<ImageJob> stuckJobs = imageJobPersistenceOutport.findStuckJobs(ImageJobStatus.DISPATCHING, threshold);

        for (ImageJob job : stuckJobs) {
            log.info("Recovering stuck DISPATCHING job: {}", job.getId().getValue());

            if (job.getDispatchAttemptCount() > 5) {
                job.markFailed(500, "최대 작업 위임 시도 횟수 초과 (복구 과정 중 감지)", now);
                imageJobPersistenceOutport.update(job);
                continue;
            }

            // 재발행을 위해 상태 정리 및 OutboxEvent 발행
            job.markPublishPending(now);
            imageJobPersistenceOutport.update(job);

            outboxPersistenceOutport.insert(
                    OutboxEvent.createNew(
                            idGenerator.nextId(),
                            DomainType.IMAGE_JOB,
                            job.getId().getValue(),
                            OutboxEventType.DISPATCH,
                            dataSerializerOutPort.serialize(
                                    new WorkerDispatchEventPayload(job.getId().getValue())
                            ),
                            now
                    )
            );
        }
    }

    @Transactional
    public void recoverPublishedJobs() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minusMinutes(1);

        List<ImageJob> stuckJobs = imageJobPersistenceOutport.findStuckJobs(ImageJobStatus.PUBLISHED, threshold);

        for (ImageJob job : stuckJobs) {
            log.info("Recovering stuck PUBLISHED job: {}", job.getId().getValue());

            // 다시 PUBLISH_PENDING으로 돌려보내고 Outbox 재발행
            job.markPublishPending(now);
            imageJobPersistenceOutport.update(job);

            outboxPersistenceOutport.insert(
                    OutboxEvent.createNew(
                            idGenerator.nextId(),
                            DomainType.IMAGE_JOB,
                            job.getId().getValue(),
                            OutboxEventType.DISPATCH,
                            dataSerializerOutPort.serialize(
                                    new WorkerDispatchEventPayload(job.getId().getValue())
                            ),
                            now
                    )
            );
        }
    }

    @Transactional
    public void recoverProcessingJobs() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minusMinutes(5);

        List<ImageJob> stuckJobs = imageJobPersistenceOutport.findStuckJobs(ImageJobStatus.PROCESSING, threshold);

        for (ImageJob job : stuckJobs) {
            log.info("복구작업 수행 ImageJob: {}", job.getId().getValue());
            imageJobDelegateService.poll(job.getId().getValue(), job.getWorkerJobId());
        }
    }
}
