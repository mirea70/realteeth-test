package com.realteeth.mq.adapter;

import com.realteeth.common.DomainType;
import com.realteeth.mq.config.RabbitMqConfig;
import com.realteeth.outbox.OutboxEventType;
import com.realteeth.port.out.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitMqPublishAdapter implements MessagePublisher {
    private final RabbitTemplate rabbitTemplate;

    public void publish(DomainType domainType, OutboxEventType eventType, String payload) {
        try {
            String routingKey = domainType.name() + "_" + eventType.name();
            rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, routingKey, payload);
        } catch (Exception e) {
            log.error("RabbitMQ publish failed. exchange={}, domainType={}, eventType={}, payload={}",
                    RabbitMqConfig.EXCHANGE, domainType, eventType, payload, e);
            throw e;
        }
    }
}
