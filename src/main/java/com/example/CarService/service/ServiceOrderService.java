package com.example.CarService.service;

import com.example.CarService.dto.response.ServiceOrderResponse;
import com.example.CarService.entity.ServiceOrder;
import com.example.CarService.entity.Vehicle;
import com.example.CarService.entity.WorkItemStatus;
import com.example.CarService.exception.BadRequestException;
import com.example.CarService.exception.BusinessException;
import com.example.CarService.exception.ResourceNotFoundException;
import com.example.CarService.repository.ServiceOrderRepository;
import com.example.CarService.repository.VehicleRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
        Vehicle vehicle = vehicleRepository.findByIdAndDeletedAtIsNull(vehicleId)
                .orElseThrow(()-> new ResourceNotFoundException("Vehicle not found"));

        ServiceOrder order = new ServiceOrder();
        order.setVehicle(vehicle);
        order.setStatus("OPEN");
        order.setCreatedAt(LocalDateTime.now());

        return toResponse(serviceOrderRepository.save(order));
    }


    public ServiceOrderResponse getOrderById(Long id){
        ServiceOrder order = serviceOrderRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(()-> new ResourceNotFoundException("Order not found"));
        return toResponse(order);
    }

    // All active orders of active vehicles, newest first; status (optional) is OPEN or COMPLETED
    public List<ServiceOrderResponse> getOrders(String status) {
        List<ServiceOrder> orders;
        if (status == null || status.isBlank()) {
            orders = serviceOrderRepository.findAllByDeletedAtIsNullAndVehicleDeletedAtIsNullOrderByCreatedAtDesc();
        } else {
            String normalized = status.trim().toUpperCase();
            if (!ServiceOrder.STATUSES.contains(normalized)) {
                throw new BadRequestException("Unknown order status '" + status + "'. Use OPEN or COMPLETED");
            }
            orders = serviceOrderRepository.findAllByStatusAndDeletedAtIsNullAndVehicleDeletedAtIsNullOrderByCreatedAtDesc(normalized);
        }
        return orders.stream().map(this::toResponse).toList();
    }

    @Transactional
    public ServiceOrderResponse completeOrder(Long id){
        ServiceOrder order = serviceOrderRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(()-> new ResourceNotFoundException("Order not found"));
        if (order.isCompleted()) {
            throw new BusinessException("Order is already completed");
        }

        // Every active work item must be DONE (TODO and IN_PROGRESS both block). Deleted items no longer count.
        boolean hasUnfinishedWork = order.getWorkItems()
                .stream()
                .filter(item -> item.getDeletedAt() == null)
                .anyMatch(item -> item.getStatus() != WorkItemStatus.DONE);

        if (hasUnfinishedWork){
            throw new BusinessException("Cannot complete order while work items are not done");
        }

        order.setStatus("COMPLETED");
        order.setCompletedAt(LocalDateTime.now());

        return toResponse(serviceOrderRepository.save(order));
    }

    public List<ServiceOrderResponse> getOrdersByVehicle(Long vehicleId) {
        // A deleted (or unknown) vehicle has no visible orders: 404 instead of listing them
        vehicleRepository.findByIdAndDeletedAtIsNull(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        return serviceOrderRepository.findAllByVehicleIdAndDeletedAtIsNullOrderByCreatedAtDesc(vehicleId)
                .stream().map(this::toResponse).toList();
    }

    public void deleteOrder(Long id) {
        ServiceOrder order = serviceOrderRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        order.setDeletedAt(LocalDateTime.now());
        serviceOrderRepository.save(order);
    }

    // One place that builds the response, so every endpoint returns the same fields
    private ServiceOrderResponse toResponse(ServiceOrder order) {
        ServiceOrderResponse response = new ServiceOrderResponse();
        response.setId(order.getId());
        response.setStatus(order.getStatus());
        response.setCreatedAt(order.getCreatedAt());
        response.setCompletedAt(order.getCompletedAt());
        response.setVehicleId(order.getVehicle().getId());
        response.setLicensePlate(order.getVehicle().getLicensePlate());
        return response;
    }
}
