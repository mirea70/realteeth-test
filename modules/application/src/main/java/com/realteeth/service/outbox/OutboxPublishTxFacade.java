package com.realteeth.service.outbox;

import com.realteeth.error.exception.BusinessException;
import com.realteeth.error.info.CommonDomainErrorInfo;
import com.realteeth.imagejob.model.ImageJobId;
import com.realteeth.outbox.OutboxEvent;
import com.realteeth.outbox.OutboxEventType;
import com.realteeth.port.out.ImageJobPersistenceOutport;
import com.realteeth.port.out.OutboxPersistenceOutport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublishTxFacade {
    private final OutboxPersistenceOutport outboxPersistenceOutport;
    private final ImageJobPersistenceOutport imageJobPersistenceOutport;

    @Transactional
    public boolean completePublished(OutboxEvent event) {
        boolean outboxMarked = outboxPersistenceOutport.markPublishedDirectly(event.getId());
        if (!outboxMarked) {
            log.warn("아웃박스 이벤트 발행이 실패하였습니다. eventId={}", event.getId());
            return false;
        }

        LocalDateTime now = LocalDateTime.now();

        if (event.getType() == OutboxEventType.DISPATCH) {
            boolean imageJobMarked = imageJobPersistenceOutport.markPublishedDirectly(
                    new ImageJobId(event.getDomainId()), now);

            if (!imageJobMarked) {
                log.info("ImageJob {} 상태 변경 실패 (이미 처리되었거나 다른 상태일 수 있음). outboxEventId={}", event.getDomainId(),
                        event.getId());
            }
        }

        return true;
    }

    @Transactional
    public void rollbackToPending(Long eventId) {
        outboxPersistenceOutport.markPendingAgainDirectly(eventId);
    }
}
