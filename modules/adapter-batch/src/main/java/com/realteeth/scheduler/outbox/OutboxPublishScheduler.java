package com.realteeth.scheduler.outbox;

import com.realteeth.port.in.OutboxPublishUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxPublishScheduler {
    private final OutboxPublishUseCase outboxPublishUseCase;

    @Scheduled(fixedDelay = 1500)
    public void publishMessages() {
        outboxPublishUseCase.publishPending(100);
    }
}
