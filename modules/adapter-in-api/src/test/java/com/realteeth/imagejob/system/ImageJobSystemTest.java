package com.realteeth.imagejob.system;

import com.realteeth.dto.response.ImageJobResponse;
import com.realteeth.imagejob.model.ImageJob;
import com.realteeth.jpa.imagejob.entity.ImageJobJpaEntity;
import com.realteeth.jpa.imagejob.repository.ImageJobJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ImageJobSystemTest {
    @LocalServerPort
    private int port;

    private RestClient restClient;

    @Autowired
    private ImageJobJpaRepository imageJobJpaRepository;

    @BeforeEach
    public void setup() {
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @DisplayName("이미지 작업 단건 조회 성공 케이스")
    @Test
    void readOne() {
        // given
        Long imageJobId = 1L;
        String sourceImageUrl = "https://naver.com/files/108";
        LocalDateTime now = LocalDateTime.now();
        ImageJob imageJob = ImageJob.createNew(
                imageJobId,
                sourceImageUrl,
                now
        );

        imageJobJpaRepository.save(ImageJobJpaEntity.from(imageJob));

        // when
        ResponseEntity<ImageJobResponse> response = whenReadOne(imageJobId);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().imageJobId()).isEqualTo(imageJobId);
    }

    private ResponseEntity<ImageJobResponse> whenReadOne(Long imageJobId) {
        return restClient.get()
                .uri("/api/v1/imageJobs/{id}",imageJobId)
                .retrieve()
                .toEntity(ImageJobResponse.class);
    }
}
