package com.realteeth.scheduler.imagejob;

import com.realteeth.service.imagejob.ImageJobRecoveryService;
import com.realteeth.service.outbox.OutboxPublishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupRecoveryListener {

    private final ImageJobRecoveryService imageJobRecoveryService;
    private final OutboxPublishService outboxPublishService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Application started. Executing initial recovery and outbox publishing...");
        
        try {
            // Outbox 재발행 시도
            outboxPublishService.publishPending(100);
            
            // 각 상태별 복구 로직 1회 즉시 실행
            imageJobRecoveryService.recoverPublishedJobs();
            imageJobRecoveryService.recoverDispatchingJobs();
            imageJobRecoveryService.recoverProcessingJobs();
            
            log.info("Initial startup recovery completed.");
        } catch (Exception e) {
            log.error("Error during startup recovery: {}", e.getMessage(), e);
        }
    }
}
