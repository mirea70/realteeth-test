package com.realteeth.workerclient.adapter;

import com.realteeth.dto.worker.WorkerProcessingInfo;
import com.realteeth.error.exception.BusinessException;
import com.realteeth.workerclient.config.WorkerClientProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WorkerClientAdapterTest {
    private WorkerClientAdapter workerClientAdapter;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        WorkerClientProperties properties = new WorkerClientProperties(
                "https://dev.realteeth.ai",
                "홍길동",
                "abc@example.com"
        );

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(properties.baseUrl());

        this.server = MockRestServiceServer.bindTo(builder).build();

        RestClient restClient = builder.build();
        this.workerClientAdapter = new WorkerClientAdapter(restClient, properties);
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

        String apiKey = workerClientAdapter.getApiKey();

        assertThat(apiKey).isEqualTo("mock_test_key");
        server.verify();
    }

    @Test
    @DisplayName("getApiKey - 응답 body에 apiKey가 없으면 BusinessException이 발생한다")
    void getApiKey_fail_whenApiKeyMissing() {
        server.expect(requestTo("https://dev.realteeth.ai/mock/auth/issue-key"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "detail": "issue key failed"
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> workerClientAdapter.getApiKey())
                .isInstanceOf(BusinessException.class);

        server.verify();
    }

    @Test
    @DisplayName("getApiKey - HTTP 오류가 발생하면 BusinessException이 발생한다")
    void getApiKey_fail_whenHttpError() {
        server.expect(requestTo("https://dev.realteeth.ai/mock/auth/issue-key"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatusCode.valueOf(500))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "detail": "internal server error"
                                }
                                """));

        assertThatThrownBy(() -> workerClientAdapter.getApiKey())
                .isInstanceOf(BusinessException.class);

        server.verify();
    }

    @Test
    @DisplayName("processStart - 성공하면 worker jobId를 반환한다")
    void processStart_success() {
        server.expect(requestTo("https://dev.realteeth.ai/mock/process"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-API-KEY", "mock_test_key"))
                .andExpect(jsonPath("$.imageUrl").value("https://images.example.com/test.jpg"))
                .andRespond(withSuccess("""
                        {
                          "jobId": "worker-job-123"
                        }
                        """, MediaType.APPLICATION_JSON));

        String jobId = workerClientAdapter.processStart(
                "mock_test_key",
                "https://images.example.com/test.jpg"
        );

        assertThat(jobId).isEqualTo("worker-job-123");
        server.verify();
    }

    @Test
    @DisplayName("processStart - 응답 body에 jobId가 없으면 BusinessException이 발생한다")
    void processStart_fail_whenJobIdMissing() {
        server.expect(requestTo("https://dev.realteeth.ai/mock/process"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-API-KEY", "mock_test_key"))
                .andRespond(withSuccess("""
                        {
                          "detail": "process start failed"
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> workerClientAdapter.processStart(
                "mock_test_key",
                "https://images.example.com/test.jpg"
        )).isInstanceOf(BusinessException.class);

        server.verify();
    }

    @Test
    @DisplayName("processStart - HTTP 400 오류가 발생하면 BusinessException이 발생한다")
    void processStart_fail_whenHttp400() {
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

        assertThatThrownBy(() -> workerClientAdapter.processStart(
                "mock_test_key",
                "https://images.example.com/test.jpg"
        )).isInstanceOf(BusinessException.class);

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

        WorkerProcessingInfo info = workerClientAdapter.getProcessingInfo("worker-job-123");

        assertThat(info).isNotNull();
        assertThat(info.jobId()).isEqualTo("worker-job-123");
        assertThat(info.status()).isEqualTo("PROCESSING");
        server.verify();
    }

    @Test
    @DisplayName("getProcessingInfo - 응답 body에 jobId가 없으면 BusinessException이 발생한다")
    void getProcessingInfo_fail_whenJobIdMissing() {
        server.expect(requestTo("https://dev.realteeth.ai/mock/process/worker-job-123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "status": "PROCESSING",
                          "detail": "job id missing"
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> workerClientAdapter.getProcessingInfo("worker-job-123"))
                .isInstanceOf(BusinessException.class);

        server.verify();
    }

    @Test
    @DisplayName("getProcessingInfo - HTTP 500 오류가 발생하면 BusinessException이 발생한다")
    void getProcessingInfo_fail_whenHttp500() {
        server.expect(requestTo("https://dev.realteeth.ai/mock/process/worker-job-123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatusCode.valueOf(500))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "detail": "server error"
                                }
                                """));

        assertThatThrownBy(() -> workerClientAdapter.getProcessingInfo("worker-job-123"))
                .isInstanceOf(BusinessException.class);

        server.verify();
    }
}