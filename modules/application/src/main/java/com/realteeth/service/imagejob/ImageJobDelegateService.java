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
        String workerJobId = startWorkerOrHandleFailure(imageJobId, imageJob, now);
        if(workerJobId == null){
            return;
        }

        // 작업 정보 다시 가져오기
        ImageJob dispatchingImageJob = imageJobPersistenceOutport.loadOne(new ImageJobId(imageJobId))
                .orElseThrow(() -> new BusinessException(ImageJobErrorInfo.NOT_FOUND));

        // Processing으로 변경
        dispatchingImageJob.markProcessing(workerJobId, now.plus(pollDelay), now);
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

    @Transactional
    public void poll(Long imageJobId, String workerJobId) {
        ImageJob imageJob = imageJobPersistenceOutport.loadOne(new ImageJobId(imageJobId))
                .orElseThrow(() -> new BusinessException(ImageJobErrorInfo.NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();

        if (imageJob.getStatus().isTerminal()) {
            return;
        }

        WorkerProcessingInfo processingInfo;
        try {
            processingInfo = workerOutport.getProcessingInfo(workerJobId);
        } catch (BusinessException e) {
            if(e.isRetryable()) {
                throw e;
            }



            imageJob.markFailed(extractFailureCode(e), e.getMessage(), now);
            imageJobPersistenceOutport.update(imageJob);
            return;
        }

        switch (processingInfo.status()) {
            case "PROCESSING" -> {
                boolean claimed = imageJobPersistenceOutport.reschedulePoll(imageJob.getId(), now.plus(pollDelay), now);
                if (!claimed){
                    return;
                }
                // 다음 Poll 예약
                outboxPersistenceOutport.insert(
                        OutboxEvent.createNew(
                                idGenerator.nextId(),
                                DomainType.IMAGE_JOB,
                                imageJobId,
                                OutboxEventType.POLL,
                                dataSerializerOutPort.serialize(
                                        new WorkerPollEventPayload(
                                                imageJobId,
                                                processingInfo.jobId())
                                ),
                                now
                        )
                );
            }
            case "COMPLETED" -> {
                imageJob.markSucceeded(processingInfo.result(), now);
                imageJobPersistenceOutport.update(imageJob);
            }
            case "FAILED" -> {
                // Mock Worker에서 실패결과 왔을 때, 실패 상세 정보 포함될 시 고도화 가능
                imageJob.markFailed(400, "이미지 처리 위임 결과 -> 실패", now);
                imageJobPersistenceOutport.update(imageJob);
            }
            default -> throw new BusinessException(SystemErrorInfo.WORKER_INVALID_STATUS);
        }
    }

    private String startWorkerOrHandleFailure(Long imageJobId, ImageJob imageJob, LocalDateTime now) {
        try {
            String apiKey = workerOutport.getApiKey();
            return workerOutport.processStart(apiKey, imageJob.getSourceImageUrl());
        } catch (BusinessException e) {
            if (e.isRetryable()) {
                throw e;
            }

            ImageJob dispatchingImageJob = imageJobPersistenceOutport.loadOne(new ImageJobId(imageJobId))
                    .orElseThrow(() -> new BusinessException(ImageJobErrorInfo.NOT_FOUND));

            dispatchingImageJob.markFailed(extractFailureCode(e), e.getMessage(), now);
            imageJobPersistenceOutport.update(dispatchingImageJob);
            return null;
        }
    }

    private void handleDispatchFailure(Long imageJobId, BusinessException e, LocalDateTime now) {
        if (e.isRetryable()) {
            throw e;
        }

        ImageJob dispatchingImageJob = imageJobPersistenceOutport.loadOne(new ImageJobId(imageJobId))
                .orElseThrow(() -> new BusinessException(ImageJobErrorInfo.NOT_FOUND));

        dispatchingImageJob.markFailed(extractFailureCode(e), e.getMessage(), now);
        imageJobPersistenceOutport.update(dispatchingImageJob);
    }

    private int extractFailureCode(BusinessException e) {
        Object status = e.getDetails().get("status");
        if (status instanceof Integer value) {
            return value;
        }
        return 500;
    }
}
