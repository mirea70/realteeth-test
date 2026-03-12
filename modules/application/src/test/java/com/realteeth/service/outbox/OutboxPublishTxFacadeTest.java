package com.realteeth.service.outbox;

import com.realteeth.common.DomainType;
import com.realteeth.error.exception.BusinessException;
import com.realteeth.error.info.CommonDomainErrorInfo;
import com.realteeth.outbox.OutboxEvent;
import com.realteeth.outbox.OutboxEventType;
import com.realteeth.outbox.OutboxStatus;
import com.realteeth.port.out.ImageJobPersistenceOutport;
import com.realteeth.port.out.OutboxPersistenceOutport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class OutboxPublishTxFacadeTest {

    @Mock
    private OutboxPersistenceOutport outboxPersistenceOutport;

    @Mock
    private ImageJobPersistenceOutport imageJobPersistenceOutport;

    @InjectMocks
    private OutboxPublishTxFacade outboxPublishTxFacade;

    @Test
    @DisplayName("아웃박스와 이미지 작업 상태 변경에 성공하면 true를 반환한다")
    void completePublished_success() {
        // given
        OutboxEvent event = OutboxEvent.fromOutside(
                1L,
                DomainType.IMAGE_JOB,
                10L,
                OutboxEventType.DISPATCH,
                "{}",
                OutboxStatus.PENDING,
                null,
                null
        );

        given(outboxPersistenceOutport.markPublishedDirectly(1L)).willReturn(true);
        given(imageJobPersistenceOutport.markPublishedDirectly(eq(new com.realteeth.imagejob.model.ImageJobId(10L)), any(LocalDateTime.class)))
                .willReturn(true);

        // when
        boolean result = outboxPublishTxFacade.completePublished(event);

        // then
        assertThat(result).isTrue();

        then(outboxPersistenceOutport).should().markPublishedDirectly(1L);

        ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        then(imageJobPersistenceOutport).should()
                .markPublishedDirectly(eq(new com.realteeth.imagejob.model.ImageJobId(10L)), timeCaptor.capture());

        assertThat(timeCaptor.getValue()).isNotNull();
    }

    @Test
    @DisplayName("아웃박스 상태 변경에 실패하면 false를 반환하고 이미지 작업 상태는 변경하지 않는다")
    void completePublished_fail_whenOutboxMarkFailed() {
        // given
        OutboxEvent event = OutboxEvent.fromOutside(
                1L,
                DomainType.IMAGE_JOB,
                10L,
                OutboxEventType.DISPATCH,
                "{}",
                OutboxStatus.PENDING,
                null,
                null
        );

        given(outboxPersistenceOutport.markPublishedDirectly(1L)).willReturn(false);

        // when
        boolean result = outboxPublishTxFacade.completePublished(event);

        // then
        assertThat(result).isFalse();

        then(outboxPersistenceOutport).should().markPublishedDirectly(1L);
        then(imageJobPersistenceOutport).should(never())
                .markPublishedDirectly(any(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("아웃박스는 성공했지만 이미지 작업 상태 변경에 실패하면 예외가 발생한다")
    void completePublished_fail_whenImageJobMarkFailed() {
        // given
        OutboxEvent event = OutboxEvent.fromOutside(
                1L,
                DomainType.IMAGE_JOB,
                30L,
                OutboxEventType.DISPATCH,
                "{}",
                OutboxStatus.PENDING,
                null,
                null
        );

        given(outboxPersistenceOutport.markPublishedDirectly(1L)).willReturn(true);
        given(imageJobPersistenceOutport.markPublishedDirectly(eq(new com.realteeth.imagejob.model.ImageJobId(30L)), any(LocalDateTime.class)))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() -> outboxPublishTxFacade.completePublished(event))
                .isInstanceOf(BusinessException.class)
                .extracting("errorInfo")
                .isEqualTo(CommonDomainErrorInfo.OUTBOX_PUBLISH_TRANSACTION_FAIL);

        then(outboxPersistenceOutport).should().markPublishedDirectly(1L);
        then(imageJobPersistenceOutport).should()
                .markPublishedDirectly(eq(new com.realteeth.imagejob.model.ImageJobId(30L)), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("rollbackToPending 호출 시 아웃박스 이벤트를 PENDING으로 되돌린다")
    void rollbackToPending_success() {
        // given
        Long eventId = 100L;

        // when
        outboxPublishTxFacade.rollbackToPending(eventId);

        // then
        then(outboxPersistenceOutport).should().markPendingAgainDirectly(100L);
    }
}