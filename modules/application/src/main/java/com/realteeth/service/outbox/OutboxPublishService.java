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

    public int publishPending(int batchSize) {
        log.info("[OutboxPublishService] publishPending start...");
        List<OutboxEvent> events = outboxPersistenceOutport.findOnPending(batchSize);

        int publishedCount = 0;

        for (OutboxEvent event : events) {
            boolean claimed = outboxPersistenceOutport.markPublishing(event.getId());
            if (!claimed) {
                continue;
            }

            try {
                messagePublisher.publish(event.getDomainType(), event.getType(), event.getPayload());
                boolean marked = outboxPersistenceOutport.markPublished(event.getId());
                if (marked) {
                    publishedCount++;
                }
            } catch (Exception e) {
                log.error("[PUBLISHING] -> [PUBLISHED] Failed. eventId : " + event.getId(), e);
                outboxPersistenceOutport.markPendingAgain(event.getId()); // 다시 발행 시도할 수 있게 복구
            }
        }

        return publishedCount;
    }
}
