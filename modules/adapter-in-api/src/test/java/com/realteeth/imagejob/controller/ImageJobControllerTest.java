package com.realteeth.imagejob.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.realteeth.dto.response.ImageJobResponse;
import com.realteeth.port.in.ImageJobUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    @DisplayName("이미지 작업 조회 성공 테스트")
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

        BDDMockito.given(imageJobUseCase.readOne(id))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/imageJobs/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageJobId").value(id));
    }
}