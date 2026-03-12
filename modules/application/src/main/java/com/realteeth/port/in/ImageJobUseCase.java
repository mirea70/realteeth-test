package com.realteeth.port.in;

import com.realteeth.dto.response.ImageJobResponse;

import java.util.List;

public interface ImageJobUseCase {
    ImageJobResponse readOne(Long imageJobId);
    ImageJobResponse register(String sourceImageUrl);
    List<ImageJobResponse> readAll(int page, int size);
}
