package com.example.CarService.service;

import com.example.CarService.dto.request.PartRequest;
import com.example.CarService.dto.response.PartResponse;
import com.example.CarService.entity.Part;
import com.example.CarService.entity.ServiceOrder;
import com.example.CarService.exception.BusinessException;
import com.example.CarService.exception.ResourceNotFoundException;
import com.example.CarService.repository.PartRepository;
import com.example.CarService.repository.ServiceOrderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PartService {

    private final PartRepository partRepository;
    private final ServiceOrderRepository serviceOrderRepository;

    public PartService(PartRepository partRepository, ServiceOrderRepository serviceOrderRepository) {
        this.partRepository = partRepository;
        this.serviceOrderRepository = serviceOrderRepository;
    }

    public PartResponse addPart(Long orderId, PartRequest request){
        ServiceOrder order = serviceOrderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(()-> new ResourceNotFoundException("Order not found"));
        if (order.isCompleted()) {
            throw new BusinessException("Cannot add parts to a completed order");
        }

        Part part = new Part();
        part.setName(request.getName());
        part.setUnitPrice(request.getUnitPrice());
        part.setQuantity(request.getQuantity());
        part.setOrder(order);

        return toResponse(partRepository.save(part));
    }

    public List<PartResponse> getPartsForOrder(Long orderId) {
        // A deleted (or unknown) order has no visible parts: 404 instead of listing them
        serviceOrderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        return partRepository.findAllByOrderIdAndDeletedAtIsNullOrderByIdAsc(orderId)
                .stream().map(this::toResponse).toList();
    }

    public void deletePart(Long id) {
        Part part = partRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Part not found"));
        if (part.getOrder().isCompleted()) {
            throw new BusinessException("Cannot delete parts of a completed order");
        }

        part.setDeletedAt(LocalDateTime.now());
        partRepository.save(part);
    }

    private PartResponse toResponse(Part part) {
        PartResponse response = new PartResponse();
        response.setId(part.getId());
        response.setName(part.getName());
        response.setQuantity(part.getQuantity());
        response.setUnitPrice(part.getUnitPrice());
        return response;
    }
}
