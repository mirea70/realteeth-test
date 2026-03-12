package com.realteeth.service.outbox;

import com.realteeth.common.DomainType;
import com.realteeth.outbox.OutboxEvent;
import com.realteeth.outbox.OutboxEventType;
import com.realteeth.outbox.OutboxStatus;
import com.realteeth.port.out.MessagePublisher;
import com.realteeth.port.out.OutboxPersistenceOutport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublishServiceTest {

    @Mock
    private OutboxPersistenceOutport outboxPersistenceOutport;

    @Mock
    private MessagePublisher messagePublisher;

    @Mock
    private OutboxPublishTxFacade outboxPublishTxFacade;

    @InjectMocks
    private OutboxPublishService outboxPublishService;

    @Test
    @DisplayName("pending 이벤트를 발행하고 completePublished 처리에 성공하면 성공 건수를 증가시킨다")
    void publishPending_success() {
        // given
        OutboxEvent event = createOutboxEvent(1L, OutboxStatus.PENDING);

        given(outboxPersistenceOutport.findOnPending(10))
                .willReturn(List.of(event));
        given(outboxPersistenceOutport.markPublishingDirectly(1L))
                .willReturn(true);
        given(outboxPublishTxFacade.completePublished(event))
                .willReturn(true);

        // when
        int result = outboxPublishService.publishPending(10);

        // then
        assertThat(result).isEqualTo(1);

        verify(outboxPersistenceOutport).findOnPending(10);
        verify(outboxPersistenceOutport).markPublishingDirectly(1L);
        verify(messagePublisher).publish(
                event.getDomainType(),
                event.getType(),
                event.getPayload()
        );
        verify(outboxPublishTxFacade).completePublished(event);
        verify(outboxPublishTxFacade, never()).rollbackToPending(any());
    }

    @Test
    @DisplayName("markPublishing 에 실패하면 메시지를 발행하지 않고 건너뛴다")
    void publishPending_skip_when_claim_failed() {
        // given
        OutboxEvent event = createOutboxEvent(1L, OutboxStatus.PENDING);

        given(outboxPersistenceOutport.findOnPending(10))
                .willReturn(List.of(event));
        given(outboxPersistenceOutport.markPublishingDirectly(1L))
                .willReturn(false);

        // when
        int result = outboxPublishService.publishPending(10);

        // then
        assertThat(result).isZero();

        verify(outboxPersistenceOutport).findOnPending(10);
        verify(outboxPersistenceOutport).markPublishingDirectly(1L);
        verify(messagePublisher, never()).publish(any(), any(), any());
        verify(outboxPublishTxFacade, never()).completePublished(any());
        verify(outboxPublishTxFacade, never()).rollbackToPending(any());
    }

    @Test
    @DisplayName("메시지 발행 중 예외가 발생하면 rollbackToPending 을 호출한다")
    void publishPending_restore_pending_when_publish_fails() {
        // given
        OutboxEvent event = createOutboxEvent(1L, OutboxStatus.PENDING);

        given(outboxPersistenceOutport.findOnPending(10))
                .willReturn(List.of(event));
        given(outboxPersistenceOutport.markPublishingDirectly(1L))
                .willReturn(true);

        doThrow(new RuntimeException("mq publish failed"))
                .when(messagePublisher)
                .publish(event.getDomainType(), event.getType(), event.getPayload());

        // when
        int result = outboxPublishService.publishPending(10);

        // then
        assertThat(result).isZero();

        verify(outboxPersistenceOutport).findOnPending(10);
        verify(outboxPersistenceOutport).markPublishingDirectly(1L);
        verify(messagePublisher).publish(
                event.getDomainType(),
                event.getType(),
                event.getPayload()
        );
        verify(outboxPublishTxFacade, never()).completePublished(any());
        verify(outboxPublishTxFacade).rollbackToPending(1L);
    }

    @Test
    @DisplayName("completePublished 에서 예외가 발생하면 rollbackToPending 을 호출한다")
    void publishPending_restore_pending_when_completePublished_fails() {
        // given
        OutboxEvent event = createOutboxEvent(1L, OutboxStatus.PENDING);

        given(outboxPersistenceOutport.findOnPending(10))
                .willReturn(List.of(event));
        given(outboxPersistenceOutport.markPublishingDirectly(1L))
                .willReturn(true);
        doNothing().when(messagePublisher)
                .publish(event.getDomainType(), event.getType(), event.getPayload());
        given(outboxPublishTxFacade.completePublished(event))
                .willThrow(new RuntimeException("tx facade failed"));

        // when
        int result = outboxPublishService.publishPending(10);

        // then
        assertThat(result).isZero();

        verify(messagePublisher).publish(
                event.getDomainType(),
                event.getType(),
                event.getPayload()
        );
        verify(outboxPublishTxFacade).completePublished(event);
        verify(outboxPublishTxFacade).rollbackToPending(1L);
    }

    @Test
    @DisplayName("rollbackToPending 에서 예외가 발생해도 다음 이벤트 처리는 계속된다")
    void publishPending_continue_when_rollback_fails() {
        // given
        OutboxEvent failedEvent = createOutboxEvent(1L, OutboxStatus.PENDING);
        OutboxEvent successEvent = createOutboxEvent(2L, OutboxStatus.PENDING);

        given(outboxPersistenceOutport.findOnPending(10))
                .willReturn(List.of(failedEvent, successEvent));

        given(outboxPersistenceOutport.markPublishingDirectly(1L)).willReturn(true);
        given(outboxPersistenceOutport.markPublishingDirectly(2L)).willReturn(true);

        doThrow(new RuntimeException("mq publish failed"))
                .when(messagePublisher)
                .publish(
                        failedEvent.getDomainType(),
                        failedEvent.getType(),
                        failedEvent.getPayload()
                );

        doNothing()
                .when(messagePublisher)
                .publish(
                        successEvent.getDomainType(),
                        successEvent.getType(),
                        successEvent.getPayload()
                );

        doThrow(new RuntimeException("rollback failed"))
                .when(outboxPublishTxFacade)
                .rollbackToPending(1L);

        given(outboxPublishTxFacade.completePublished(successEvent))
                .willReturn(true);

        // when
        int result = outboxPublishService.publishPending(10);

        // then
        assertThat(result).isEqualTo(1);

        verify(outboxPublishTxFacade).rollbackToPending(1L);
        verify(outboxPublishTxFacade).completePublished(successEvent);
    }

    @Test
    @DisplayName("여러 pending 이벤트 중 completePublished 성공한 건수만 반환한다")
    void publishPending_returns_only_success_count() {
        // given
        OutboxEvent event1 = createOutboxEvent(1L, OutboxStatus.PENDING);
        OutboxEvent event2 = createOutboxEvent(2L, OutboxStatus.PENDING);
        OutboxEvent event3 = createOutboxEvent(3L, OutboxStatus.PENDING);

        given(outboxPersistenceOutport.findOnPending(10))
                .willReturn(List.of(event1, event2, event3));

        given(outboxPersistenceOutport.markPublishingDirectly(1L)).willReturn(true);
        given(outboxPersistenceOutport.markPublishingDirectly(2L)).willReturn(false);
        given(outboxPersistenceOutport.markPublishingDirectly(3L)).willReturn(true);

        given(outboxPublishTxFacade.completePublished(event1)).willReturn(true);

        doAnswer(invocation -> {
            DomainType domainType = invocation.getArgument(0);
            OutboxEventType eventType = invocation.getArgument(1);
            String payload = invocation.getArgument(2);

            if (payload.equals(event3.getPayload())) {
                throw new RuntimeException("mq error");
            }
            return null;
        }).when(messagePublisher).publish(any(), any(), any());

        // when
        int result = outboxPublishService.publishPending(10);

        // then
        assertThat(result).isEqualTo(1);

        verify(messagePublisher).publish(
                event1.getDomainType(),
                event1.getType(),
                event1.getPayload()
        );
        verify(messagePublisher, never()).publish(
                event2.getDomainType(),
                event2.getType(),
                event2.getPayload()
        );
        verify(messagePublisher).publish(
                event3.getDomainType(),
                event3.getType(),
                event3.getPayload()
        );

        verify(outboxPublishTxFacade).completePublished(event1);
        verify(outboxPublishTxFacade, never()).completePublished(event2);
        verify(outboxPublishTxFacade, never()).completePublished(event3);

        verify(outboxPublishTxFacade).rollbackToPending(3L);
    }

    private OutboxEvent createOutboxEvent(Long id, OutboxStatus status) {
        return OutboxEvent.fromOutside(
                id,
                DomainType.IMAGE_JOB,
                100L + id,
                OutboxEventType.DISPATCH,
                "{\"jobId\":" + id + "}",
                status,
                LocalDateTime.now(),
                null
        );
    }
}