package com.example.CarService.controller;

import com.example.CarService.dto.request.PartRequest;
import com.example.CarService.dto.response.PartResponse;
import com.example.CarService.service.PartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PartController {
    private final PartService partService;

    public PartController(PartService partService) {
        this.partService = partService;
    }

    @PostMapping("/orders/{orderId}/parts")
    public ResponseEntity<PartResponse> addPart(@PathVariable Long orderId, @Valid @RequestBody PartRequest request){
        return ResponseEntity.status(201).body(partService.addPart(orderId,request));

    }
}
