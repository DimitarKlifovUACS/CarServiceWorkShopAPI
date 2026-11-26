package com.example.CarService.service;

import com.example.CarService.dto.request.VehicleRequest;
import com.example.CarService.dto.response.VehicleResponse;
import com.example.CarService.entity.Vehicle;
import com.example.CarService.exception.BusinessException;
import com.example.CarService.exception.ResourceNotFoundException;
import com.example.CarService.repository.VehicleRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public VehicleResponse createVehicle(VehicleRequest vehicleRequest){
        // License plates are unique in the database; check first so the client gets a clear 409 instead of a 500
        if (vehicleRepository.existsByLicensePlate(vehicleRequest.getLicensePlate())) {
            throw new BusinessException("A vehicle with license plate " + vehicleRequest.getLicensePlate() + " already exists");
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setLicensePlate(vehicleRequest.getLicensePlate());
        vehicle.setOwnerName(vehicleRequest.getOwnerName());
        vehicle.setMake(vehicleRequest.getMake());
        vehicle.setModel(vehicleRequest.getModel());
        vehicle.setYear(vehicleRequest.getYear());

        Vehicle saved = vehicleRepository.save(vehicle);

        VehicleResponse response = new VehicleResponse();
        response.setId(saved.getId());
        response.setLicensePlate(saved.getLicensePlate());
        response.setOwnerName(saved.getOwnerName());
        response.setMake(saved.getMake());
        response.setModel(saved.getModel());
        response.setYear(saved.getYear());

        return response;
    }

    public List<VehicleResponse> getAllVehicles(Pageable pageable){
        List<Vehicle> vehicles = vehicleRepository.findAllByDeletedAtIsNull(pageable).getContent();
        List<VehicleResponse> responses = new ArrayList<>();

        for(Vehicle vehicle : vehicles){
            VehicleResponse response = new VehicleResponse();
            response.setId(vehicle.getId());
            response.setLicensePlate(vehicle.getLicensePlate());
            response.setMake(vehicle.getMake());
            response.setModel(vehicle.getModel());
            response.setYear(vehicle.getYear());
            response.setOwnerName(vehicle.getOwnerName());

            if(vehicle.getYear() < 2020){
                response.setOld(true);
            }

            responses.add(response);
        }

        return responses;
    }


    public void deleteVehicle(Long id){
        Vehicle vehicle = vehicleRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(()-> new ResourceNotFoundException("Vehicle not found"));

        vehicle.setDeletedAt(LocalDateTime.now());
        vehicleRepository.save(vehicle);
    }
}
