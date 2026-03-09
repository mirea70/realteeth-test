package com.realteeth.imagejob.controller;

import com.realteeth.dto.response.ImageJobResponse;
import com.realteeth.port.in.ImageJobUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/imageJobs")
@RequiredArgsConstructor
public class ImageJobController {
    private final ImageJobUseCase imageJobUseCase;

    @GetMapping("/{id}")
    public ResponseEntity<ImageJobResponse> readOne(@PathVariable Long id) {
        return ResponseEntity.ok(imageJobUseCase.readOne(id));
    }
}
