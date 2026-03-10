package com.realteeth.port.in;

public interface OutboxPublishUseCase {
   int publishPending(int batchSize);
}
