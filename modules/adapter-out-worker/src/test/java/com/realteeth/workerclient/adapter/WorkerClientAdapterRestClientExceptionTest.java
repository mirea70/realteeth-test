package com.realteeth.workerclient.adapter;

import com.realteeth.error.exception.BusinessException;
import com.realteeth.workerclient.config.WorkerClientProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkerClientAdapterRestClientExceptionTest {

    @Test
    @DisplayName("getApiKey - 네트워크 예외가 발생하면 BusinessException으로 변환한다")
    void getApiKey_fail_whenRestClientException() {
        WorkerClientProperties properties = new WorkerClientProperties(
                "https://dev.realteeth.ai",
                "홍길동",
                "abc@example.com"
        );

        ClientHttpRequestFactory failingRequestFactory = (uri, httpMethod) -> {
            throw new IOException("connection timeout");
        };

        RestClient restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(failingRequestFactory)
                .build();

        WorkerClientAdapter adapter = new WorkerClientAdapter(restClient, properties);

        assertThatThrownBy(adapter::getApiKey)
                .isInstanceOf(BusinessException.class);
    }
}