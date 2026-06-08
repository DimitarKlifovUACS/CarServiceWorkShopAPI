package com.example.CarService.service;

import com.example.CarService.dto.request.WorkItemRequest;
import com.example.CarService.dto.response.ServiceOrderResponse;
import com.example.CarService.dto.response.WorkItemResponse;
import com.example.CarService.entity.ServiceOrder;
import com.example.CarService.entity.WorkItem;
import com.example.CarService.exception.ResourceNotFoundException;
import com.example.CarService.repository.ServiceOrderRepository;
import com.example.CarService.repository.WorkItemRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class WorkItemService {

    private final WorkItemRepository workItemRepository;

    private final ServiceOrderRepository serviceOrderRepository;

    public WorkItemService(WorkItemRepository workItemRepository, ServiceOrderRepository serviceOrderRepository) {
        this.workItemRepository = workItemRepository;
        this.serviceOrderRepository = serviceOrderRepository;
    }

    public WorkItemResponse addWorkItem(Long orderId, WorkItemRequest request){
        ServiceOrder order = serviceOrderRepository.findById(orderId)
                .orElseThrow(()-> new ResourceNotFoundException("Order not found"));

        WorkItem workItem = new WorkItem();
        workItem.setOrder(order);
        workItem.setStatus("IN_PROGRESS");

        workItem.setDescription(request.getDescription());
        workItem.setEstimatedHours(request.getEstimatedHours());
        workItem.setMechanicName(request.getMechanicName());

        WorkItem saved = workItemRepository.save(workItem);

        WorkItemResponse response = new WorkItemResponse();
        response.setId(saved.getId());
        response.setDescription(saved.getDescription());
        response.setEstimatedHours(saved.getEstimatedHours());
        response.setMechanicName(saved.getMechanicName());
        response.setStatus(saved.getStatus());

        return response;

    }

    public WorkItemResponse finishWorkItem(Long id){
        WorkItem workItem = workItemRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("Work item not found"));

        workItem.setStatus("DONE");

        WorkItem saved = workItemRepository.save(workItem);

        WorkItemResponse response = new WorkItemResponse();
        response.setId(saved.getId());
        response.setDescription(saved.getDescription());
        response.setEstimatedHours(saved.getEstimatedHours());
        response.setMechanicName(saved.getMechanicName());
        response.setStatus(saved.getStatus());

        return response;
    }

    public List<WorkItemResponse> getWorkItemsForOrder(Long orderId) {
        List<WorkItem> workItems = workItemRepository.findAllByOrderIdAndDeletedAtIsNull(orderId);
        List<WorkItemResponse> responses = new ArrayList<>();

        for (WorkItem workItem : workItems) {
            WorkItemResponse response = new WorkItemResponse();
            response.setId(workItem.getId());
            response.setEstimatedHours(workItem.getEstimatedHours());
            response.setStatus(workItem.getStatus());
            response.setMechanicName(workItem.getMechanicName());
            response.setDescription(workItem.getDescription());

            responses.add(response);
        }

        return responses;
    }

    public void deleteWorkItem(Long id) {
        WorkItem workItem = workItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work item not found"));

        workItem.setDeletedAt(LocalDateTime.now());
        workItemRepository.save(workItem);
    }
}
