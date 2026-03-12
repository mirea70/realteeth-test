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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ImageJobRecoveryServiceTest {

    @Mock
    private ImageJobPersistenceOutport imageJobPersistenceOutport;

    @Mock
    private OutboxPersistenceOutport outboxPersistenceOutport;

    @Mock
    private ImageJobDelegateService imageJobDelegateService;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private DataSerializerOutPort dataSerializerOutPort;

    @InjectMocks
    private ImageJobRecoveryService imageJobRecoveryService;

    @Test
    @DisplayName("recoverDispatchingJobs - 5회 이하 재시도 시 PUBLISH_PENDING으로 갱신하고 Outbox 이벤트를 재발행한다")
    void recoverDispatchingJobs_success_whenMaxAttemptNotExceeded() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Long imageJobId = 100L;
        Long outboxEventId = 200L;

        ImageJob stuckJob = getImageJob(imageJobId, ImageJobStatus.DISPATCHING, 3, 0, now);

        given(imageJobPersistenceOutport.findStuckJobs(eq(ImageJobStatus.DISPATCHING), any(LocalDateTime.class)))
                .willReturn(List.of(stuckJob));

        given(idGenerator.nextId()).willReturn(outboxEventId);
        given(dataSerializerOutPort.serialize(any(WorkerDispatchEventPayload.class)))
                .willReturn("{\"imageJobId\":100}");

        // when
        imageJobRecoveryService.recoverDispatchingJobs();

        // then
        then(imageJobPersistenceOutport).should().update(stuckJob);
        assertThat(stuckJob.getStatus()).isEqualTo(ImageJobStatus.PUBLISH_PENDING);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        then(outboxPersistenceOutport).should().insert(captor.capture());

        OutboxEvent outboxEvent = captor.getValue();
        assertThat(outboxEvent.getId()).isEqualTo(outboxEventId);
        assertThat(outboxEvent.getDomainId()).isEqualTo(imageJobId);
        assertThat(outboxEvent.getDomainType()).isEqualTo(DomainType.IMAGE_JOB);
        assertThat(outboxEvent.getType()).isEqualTo(OutboxEventType.DISPATCH);
    }

    @Test
    @DisplayName("recoverDispatchingJobs - 5회 초과 재시도 시 FAILED 상태로 갱신하고 재발행하지 않는다")
    void recoverDispatchingJobs_fail_whenMaxAttemptExceeded() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Long imageJobId = 100L;

        ImageJob stuckJob = getImageJob(imageJobId, ImageJobStatus.DISPATCHING, 6, 0, now);

        given(imageJobPersistenceOutport.findStuckJobs(eq(ImageJobStatus.DISPATCHING), any(LocalDateTime.class)))
                .willReturn(List.of(stuckJob));

        // when
        imageJobRecoveryService.recoverDispatchingJobs();

        // then
        then(imageJobPersistenceOutport).should().update(stuckJob);
        assertThat(stuckJob.getStatus()).isEqualTo(ImageJobStatus.FAILED);
        assertThat(stuckJob.getFailure()).isNotNull();
        assertThat(stuckJob.getFailure().getCode()).isEqualTo(500);
        assertThat(stuckJob.getFailure().getMessage()).isEqualTo("최대 작업 위임 시도 횟수 초과 (복구 과정 중 감지)");

        then(outboxPersistenceOutport).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("recoverProcessingJobs - 조회된 각 작업에 대해 ImageJobDelegateService.poll을 호출한다")
    void recoverProcessingJobs_success() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Long imageJobId1 = 1L;
        Long imageJobId2 = 2L;

        ImageJob stuckJob1 = getImageJob(imageJobId1, ImageJobStatus.PROCESSING, 1, 3, now);
        ImageJob stuckJob2 = getImageJob(imageJobId2, ImageJobStatus.PROCESSING, 1, 4, now);

        given(imageJobPersistenceOutport.findStuckJobs(eq(ImageJobStatus.PROCESSING), any(LocalDateTime.class)))
                .willReturn(List.of(stuckJob1, stuckJob2));

        // when
        imageJobRecoveryService.recoverProcessingJobs();

        // then
        then(imageJobDelegateService).should().poll(imageJobId1, "worker-job-" + imageJobId1);
        then(imageJobDelegateService).should().poll(imageJobId2, "worker-job-" + imageJobId2);
    }

    private ImageJob getImageJob(Long id, ImageJobStatus status, int dispatchAttemptCount, int pollAttemptCount, LocalDateTime now) {
        String workerId = status == ImageJobStatus.PROCESSING ? "worker-job-" + id : null;
        return ImageJob.fromOutside(
                id,
                "https://example.com/image.jpg",
                status.name(),
                workerId,
                null,
                null,
                null,
                null,
                null,
                dispatchAttemptCount,
                pollAttemptCount,
                now,
                now
        );
    }
}
