package com.realteeth.service.imagejob;

import com.realteeth.common.DomainType;
import com.realteeth.dto.worker.WorkerProcessingInfo;
import com.realteeth.error.exception.BusinessException;
import com.realteeth.error.info.SystemErrorInfo;
import com.realteeth.imagejob.model.ImageJob;
import com.realteeth.imagejob.model.ImageJobId;
import com.realteeth.imagejob.model.ImageJobStatus;
import com.realteeth.outbox.OutboxEvent;
import com.realteeth.outbox.OutboxEventType;
import com.realteeth.port.out.DataSerializerOutPort;
import com.realteeth.port.out.IdGenerator;
import com.realteeth.port.out.ImageJobPersistenceOutport;
import com.realteeth.port.out.OutboxPersistenceOutport;
import com.realteeth.port.out.WorkerOutport;
import com.realteeth.worker.WorkerPollEventPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ImageJobDelegateServicePollTest {

    @Mock
    private ImageJobPersistenceOutport imageJobPersistenceOutport;

    @Mock
    private OutboxPersistenceOutport outboxPersistenceOutport;

    @Mock
    private WorkerOutport workerOutport;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private DataSerializerOutPort dataSerializerOutPort;

    @InjectMocks
    private ImageJobDelegateService imageJobDelegateService;

    @Test
    @DisplayName("존재하지 않는 ImageJob이면 예외가 발생한다")
    void poll_fail_whenImageJobNotFound() {
        Long imageJobId = 1L;

        given(imageJobPersistenceOutport.loadOne(new ImageJobId(imageJobId)))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> imageJobDelegateService.poll(imageJobId, "worker-job-123"))
                .isInstanceOf(BusinessException.class);

        then(workerOutport).shouldHaveNoInteractions();
        then(outboxPersistenceOutport).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("terminal 상태이면 아무것도 하지 않고 종료한다")
    void poll_return_whenTerminal() {
        Long imageJobId = 1L;
        LocalDateTime now = LocalDateTime.of(2026, 3, 11, 10, 0);

        ImageJob succeededImageJob = getImageJob(imageJobId, "https://example.com/image.jpg", ImageJobStatus.SUCCEEDED, now);

        given(imageJobPersistenceOutport.loadOne(new ImageJobId(imageJobId)))
                .willReturn(Optional.of(succeededImageJob));

        imageJobDelegateService.poll(imageJobId, "worker-job-123");

        then(workerOutport).shouldHaveNoInteractions();
        then(imageJobPersistenceOutport).should(never()).update(any());
        then(outboxPersistenceOutport).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("getProcessingInfo에서 retryable 예외가 발생하면 예외를 전파한다")
    void poll_fail_whenGetProcessingInfoRetryable() {
        Long imageJobId = 1L;
        LocalDateTime now = LocalDateTime.of(2026, 3, 11, 10, 0);

        ImageJob processingImageJob = getImageJob(imageJobId, "https://example.com/image.jpg", ImageJobStatus.PROCESSING, now);

        given(imageJobPersistenceOutport.loadOne(new ImageJobId(imageJobId)))
                .willReturn(Optional.of(processingImageJob));

        BusinessException retryableException = new BusinessException(
                SystemErrorInfo.WORKER_GET_PROCESS_INFO_FAIL,
                Map.of("status", 503),
                true
        );
        given(workerOutport.getProcessingInfo("worker-job-123")).willThrow(retryableException);

        assertThatThrownBy(() -> imageJobDelegateService.poll(imageJobId, "worker-job-123"))
                .isSameAs(retryableException);

        then(imageJobPersistenceOutport).should(never()).update(any());
        then(outboxPersistenceOutport).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("getProcessingInfo에서 non-retryable 예외가 발생하면 FAILED로 저장하고 종료한다")
    void poll_return_whenGetProcessingInfoNonRetryable() {
        Long imageJobId = 1L;
        LocalDateTime now = LocalDateTime.of(2026, 3, 11, 10, 0);

        ImageJob processingImageJob = getImageJob(imageJobId, "https://example.com/image.jpg", ImageJobStatus.PROCESSING, now);

        given(imageJobPersistenceOutport.loadOne(new ImageJobId(imageJobId)))
                .willReturn(Optional.of(processingImageJob));

        BusinessException nonRetryableException = new BusinessException(
                SystemErrorInfo.WORKER_GET_PROCESS_INFO_FAIL,
                Map.of("status", 404),
                false
        );
        given(workerOutport.getProcessingInfo("worker-job-123")).willThrow(nonRetryableException);

        imageJobDelegateService.poll(imageJobId, "worker-job-123");

        then(imageJobPersistenceOutport).should().update(processingImageJob);
        then(outboxPersistenceOutport).shouldHaveNoInteractions();

        assertThat(processingImageJob.getStatus()).isEqualTo(ImageJobStatus.FAILED);
        assertThat(processingImageJob.getFailure()).isNotNull();
        assertThat(processingImageJob.getFailure().getCode()).isEqualTo(404);
    }

    @Test
    @DisplayName("Worker 상태가 PROCESSING이고 선점 성공하면 다음 POLL 이벤트를 저장한다")
    void poll_success_whenProcessingAndClaimed() {
        Long imageJobId = 1L;
        Long outboxEventId = 999L;
        LocalDateTime now = LocalDateTime.of(2026, 3, 11, 10, 0);

        ImageJob processingImageJob = getImageJob(imageJobId, "https://example.com/image.jpg", ImageJobStatus.PROCESSING, now);

        given(imageJobPersistenceOutport.loadOne(new ImageJobId(imageJobId)))
                .willReturn(Optional.of(processingImageJob));
        given(workerOutport.getProcessingInfo("worker-job-123"))
                .willReturn(new WorkerProcessingInfo("worker-job-123", "PROCESSING", null, null));
        given(imageJobPersistenceOutport.reschedulePoll(eq(new ImageJobId(imageJobId)), any(LocalDateTime.class)))
                .willReturn(true);
        given(idGenerator.nextId()).willReturn(outboxEventId);
        given(dataSerializerOutPort.serialize(any(WorkerPollEventPayload.class)))
                .willReturn("{\"imageJobId\":1,\"workerJobId\":\"worker-job-123\"}");

        imageJobDelegateService.poll(imageJobId, "worker-job-123");

        then(imageJobPersistenceOutport).should().reschedulePoll(eq(new ImageJobId(imageJobId)), any(LocalDateTime.class));

        ArgumentCaptor<OutboxEvent> outboxEventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        then(outboxPersistenceOutport).should().insert(outboxEventCaptor.capture());

        OutboxEvent outboxEvent = outboxEventCaptor.getValue();
        assertThat(outboxEvent.getId()).isEqualTo(outboxEventId);
        assertThat(outboxEvent.getDomainType()).isEqualTo(DomainType.IMAGE_JOB);
        assertThat(outboxEvent.getDomainId()).isEqualTo(imageJobId);
        assertThat(outboxEvent.getType()).isEqualTo(OutboxEventType.POLL);
    }

    @Test
    @DisplayName("Worker 상태가 PROCESSING이나 pollAttemptCount가 5 이상이면 FAILED로 갱신하고 종료한다")
    void poll_fail_whenProcessingAndMaxAttemptExceeded() {
        Long imageJobId = 1L;
        LocalDateTime now = LocalDateTime.of(2026, 3, 11, 10, 0);

        ImageJob processingImageJob = ImageJob.fromOutside(
                imageJobId,
                "https://example.com/image.jpg",
                ImageJobStatus.PROCESSING.name(),
                "worker-job-123",
                null,
                null,
                null,
                null,
                null,
                0,
                5,
                now,
                now
        );

        given(imageJobPersistenceOutport.loadOne(new ImageJobId(imageJobId)))
                .willReturn(Optional.of(processingImageJob));
        given(workerOutport.getProcessingInfo("worker-job-123"))
                .willReturn(new WorkerProcessingInfo("worker-job-123", "PROCESSING", null, null));

        imageJobDelegateService.poll(imageJobId, "worker-job-123");

        then(imageJobPersistenceOutport).should().update(processingImageJob);
        then(outboxPersistenceOutport).shouldHaveNoInteractions();

        assertThat(processingImageJob.getStatus()).isEqualTo(ImageJobStatus.FAILED);
        assertThat(processingImageJob.getFailure()).isNotNull();
        assertThat(processingImageJob.getFailure().getMessage()).isEqualTo("최대 상태 확인 시도 횟수 초과");
    }

    @Test
    @DisplayName("Worker 상태가 PROCESSING이어도 선점 실패하면 Poll 이벤트를 저장하지 않는다")
    void poll_return_whenProcessingAndClaimFailed() {
        Long imageJobId = 1L;
        LocalDateTime now = LocalDateTime.of(2026, 3, 11, 10, 0);

        ImageJob processingImageJob = getImageJob(imageJobId, "https://example.com/image.jpg", ImageJobStatus.PROCESSING, now);

        given(imageJobPersistenceOutport.loadOne(new ImageJobId(imageJobId)))
                .willReturn(Optional.of(processingImageJob));
        given(workerOutport.getProcessingInfo("worker-job-123"))
                .willReturn(new WorkerProcessingInfo("worker-job-123", "PROCESSING", null, null));
        given(imageJobPersistenceOutport.reschedulePoll(eq(new ImageJobId(imageJobId)), any(LocalDateTime.class)))
                .willReturn(false);

        imageJobDelegateService.poll(imageJobId, "worker-job-123");

        then(outboxPersistenceOutport).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Worker 상태가 COMPLETED이면 SUCCEEDED로 저장한다")
    void poll_success_whenCompleted() {
        Long imageJobId = 1L;
        LocalDateTime now = LocalDateTime.of(2026, 3, 11, 10, 0);

        ImageJob processingImageJob = getImageJob(imageJobId, "https://example.com/image.jpg", ImageJobStatus.PROCESSING, now);

        given(imageJobPersistenceOutport.loadOne(new ImageJobId(imageJobId)))
                .willReturn(Optional.of(processingImageJob));
        given(workerOutport.getProcessingInfo("worker-job-123"))
                .willReturn(new WorkerProcessingInfo("worker-job-123", "COMPLETED", "https://example.com/result.jpg", null));

        imageJobDelegateService.poll(imageJobId, "worker-job-123");

        then(imageJobPersistenceOutport).should().update(processingImageJob);
        assertThat(processingImageJob.getStatus()).isEqualTo(ImageJobStatus.SUCCEEDED);
        assertThat(processingImageJob.getResult()).isNotNull();
        assertThat(processingImageJob.getResult().getValue()).isEqualTo("https://example.com/result.jpg");
    }

    @Test
    @DisplayName("Worker 상태가 FAILED이면 FAILED로 저장한다")
    void poll_success_whenFailed() {
        Long imageJobId = 1L;
        LocalDateTime now = LocalDateTime.of(2026, 3, 11, 10, 0);

        ImageJob processingImageJob = getImageJob(imageJobId, "https://example.com/image.jpg", ImageJobStatus.PROCESSING, now);

        given(imageJobPersistenceOutport.loadOne(new ImageJobId(imageJobId)))
                .willReturn(Optional.of(processingImageJob));
        given(workerOutport.getProcessingInfo("worker-job-123"))
                .willReturn(new WorkerProcessingInfo("worker-job-123", "FAILED", null, null));

        imageJobDelegateService.poll(imageJobId, "worker-job-123");

        then(imageJobPersistenceOutport).should().update(processingImageJob);
        assertThat(processingImageJob.getStatus()).isEqualTo(ImageJobStatus.FAILED);
        assertThat(processingImageJob.getFailure()).isNotNull();
        assertThat(processingImageJob.getFailure().getCode()).isEqualTo(400);
    }

    @Test
    @DisplayName("Worker 상태가 유효하지 않으면 예외가 발생한다")
    void poll_fail_whenInvalidWorkerStatus() {
        Long imageJobId = 1L;
        LocalDateTime now = LocalDateTime.of(2026, 3, 11, 10, 0);

        ImageJob processingImageJob = getImageJob(imageJobId, "https://example.com/image.jpg", ImageJobStatus.PROCESSING, now);

        given(imageJobPersistenceOutport.loadOne(new ImageJobId(imageJobId)))
                .willReturn(Optional.of(processingImageJob));
        given(workerOutport.getProcessingInfo("worker-job-123"))
                .willReturn(new WorkerProcessingInfo("worker-job-123", "UNKNOWN", null, null));

        assertThatThrownBy(() -> imageJobDelegateService.poll(imageJobId, "worker-job-123"))
                .isInstanceOf(BusinessException.class);

        then(imageJobPersistenceOutport).should(never()).update(any());
        then(outboxPersistenceOutport).shouldHaveNoInteractions();
    }

    private ImageJob getImageJob(Long id, String sourceImageUrl, ImageJobStatus status, LocalDateTime now) {
        return ImageJob.fromOutside(
                id,
                sourceImageUrl,
                status.name(),
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                0,
                now,
                now
        );
    }
}