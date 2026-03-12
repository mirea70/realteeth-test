package com.realteeth.service.imagejob;

import com.realteeth.common.DomainType;
import com.realteeth.error.exception.BusinessException;
import com.realteeth.error.info.SystemErrorInfo;
import com.realteeth.imagejob.model.ImageJob;
import com.realteeth.imagejob.model.ImageJobId;
import com.realteeth.imagejob.model.ImageJobStatus;
import com.realteeth.outbox.OutboxEvent;
import com.realteeth.outbox.OutboxEventType;
import com.realteeth.port.out.*;
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
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ImageJobDelegateServiceDispatchTest {

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
    void dispatch_fail_whenImageJobNotFound() {
        Long imageJobId = 1L;

        given(imageJobPersistenceOutport.loadOne(new ImageJobId(imageJobId)))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> imageJobDelegateService.dispatch(imageJobId))
                .isInstanceOf(BusinessException.class);

        then(imageJobPersistenceOutport).should().loadOne(new ImageJobId(imageJobId));
        then(imageJobPersistenceOutport).should(never()).markDispatchingDirectly(any(), any());
        then(workerOutport).shouldHaveNoInteractions();
        then(outboxPersistenceOutport).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("dispatch 불가능한 상태이면 아무것도 하지 않고 종료한다")
    void dispatch_return_whenImageJobIsNotDispatchable() {
        Long imageJobId = 1L;
        LocalDateTime now = LocalDateTime.of(2026, 3, 11, 10, 0);

        ImageJob imageJob = ImageJob.createNew(imageJobId, "https://example.com/image.jpg", now);

        given(imageJobPersistenceOutport.loadOne(new ImageJobId(imageJobId)))
                .willReturn(Optional.of(imageJob));

        imageJobDelegateService.dispatch(imageJobId);

        then(imageJobPersistenceOutport).should().loadOne(new ImageJobId(imageJobId));
        then(imageJobPersistenceOutport).should(never()).markDispatchingDirectly(any(), any());
        then(workerOutport).shouldHaveNoInteractions();
        then(outboxPersistenceOutport).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("상태 선점에 실패하면 외부 Worker 호출과 Poll 이벤트 발행 없이 종료한다")
    void dispatch_return_whenClaimFailed() {
        Long imageJobId = 1L;
        LocalDateTime now = LocalDateTime.of(2026, 3, 11, 10, 0);

        ImageJob imageJob = getImageJob(imageJobId, "https://example.com/image.jpg", ImageJobStatus.PUBLISHED, now);

        given(imageJobPersistenceOutport.loadOne(new ImageJobId(imageJobId)))
                .willReturn(Optional.of(imageJob));
        given(imageJobPersistenceOutport.markDispatchingDirectly(eq(new ImageJobId(imageJobId)), any(LocalDateTime.class)))
                .willReturn(false);

        imageJobDelegateService.dispatch(imageJobId);

        then(imageJobPersistenceOutport).should().loadOne(new ImageJobId(imageJobId));
        then(imageJobPersistenceOutport).should().markDispatchingDirectly(eq(new ImageJobId(imageJobId)), any(LocalDateTime.class));
        then(workerOutport).shouldHaveNoInteractions();
        then(outboxPersistenceOutport).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("getApiKey에서 retryable 예외가 발생하면 예외를 전파하고 update, outbox 저장을 하지 않는다")
    void dispatch_fail_whenGetApiKeyRetryable() {
        Long imageJobId = 1L;
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 11, 10, 0);

        ImageJob publishedImageJob = getImageJob(imageJobId, "https://example.com/image.jpg", ImageJobStatus.PUBLISHED, createdAt);

        given(imageJobPersistenceOutport.loadOne(new ImageJobId(imageJobId)))
                .willReturn(Optional.of(publishedImageJob));
        given(imageJobPersistenceOutport.markDispatchingDirectly(eq(new ImageJobId(imageJobId)), any(LocalDateTime.class)))
                .willReturn(true);

        BusinessException retryableException = new BusinessException(
                SystemErrorInfo.WORKER_ISSUE_KEY_FAIL,
                Map.of("status", 503),
                true
        );
        given(workerOutport.getApiKey()).willThrow(retryableException);

        assertThatThrownBy(() -> imageJobDelegateService.dispatch(imageJobId))
                .isSameAs(retryableException);

        then(imageJobPersistenceOutport).should(never()).update(any());
        then(outboxPersistenceOutport).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("processStart에서 retryable 예외가 발생하면 예외를 전파하고 update, outbox 저장을 하지 않는다")
    void dispatch_fail_whenProcessStartRetryable() {
        Long imageJobId = 1L;
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 11, 10, 0);

        ImageJob publishedImageJob = getImageJob(imageJobId, "https://example.com/image.jpg", ImageJobStatus.PUBLISHED, createdAt);

        given(imageJobPersistenceOutport.loadOne(new ImageJobId(imageJobId)))
                .willReturn(Optional.of(publishedImageJob));
        given(imageJobPersistenceOutport.markDispatchingDirectly(eq(new ImageJobId(imageJobId)), any(LocalDateTime.class)))
                .willReturn(true);
        given(workerOutport.getApiKey()).willReturn("mock_api_key");

        BusinessException retryableException = new BusinessException(
                SystemErrorInfo.WORKER_PROCESS_START_FAIL,
                Map.of("status", 503),
                true
        );
        given(workerOutport.processStart(anyString(), anyString())).willThrow(retryableException);

        assertThatThrownBy(() -> imageJobDelegateService.dispatch(imageJobId))
                .isSameAs(retryableException);

        then(imageJobPersistenceOutport).should(never()).update(any());
        then(outboxPersistenceOutport).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("processStart에서 non-retryable 예외가 발생하면 FAILED로 저장하고 종료한다")
    void dispatch_return_whenProcessStartNonRetryable() {
        Long imageJobId = 1L;
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 11, 10, 0);

        ImageJob publishedImageJob = getImageJob(imageJobId, "https://example.com/image.jpg", ImageJobStatus.PUBLISHED, createdAt);
        ImageJob dispatchingImageJob = getImageJob(imageJobId, "https://example.com/image.jpg", ImageJobStatus.DISPATCHING, createdAt);

        given(imageJobPersistenceOutport.loadOne(new ImageJobId(imageJobId)))
                .willReturn(Optional.of(publishedImageJob), Optional.of(dispatchingImageJob));
        given(imageJobPersistenceOutport.markDispatchingDirectly(eq(new ImageJobId(imageJobId)), any(LocalDateTime.class)))
                .willReturn(true);
        given(workerOutport.getApiKey()).willReturn("mock_api_key");

        BusinessException nonRetryableException = new BusinessException(
                SystemErrorInfo.WORKER_PROCESS_START_FAIL,
                Map.of("status", 400),
                false
        );
        given(workerOutport.processStart(anyString(), anyString())).willThrow(nonRetryableException);

        imageJobDelegateService.dispatch(imageJobId);

        then(imageJobPersistenceOutport).should(times(2)).loadOne(new ImageJobId(imageJobId));
        then(imageJobPersistenceOutport).should().update(dispatchingImageJob);
        then(outboxPersistenceOutport).shouldHaveNoInteractions();

        assertThat(dispatchingImageJob.getStatus()).isEqualTo(ImageJobStatus.FAILED);
        assertThat(dispatchingImageJob.getFailure()).isNotNull();
        assertThat(dispatchingImageJob.getFailure().getCode()).isEqualTo(400);
    }

    @Test
    @DisplayName("정상적으로 Worker에 위임하고 PROCESSING 상태 및 POLL 이벤트를 저장한다")
    void dispatch_success() {
        Long imageJobId = 1L;
        Long outboxEventId = 999L;
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 11, 10, 0);

        ImageJob publishedImageJob = getImageJob(imageJobId, "https://example.com/image.jpg", ImageJobStatus.PUBLISHED, createdAt);
        ImageJob dispatchingImageJob = getImageJob(imageJobId, "https://example.com/image.jpg", ImageJobStatus.DISPATCHING, createdAt);

        given(imageJobPersistenceOutport.loadOne(new ImageJobId(imageJobId)))
                .willReturn(Optional.of(publishedImageJob), Optional.of(dispatchingImageJob));
        given(imageJobPersistenceOutport.markDispatchingDirectly(eq(new ImageJobId(imageJobId)), any(LocalDateTime.class)))
                .willReturn(true);
        given(workerOutport.getApiKey()).willReturn("mock_api_key");
        given(workerOutport.processStart("mock_api_key", "https://example.com/image.jpg"))
                .willReturn("worker-job-123");
        given(idGenerator.nextId()).willReturn(outboxEventId);
        given(dataSerializerOutPort.serialize(any(WorkerPollEventPayload.class)))
                .willReturn("{\"imageJobId\":1,\"workerJobId\":\"worker-job-123\"}");

        imageJobDelegateService.dispatch(imageJobId);

        then(workerOutport).should().getApiKey();
        then(workerOutport).should().processStart("mock_api_key", "https://example.com/image.jpg");
        then(imageJobPersistenceOutport).should(times(2)).loadOne(new ImageJobId(imageJobId));
        then(imageJobPersistenceOutport).should().update(dispatchingImageJob);

        ArgumentCaptor<WorkerPollEventPayload> payloadCaptor = ArgumentCaptor.forClass(WorkerPollEventPayload.class);
        then(dataSerializerOutPort).should().serialize(payloadCaptor.capture());

        WorkerPollEventPayload capturedPayload = payloadCaptor.getValue();
        assertThat(capturedPayload.imageJobId()).isEqualTo(imageJobId);
        assertThat(capturedPayload.workerJobId()).isEqualTo("worker-job-123");

        ArgumentCaptor<OutboxEvent> outboxEventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        then(outboxPersistenceOutport).should().insert(outboxEventCaptor.capture());

        OutboxEvent savedOutboxEvent = outboxEventCaptor.getValue();
        assertThat(savedOutboxEvent.getId()).isEqualTo(outboxEventId);
        assertThat(savedOutboxEvent.getDomainType()).isEqualTo(DomainType.IMAGE_JOB);
        assertThat(savedOutboxEvent.getDomainId()).isEqualTo(imageJobId);
        assertThat(savedOutboxEvent.getType()).isEqualTo(OutboxEventType.POLL);
        assertThat(savedOutboxEvent.getPayload()).isEqualTo("{\"imageJobId\":1,\"workerJobId\":\"worker-job-123\"}");
        assertThat(savedOutboxEvent.getCreatedAt()).isNotNull();

        assertThat(dispatchingImageJob.getStatus()).isEqualTo(ImageJobStatus.PROCESSING);
        assertThat(dispatchingImageJob.getWorkerJobId()).isEqualTo("worker-job-123");
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