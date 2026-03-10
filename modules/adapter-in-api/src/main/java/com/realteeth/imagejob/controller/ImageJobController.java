package com.realteeth.imagejob.controller;

import com.realteeth.dto.response.ImageJobResponse;
import com.realteeth.imagejob.dto.request.ImageJobProcessRequest;
import com.realteeth.port.in.ImageJobUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/imageJobs")
@RequiredArgsConstructor
public class ImageJobController {
    private final ImageJobUseCase imageJobUseCase;

    @GetMapping("/{id}")
    public ResponseEntity<ImageJobResponse> readOne(@PathVariable Long id) {
        return ResponseEntity.ok(imageJobUseCase.readOne(id));
    }

    @PostMapping("/process")
    public ResponseEntity<ImageJobResponse> process(@RequestBody ImageJobProcessRequest request) {
        return ResponseEntity.ok(imageJobUseCase.register(request.sourceImageUrl()));
    }
}
