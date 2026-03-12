package com.realteeth.scheduler.imagejob;

import com.realteeth.service.imagejob.ImageJobRecoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageJobRecoverScheduler {

    private final ImageJobRecoveryService imageJobRecoveryService;

    @Scheduled(fixedDelayString = "${scheduler.imagejob.recover.dispatching:60000}")
    public void runDispatchingRecovery() {
        log.info("Starting ImageJob DISPATCHING recovery scheduler...");
        try {
            imageJobRecoveryService.recoverDispatchingJobs();
        } catch (Exception e) {
            log.error("이미지 작업 DISPATCHING 복구 실행 중 에러 발생 : {}", e.getMessage(), e);
        }
        log.info("Finished ImageJob DISPATCHING recovery scheduler.");
    }

    @Scheduled(fixedDelayString = "${scheduler.imagejob.recover.processing:180000}")
    public void runProcessingRecovery() {
        log.info("Starting ImageJob PROCESSING recovery scheduler...");
        try {
            imageJobRecoveryService.recoverProcessingJobs();
        } catch (Exception e) {
            log.error("이미지 작업 PROCESSING 복구 실행 중 에러 발생 : {}", e.getMessage(), e);
        }
        log.info("Finished ImageJob PROCESSING recovery scheduler.");
    }
}
