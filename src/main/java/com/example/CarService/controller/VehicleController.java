package com.example.CarService.controller;

import com.example.CarService.dto.request.VehicleRequest;
import com.example.CarService.dto.response.PageResponse;
import com.example.CarService.dto.response.VehicleResponse;
import com.example.CarService.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    // GET /vehicles?search=bmw&page=0&size=20&sort=licensePlate,asc  (all parameters optional)
    @GetMapping("/vehicles")
    public ResponseEntity<PageResponse<VehicleResponse>> getAllVehicles(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable){
        return ResponseEntity.ok(vehicleService.getAllVehicles(search, pageable));
    }

    @GetMapping("/vehicles/{id}")
    public ResponseEntity<VehicleResponse> getVehicleById(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleService.getVehicleById(id));
    }

    @DeleteMapping("/vehicles/{id}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long id){
        vehicleService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }


}
