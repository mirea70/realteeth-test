package com.realteeth.port.in;

import com.realteeth.dto.response.ImageJobResponse;

public interface ImageJobUseCase {
    ImageJobResponse readOne(Long imageJobId);
}
