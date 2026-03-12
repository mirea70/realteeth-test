package com.realteeth.service.imagejob;

import com.realteeth.common.DomainType;
import com.realteeth.dto.response.ImageJobResponse;
import com.realteeth.error.exception.BusinessException;
import com.realteeth.error.info.ImageJobErrorInfo;
import com.realteeth.imagejob.model.ImageJob;
import com.realteeth.imagejob.model.ImageJobId;
import com.realteeth.outbox.OutboxEvent;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ImageJobServiceTest {

    @Mock
    private ImageJobPersistenceOutport imageJobPersistenceOutport;

    @Mock
    private OutboxPersistenceOutport outboxPersistenceOutport;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private DataSerializerOutPort dataSerializerOutPort;

    @InjectMocks
    private ImageJobService imageJobService;

    @Test
    @DisplayName("readOne: requestImageJobId에 해당하는 ImageJob이 존재하면 ImageJobResponse를 반환한다")
    void readOne_success() {
        // given
        Long requestImageJobId = 1L;
        LocalDateTime now = LocalDateTime.of(2026, 3, 9, 12, 0, 0);
        String sourceImageUrl = "https://example.com/source.png";

        ImageJob imageJob = ImageJob.createNew(
                requestImageJobId,
                sourceImageUrl,
                now
        );

        given(imageJobPersistenceOutport.loadOne(any(ImageJobId.class)))
                .willReturn(Optional.of(imageJob));

        // when
        ImageJobResponse response = imageJobService.readOne(requestImageJobId);

        // then
        assertThat(response).isNotNull();

        assertThat(response.imageJobId()).isEqualTo(requestImageJobId);
        assertThat(response.sourceImageUrl()).isEqualTo(sourceImageUrl);
        assertThat(response.createdAt()).isEqualTo(now);

        then(imageJobPersistenceOutport)
                .should(times(1))
                .loadOne(any(ImageJobId.class));
    }

    @Test
    @DisplayName("readOne: requestImageJobId에 해당하는 ImageJob이 없으면 BusinessException을 던진다")
    void readOne_notFound() {
        // given
        Long requestImageJobId = 999L;

        given(imageJobPersistenceOutport.loadOne(any(ImageJobId.class)))
                .willReturn(Optional.empty());

        // when
        Throwable thrown = catchThrowable(() -> imageJobService.readOne(requestImageJobId));

        // then
        assertThat(thrown)
                .isInstanceOf(BusinessException.class);

        BusinessException exception = (BusinessException) thrown;
        assertThat(exception.getErrorInfo()).isEqualTo(ImageJobErrorInfo.NOT_FOUND);

        then(imageJobPersistenceOutport)
                .should(times(1))
                .loadOne(any(ImageJobId.class));
    }

    @DisplayName("이미지 작업 등록 시 ImageJob을 저장하고 OutboxEvent도 함께 저장한다")
    @Test
    void register_success() {
        // given
        Long imageJobId = 100L;
        String sourceImageUrl = "https://example.com/source.png";
        Long outboxEventId = 200L;
        String serializedPayload = "{\"imageJobId\":100}";

        given(idGenerator.nextId())
                .willReturn(imageJobId, outboxEventId);

        given(dataSerializerOutPort.serialize(any(WorkerDispatchEventPayload.class)))
                .willReturn(serializedPayload);

        ArgumentCaptor<ImageJob> imageJobCaptor = ArgumentCaptor.forClass(ImageJob.class);
        ArgumentCaptor<OutboxEvent> outboxEventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);

        given(imageJobPersistenceOutport.insert(org.mockito.ArgumentMatchers.any(ImageJob.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        ImageJobResponse response = imageJobService.register(sourceImageUrl);

        // then
        verify(imageJobPersistenceOutport).insert(imageJobCaptor.capture());
        verify(outboxPersistenceOutport).insert(outboxEventCaptor.capture());

        ImageJob savedImageJob = imageJobCaptor.getValue();
        OutboxEvent savedOutboxEvent = outboxEventCaptor.getValue();

        assertThat(savedImageJob).isNotNull();
        assertThat(savedOutboxEvent).isNotNull();

        // ImageJob 검증
        // 아래 getter 이름은 네 실제 도메인 모델에 맞게 바꿔줘
        assertThat(savedImageJob.getId().getValue()).isEqualTo(imageJobId);
        assertThat(savedImageJob.getSourceImageUrl()).isEqualTo(sourceImageUrl);

        // OutboxEvent 검증
        assertThat(savedOutboxEvent.getId()).isEqualTo(outboxEventId);
        assertThat(savedOutboxEvent.getDomainType()).isEqualTo(DomainType.IMAGE_JOB);
        assertThat(savedOutboxEvent.getDomainId()).isEqualTo(imageJobId);
        assertThat(savedOutboxEvent.getPayload()).isEqualTo(serializedPayload);

        // 응답 검증
        assertThat(response).isNotNull();
        assertThat(response.imageJobId()).isEqualTo(imageJobId);
    }
}