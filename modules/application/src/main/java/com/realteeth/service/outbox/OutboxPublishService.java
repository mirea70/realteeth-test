package com.realteeth.service.outbox;

import com.realteeth.outbox.OutboxEvent;
import com.realteeth.port.in.OutboxPublishUseCase;
import com.realteeth.port.out.MessagePublisher;
import com.realteeth.port.out.OutboxPersistenceOutport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublishService implements OutboxPublishUseCase {
    private final OutboxPersistenceOutport outboxPersistenceOutport;
    private final MessagePublisher messagePublisher;
    private final OutboxPublishTxFacade outboxPublishTxFacade;

    public int publishPending(int batchSize) {
        log.info("[OutboxPublishService] publishPending start...");
        List<OutboxEvent> events = outboxPersistenceOutport.findOnPending(batchSize);

        int publishedCount = 0;

        for (OutboxEvent event : events) {
            boolean claimed = outboxPersistenceOutport.markPublishingDirectly(event.getId());
            if (!claimed) {
                continue;
            }

            try {
                messagePublisher.publish(event.getDomainType(), event.getType(), event.getPayload());
                boolean completed = outboxPublishTxFacade.completePublished(event);
                if (completed) {
                    publishedCount++;
                }
            } catch (Exception e) {
                log.error("[PUBLISHING] -> [PUBLISHED] Failed. eventId : " + event.getId(), e);
                try {
                    outboxPublishTxFacade.rollbackToPending(event.getId());
                } catch (Exception rollbackEx) {
                    log.error("아웃박스 이벤트를 PENDING 상태로 롤백하는데 실패하였습니다. eventId={}", event.getId(), rollbackEx);
                }
            }
        }

        return publishedCount;
    }
}
