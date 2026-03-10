package com.realteeth.scheduler.outbox;

import com.realteeth.service.outbox.OutboxPublishService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxPublishScheduler {
    private final OutboxPublishService outboxPublishService;

    @Scheduled(fixedDelay = 1500)
    public void publishMessages() {
        outboxPublishService.publishPending(100);
    }
}
