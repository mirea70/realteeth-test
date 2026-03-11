package com.realteeth.worker.consumer;

import com.realteeth.mq.config.RabbitMqConfig;
import com.realteeth.port.out.DataSerializerOutPort;
import com.realteeth.service.imagejob.ImageJobDelegateService;
import com.realteeth.worker.WorkerDispatchEventPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImageJobMessageConsumer {
    private final DataSerializerOutPort dataSerializerOutPort;
    private final ImageJobDelegateService imageJobDelegateService;

    @RabbitListener(queues = RabbitMqConfig.DISPATCH_QUEUE)
    public void dispatch(String payload) {
        WorkerDispatchEventPayload message = dataSerializerOutPort.deserialize(payload, WorkerDispatchEventPayload.class);
        imageJobDelegateService.dispatch(message.imageJobId());
    }
}
