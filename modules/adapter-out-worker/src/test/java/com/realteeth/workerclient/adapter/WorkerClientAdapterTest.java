package com.realteeth.workerclient.adapter;

import com.realteeth.dto.worker.WorkerProcessingInfo;
import com.realteeth.error.exception.BusinessException;
import com.realteeth.workerclient.config.WorkerClientProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WorkerClientAdapterTest {

    private WorkerClientAdapter adapter;
    private MockRestServiceServer server;
    private WorkerClientProperties properties;

    @BeforeEach
    void setUp() {
        properties = new WorkerClientProperties(
                "https://dev.realteeth.ai",
                "홍길동",
                "abc@example.com"
        );

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(properties.baseUrl());

        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new WorkerClientAdapter(builder.build(), properties);
    }

    @Test
    @DisplayName("getApiKey - 성공하면 apiKey를 반환한다")
    void getApiKey_success() {
        server.expect(requestTo("https://dev.realteeth.ai/mock/auth/issue-key"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.candidateName").value("홍길동"))
                .andExpect(jsonPath("$.email").value("abc@example.com"))
                .andRespond(withSuccess("""
                        {
                          "apiKey": "mock_test_key"
                        }
                        """, MediaType.APPLICATION_JSON));

        String apiKey = adapter.getApiKey();

        assertThat(apiKey).isEqualTo("mock_test_key");
        server.verify();
    }

    @Test
    @DisplayName("getApiKey - 응답 body에 apiKey가 없으면 BusinessException이 발생하고 retryable은 false다")
    void getApiKey_fail_whenApiKeyMissing() {
        server.expect(requestTo("https://dev.realteeth.ai/mock/auth/issue-key"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "detail": "issue key failed"
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.getApiKey())
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.isRetryable()).isFalse();
                });

        server.verify();
    }

    @Test
    @DisplayName("processStart - 4xx 응답이면 BusinessException이 발생하고 retryable은 false다")
    void processStart_fail_when4xx() {
        server.expect(requestTo("https://dev.realteeth.ai/mock/process"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-API-KEY", "mock_test_key"))
                .andRespond(withStatus(HttpStatusCode.valueOf(400))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "detail": "bad request"
                                }
                                """));

        assertThatThrownBy(() ->
                adapter.processStart("mock_test_key", "https://images.example.com/test.jpg")
        ).isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.isRetryable()).isFalse();
                });

        server.verify();
    }

    @Test
    @DisplayName("processStart - 5xx 응답이면 BusinessException이 발생하고 retryable은 true다")
    void processStart_fail_when5xx() {
        server.expect(requestTo("https://dev.realteeth.ai/mock/process"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-API-KEY", "mock_test_key"))
                .andRespond(withStatus(HttpStatusCode.valueOf(503))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "detail": "service unavailable"
                                }
                                """));

        assertThatThrownBy(() ->
                adapter.processStart("mock_test_key", "https://images.example.com/test.jpg")
        ).isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.isRetryable()).isTrue();
                });

        server.verify();
    }

    @Test
    @DisplayName("getProcessingInfo - 성공하면 작업 정보를 반환한다")
    void getProcessingInfo_success() {
        server.expect(requestTo("https://dev.realteeth.ai/mock/process/worker-job-123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "jobId": "worker-job-123",
                          "status": "PROCESSING",
                          "result": null,
                          "detail": null
                        }
                        """, MediaType.APPLICATION_JSON));

        WorkerProcessingInfo info = adapter.getProcessingInfo("worker-job-123");

        assertThat(info).isNotNull();
        assertThat(info.jobId()).isEqualTo("worker-job-123");
        assertThat(info.status()).isEqualTo("PROCESSING");
        server.verify();
    }

    @Test
    @DisplayName("getApiKey - 네트워크 예외가 발생하면 BusinessException으로 변환되고 retryable은 true다")
    void getApiKey_fail_whenNetworkError() {
        ClientHttpRequestFactory failingRequestFactory = new ClientHttpRequestFactory() {
            @Override
            public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
                throw new IOException("connection timeout");
            }
        };

        RestClient restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(failingRequestFactory)
                .build();

        WorkerClientAdapter failingAdapter = new WorkerClientAdapter(restClient, properties);

        assertThatThrownBy(failingAdapter::getApiKey)
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.isRetryable()).isTrue();
                });
    }
}