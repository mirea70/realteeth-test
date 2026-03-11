package com.realteeth.workerclient.adapter;

import com.realteeth.dto.worker.WorkerProcessingInfo;
import com.realteeth.workerclient.config.WorkerClientProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class WorkerClientAdapterTest {
    private static WorkerClientProperties properties;
    private static RestClient restClient;
    private static WorkerClientAdapter workerClientAdapter;

    @BeforeAll
    static void init() {
        properties = new WorkerClientProperties(
                "https://dev.realteeth.ai",
                "홍길동",
                "abc@example.com"
        );

        restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .build();

        workerClientAdapter = new WorkerClientAdapter(restClient, properties);
    }


    @Test
    @DisplayName("Mock Worker에 API Key 발급 요청을 보내 성공하면 apiKey 값을 반환한다.")
    void getApiKey() {
        // when
        String apiKey = workerClientAdapter.getApiKey();

        // then
        assertThat(apiKey).isNotNull();
        assertThat(apiKey).isNotBlank();
        System.out.println("apiKey = " + apiKey);
        assertThat(apiKey).startsWith("mock_");
    }

    @Test
    @DisplayName("Mock Worker에 이미지 처리 시작 요청을 보내 성공하면 Work의 작업 Id를 반환한다.")
    void processStart() {
        // given
        String apiKey = workerClientAdapter.getApiKey();
        String imageUrl = "https://images.unsplash.com/photo-1503023345310-bd7c1de61c7d";

        // when
        String jobId = workerClientAdapter.processStart(apiKey, imageUrl);

        // then
        assertThat(jobId).isNotNull();
        assertThat(jobId).isNotBlank();

        System.out.println("apiKey = " + apiKey);
        System.out.println("jobId = " + jobId);
    }

    @Test
    @DisplayName("Mock Worker에서 작업 상태 조회")
    void getProcessingInfo() {
        // given
        String apiKey = workerClientAdapter.getApiKey();
        String imageUrl = "https://images.unsplash.com/photo-1503023345310-bd7c1de61c7d";
        String jobId = workerClientAdapter.processStart(apiKey, imageUrl);

        // when
        WorkerProcessingInfo info = workerClientAdapter.getProcessingInfo(jobId);

        // then
        assertThat(info).isNotNull();
        assertThat(info.jobId()).isEqualTo(jobId);

        System.out.println("jobId = " + jobId);
        System.out.println("status = " + info.status());
    }
}