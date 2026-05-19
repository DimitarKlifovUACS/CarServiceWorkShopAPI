package com.example.CarService.controller;

import com.example.CarService.dto.response.ServiceOrderResponse;
import com.example.CarService.service.ServiceOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ServiceOrderController {
    private final ServiceOrderService serviceOrderService;

    public ServiceOrderController(ServiceOrderService serviceOrderService) {
        this.serviceOrderService = serviceOrderService;
    }

    @PostMapping("/vehicles/{vehicleId}/orders")
    public ResponseEntity<ServiceOrderResponse> createOrder(@PathVariable Long vehicleId){
        return ResponseEntity.status(201).body(serviceOrderService.createOrder(vehicleId));

    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<ServiceOrderResponse>getOrderById(@PathVariable Long id){
        return ResponseEntity.ok(serviceOrderService.getOrderById(id));
    }

    @PatchMapping("/orders/{id}/complete")
    public ResponseEntity<ServiceOrderResponse> completeOrder(@PathVariable Long id){
        return ResponseEntity.ok(serviceOrderService.completeOrder(id));
    }

    @GetMapping("/vehicles/{vehicleId}/orders")
    public ResponseEntity<List<ServiceOrderResponse>> getOrdersByVehicle(@PathVariable Long vehicleId) {
        return ResponseEntity.ok(serviceOrderService.getOrdersByVehicle(vehicleId));
    }

    @DeleteMapping("/orders/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        serviceOrderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}
