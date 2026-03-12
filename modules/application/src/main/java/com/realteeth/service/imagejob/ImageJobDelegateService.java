package com.realteeth.service.imagejob;

import com.realteeth.common.DomainType;
import com.realteeth.dto.worker.WorkerProcessingInfo;
import com.realteeth.error.exception.BusinessException;
import com.realteeth.error.info.ImageJobErrorInfo;
import com.realteeth.error.info.SystemErrorInfo;
import com.realteeth.imagejob.model.ImageJob;
import com.realteeth.imagejob.model.ImageJobId;
import com.realteeth.outbox.OutboxEvent;
import com.realteeth.outbox.OutboxEventType;
import com.realteeth.port.out.*;
import com.realteeth.worker.WorkerPollEventPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ImageJobDelegateService {

    private static final Duration pollDelay = Duration.ofSeconds(40);

    private final ImageJobPersistenceOutport imageJobPersistenceOutport;
    private final OutboxPersistenceOutport outboxPersistenceOutport;
    private final WorkerOutport workerOutport;
    private final IdGenerator idGenerator;
    private final DataSerializerOutPort dataSerializerOutPort;
    private final TransactionTemplate transactionTemplate;

    public void dispatch(Long imageJobId) {
        ImageJob imageJob = imageJobPersistenceOutport.loadOne(new ImageJobId(imageJobId))
                .orElseThrow(() -> new BusinessException(ImageJobErrorInfo.NOT_FOUND));

        if(!imageJob.isDispatchable()){
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        Boolean claimed = transactionTemplate.execute(status ->
                imageJobPersistenceOutport.markDispatchingDirectly(new ImageJobId(imageJobId), now)
        );

        if(claimed == null || !claimed){
            return;
        }

        // 작업 정보 다시 가져오기 (이 시점에 DB상 카운트는 증가됨)
        ImageJob dispatchingImageJob = imageJobPersistenceOutport.loadOne(new ImageJobId(imageJobId))
                .orElseThrow(() -> new BusinessException(ImageJobErrorInfo.NOT_FOUND));

        if (dispatchingImageJob.getDispatchAttemptCount() > 5) {
            dispatchingImageJob.markFailed(500, "최대 작업 위임 시도 횟수 초과", now);
            imageJobPersistenceOutport.update(dispatchingImageJob);
            return;
        }

        // MockWorker에 작업 위임 (트랜잭션 밖에서 수행)
        String workerJobId = startWorkerOrHandleFailure(dispatchingImageJob, now);
        if(workerJobId == null){
            return;
        }

        transactionTemplate.executeWithoutResult(status -> {
            // Processing으로 변경
            dispatchingImageJob.markProcessing(workerJobId, now);
            imageJobPersistenceOutport.update(dispatchingImageJob);

            // Poll 이벤트 발행
            outboxPersistenceOutport.insert(
                    OutboxEvent.createScheduled(
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
                            now,
                            now.plus(pollDelay)
                    )
            );
        });
    }

    public void poll(Long imageJobId, String workerJobId) {
        ImageJob imageJob = imageJobPersistenceOutport.loadOne(new ImageJobId(imageJobId))
                .orElseThrow(() -> new BusinessException(ImageJobErrorInfo.NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();

        if (imageJob.getStatus().isTerminal()) {
            return;
        }

        WorkerProcessingInfo processingInfo;
        try {
            // 외부 워커 호출 (트랜잭션 밖에서 수행)
            processingInfo = workerOutport.getProcessingInfo(workerJobId);
        } catch (BusinessException e) {
            if(e.isRetryable()) {
                throw e;
            }

            transactionTemplate.executeWithoutResult(status -> {
                ImageJob innerJob = imageJobPersistenceOutport.loadOne(new ImageJobId(imageJobId))
                        .orElseThrow(() -> new BusinessException(ImageJobErrorInfo.NOT_FOUND));
                innerJob.markFailed(extractFailureCode(e), e.getMessage(), now);
                imageJobPersistenceOutport.update(innerJob);
            });
            return;
        }

        transactionTemplate.executeWithoutResult(tsStatus -> {
            ImageJob innerJob = imageJobPersistenceOutport.loadOne(new ImageJobId(imageJobId))
                    .orElseThrow(() -> new BusinessException(ImageJobErrorInfo.NOT_FOUND));

            switch (processingInfo.status()) {
                case "PROCESSING" -> {
                    if (innerJob.getPollAttemptCount() >= 5) {
                        innerJob.markFailed(500, "최대 상태 확인 시도 횟수 초과", now);
                        imageJobPersistenceOutport.update(innerJob);
                        return;
                    }

                    LocalDateTime nextPollAt = now.plus(pollDelay);

                    boolean claimed = imageJobPersistenceOutport.reschedulePoll(innerJob.getId(), now);
                    if (!claimed){
                        return;
                    }
                    // 다음 Poll 예약
                    outboxPersistenceOutport.insert(
                            OutboxEvent.createScheduled(
                                    idGenerator.nextId(),
                                    DomainType.IMAGE_JOB,
                                    imageJobId,
                                    OutboxEventType.POLL,
                                    dataSerializerOutPort.serialize(
                                            new WorkerPollEventPayload(
                                                    imageJobId,
                                                    processingInfo.jobId()
                                                    )
                                    ),
                                    now,
                                    nextPollAt
                            )
                    );
                }
                case "COMPLETED" -> {
                    innerJob.markSucceeded(processingInfo.result(), now);
                    imageJobPersistenceOutport.update(innerJob);
                }
                case "FAILED" -> {
                    innerJob.markFailed(400, "이미지 처리 위임 결과 -> 실패", now);
                    imageJobPersistenceOutport.update(innerJob);
                }
                default -> throw new BusinessException(SystemErrorInfo.WORKER_INVALID_STATUS);
            }
        });
    }

    private String startWorkerOrHandleFailure(ImageJob imageJob, LocalDateTime now) {
        try {
            String apiKey = workerOutport.getApiKey();
            return workerOutport.processStart(apiKey, imageJob.getSourceImageUrl());
        } catch (BusinessException e) {
            if (e.isRetryable()) {
                throw e;
            }

            imageJob.markFailed(extractFailureCode(e), e.getMessage(), now);
            transactionTemplate.executeWithoutResult(status -> imageJobPersistenceOutport.update(imageJob));
            return null;
        }
    }

    private int extractFailureCode(BusinessException e) {
        Object status = e.getDetails().get("status");
        if (status instanceof Integer value) {
            return value;
        }
        return 500;
    }
}
