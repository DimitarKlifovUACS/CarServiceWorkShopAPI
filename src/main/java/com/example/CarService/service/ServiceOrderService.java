package com.example.CarService.service;

import com.example.CarService.dto.response.ServiceOrderResponse;
import com.example.CarService.dto.response.VehicleResponse;
import com.example.CarService.entity.ServiceOrder;
import com.example.CarService.entity.Vehicle;
import com.example.CarService.exception.BusinessException;
import com.example.CarService.exception.ResourceNotFoundException;
import com.example.CarService.repository.ServiceOrderRepository;
import com.example.CarService.repository.VehicleRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ServiceOrderService {

    private final ServiceOrderRepository serviceOrderRepository;
    private final VehicleRepository vehicleRepository;

    public ServiceOrderService(ServiceOrderRepository serviceOrderRepository, VehicleRepository vehicleRepository) {
        this.serviceOrderRepository = serviceOrderRepository;
        this.vehicleRepository = vehicleRepository;
    }

    public ServiceOrderResponse createOrder(Long vehicleId){
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(()-> new ResourceNotFoundException("Vehicle not found"));

        ServiceOrder order = new ServiceOrder();
        order.setVehicle(vehicle);
        order.setStatus("OPEN");
        order.setCreatedAt(LocalDateTime.now());

        ServiceOrder saved = serviceOrderRepository.save(order);

        ServiceOrderResponse response = new ServiceOrderResponse();
        response.setId(saved.getId());
        response.setStatus(saved.getStatus());
        response.setCreatedAt(saved.getCreatedAt());
        response.setCompletedAt(saved.getCompletedAt());

        return response;
    }


    public ServiceOrderResponse getOrderById(Long id){
        ServiceOrder order = serviceOrderRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Order not found"));

        ServiceOrderResponse response = new ServiceOrderResponse();
        response.setId(order.getId());
        response.setStatus(order.getStatus());
        response.setCreatedAt(order.getCreatedAt());
        response.setCompletedAt(order.getCompletedAt());

        return response;
    }

    @Transactional
    public ServiceOrderResponse completeOrder(Long id){
        ServiceOrder order = serviceOrderRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Order not found"));

        boolean hasInProgress = order.getWorkItems()
                .stream()
                .anyMatch(item -> "IN_PROGRESS".equals(item.getStatus()));

        if (hasInProgress){
            throw new BusinessException("Cannot complete order while work items are in progress");
        }

        order.setStatus("COMPLETED");
        order.setCompletedAt(LocalDateTime.now());

        ServiceOrder saved = serviceOrderRepository.save(order);

        ServiceOrderResponse response = new ServiceOrderResponse();
        response.setId(saved.getId());
        response.setStatus(saved.getStatus());
        response.setCreatedAt(saved.getCreatedAt());
        response.setCompletedAt(saved.getCompletedAt());

        return response;

    }

    public List<ServiceOrderResponse> getOrdersByVehicle(Long vehicleId) {
        List<ServiceOrder> orders = serviceOrderRepository.findAllByVehicleIdAndDeletedAtIsNull(vehicleId);
        List<ServiceOrderResponse> responses = new ArrayList<>();

        for (ServiceOrder order : orders) {
            ServiceOrderResponse response = new ServiceOrderResponse();
            response.setId(order.getId());
            response.setStatus(order.getStatus());
            response.setCompletedAt(order.getCompletedAt());
            response.setCreatedAt(order.getCreatedAt());

            responses.add(response);
        }

        return responses;
    }

    public void deleteOrder(Long id) {
        ServiceOrder order = serviceOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        order.setDeletedAt(LocalDateTime.now());
        serviceOrderRepository.save(order);
    }
}
