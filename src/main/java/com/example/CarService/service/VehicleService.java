package com.example.CarService.service;

import com.example.CarService.dto.request.VehicleRequest;
import com.example.CarService.dto.response.PageResponse;
import com.example.CarService.dto.response.VehicleResponse;
import com.example.CarService.entity.Vehicle;
import com.example.CarService.exception.BusinessException;
import com.example.CarService.exception.ResourceNotFoundException;
import com.example.CarService.repository.VehicleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class VehicleService {

    // Vehicles built before this year are flagged as "old"
    private static final int OLD_VEHICLE_BEFORE_YEAR = 2020;

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public VehicleResponse createVehicle(VehicleRequest vehicleRequest){
        // Plates are unique among active (not deleted) vehicles; check first so the client gets a clear 409 instead of a 500
        if (vehicleRepository.existsByLicensePlateAndDeletedAtIsNull(vehicleRequest.getLicensePlate())) {
            throw new BusinessException("A vehicle with license plate " + vehicleRequest.getLicensePlate() + " already exists");
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setLicensePlate(vehicleRequest.getLicensePlate());
        vehicle.setOwnerName(vehicleRequest.getOwnerName());
        vehicle.setMake(vehicleRequest.getMake());
        vehicle.setModel(vehicleRequest.getModel());
        vehicle.setYear(vehicleRequest.getYear());

        return toResponse(vehicleRepository.save(vehicle));
    }

    public VehicleResponse getVehicleById(Long id) {
        Vehicle vehicle = vehicleRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        return toResponse(vehicle);
    }

    // search (optional) matches license plate, owner, make or model, ignoring upper/lower case
    public PageResponse<VehicleResponse> getAllVehicles(String search, Pageable pageable){
        Page<Vehicle> page = (search == null || search.isBlank())
                ? vehicleRepository.findAllByDeletedAtIsNull(pageable)
                : vehicleRepository.searchActive(search.trim(), pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    public void deleteVehicle(Long id){
        Vehicle vehicle = vehicleRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(()-> new ResourceNotFoundException("Vehicle not found"));

        vehicle.setDeletedAt(LocalDateTime.now());
        vehicleRepository.save(vehicle);
    }

    // One place that builds the response, so every endpoint returns the same fields (including "old")
    private VehicleResponse toResponse(Vehicle vehicle) {
        VehicleResponse response = new VehicleResponse();
        response.setId(vehicle.getId());
        response.setLicensePlate(vehicle.getLicensePlate());
        response.setOwnerName(vehicle.getOwnerName());
        response.setMake(vehicle.getMake());
        response.setModel(vehicle.getModel());
        response.setYear(vehicle.getYear());
        response.setOld(vehicle.getYear() < OLD_VEHICLE_BEFORE_YEAR);
        return response;
    }
}
