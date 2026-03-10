package com.realteeth.imagejob.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.realteeth.dto.response.ImageJobResponse;
import com.realteeth.imagejob.dto.request.ImageJobProcessRequest;
import com.realteeth.imagejob.model.ImageJobStatus;
import com.realteeth.port.in.ImageJobUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.only;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ImageJobController.class)
class ImageJobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImageJobUseCase imageJobUseCase;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("이미지 작업 조회 요청이 오면 결과가 잘 조회되고, 200 OK를 반환한다.")
    void readOne_success() throws Exception {
        // given
        Long id = 1L;
        LocalDateTime now = LocalDateTime.now();

        ImageJobResponse response = new ImageJobResponse(
                id,
                "https://example.com/image.png",
                "ACCEPTED",
                null,
                null,
                null,
                null,
                null,
                now,
                now
        );

        given(imageJobUseCase.readOne(id))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/imageJobs/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageJobId").value(id));
    }

    @DisplayName("process 요청이 오면 sourceImageUrl로 이미지 작업을 등록하고 200 OK를 반환한다")
    @Test
    void process_success() throws Exception {
        // given
        String sourceImageUrl = "https://example.com/image.png";

        ImageJobProcessRequest request = new ImageJobProcessRequest(sourceImageUrl);

        LocalDateTime now = LocalDateTime.now();
        Long imageJobId = 1L;
        ImageJobStatus imageJobStatus = ImageJobStatus.PUBLISH_PENDING;

        ImageJobResponse response = new ImageJobResponse(
                imageJobId,
                sourceImageUrl,
                imageJobStatus.name(),
                null,
                null,
                null,
                null,
                null,
                now,
                now
        );

        given(imageJobUseCase.register(sourceImageUrl)).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/imageJobs/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageJobId").value(imageJobId))
                .andExpect(jsonPath("$.sourceImageUrl").value(sourceImageUrl))
                .andExpect(jsonPath("$.status").value(imageJobStatus.name()));

        then(imageJobUseCase).should(only()).register(sourceImageUrl);
    }
}