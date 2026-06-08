package com.example.CarService.service;

import com.example.CarService.dto.request.PartRequest;
import com.example.CarService.dto.response.PartResponse;
import com.example.CarService.entity.Part;
import com.example.CarService.entity.ServiceOrder;
import com.example.CarService.exception.ResourceNotFoundException;
import com.example.CarService.repository.PartRepository;
import com.example.CarService.repository.ServiceOrderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PartService {

    private final PartRepository partRepository;
    private final ServiceOrderRepository serviceOrderRepository;

    public PartService(PartRepository partRepository, ServiceOrderRepository serviceOrderRepository) {
        this.partRepository = partRepository;
        this.serviceOrderRepository = serviceOrderRepository;
    }

    public PartResponse addPart(Long orderId, PartRequest request){
        ServiceOrder order = serviceOrderRepository.findById(orderId)
                .orElseThrow(()-> new ResourceNotFoundException("Order not found"));

        Part part = new Part();
        part.setName(request.getName());
        part.setUnitPrice(request.getUnitPrice());
        part.setQuantity(request.getQuantity());
        part.setOrder(order);

        Part saved = partRepository.save(part);

        PartResponse response = new PartResponse();
        response.setId(saved.getId());
        response.setName(saved.getName());
        response.setQuantity(saved.getQuantity());
        response.setUnitPrice(saved.getUnitPrice());

        return response;


    }

    public void deletePart(Long id) {
        Part part = partRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Part not found"));

        part.setDeletedAt(LocalDateTime.now());
        partRepository.save(part);
    }
}
