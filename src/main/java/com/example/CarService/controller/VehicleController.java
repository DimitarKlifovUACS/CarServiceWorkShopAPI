package com.example.CarService.controller;

import com.example.CarService.dto.request.VehicleRequest;
import com.example.CarService.dto.response.VehicleResponse;
import com.example.CarService.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController

public class VehicleController {
    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping("/vehicles")
    public ResponseEntity<VehicleResponse> createVehicle(@Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.status(201).body(vehicleService.createVehicle(request));
    }

    @GetMapping("/vehicles")
    public ResponseEntity<List<VehicleResponse>> getAllVehicles(Pageable pageable){
        return ResponseEntity.ok(vehicleService.getAllVehicles(pageable));
    }

    @DeleteMapping("/vehicles/{id}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long id){
        vehicleService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }


}
