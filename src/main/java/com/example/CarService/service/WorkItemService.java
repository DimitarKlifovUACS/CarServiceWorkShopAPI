package com.example.CarService.service;

import com.example.CarService.dto.request.WorkItemRequest;
import com.example.CarService.dto.response.WorkItemResponse;
import com.example.CarService.entity.ServiceOrder;
import com.example.CarService.entity.WorkItem;
import com.example.CarService.entity.WorkItemStatus;
import com.example.CarService.exception.BadRequestException;
import com.example.CarService.exception.BusinessException;
import com.example.CarService.exception.ResourceNotFoundException;
import com.example.CarService.repository.ServiceOrderRepository;
import com.example.CarService.repository.WorkItemRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
        ServiceOrder order = serviceOrderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(()-> new ResourceNotFoundException("Order not found"));
        if (order.isCompleted()) {
            throw new BusinessException("Cannot add work items to a completed order");
        }

        WorkItem workItem = new WorkItem();
        workItem.setOrder(order);
        workItem.setStatus(WorkItemStatus.TODO); // new work starts in the "To do" column of the board

        workItem.setDescription(request.getDescription());
        workItem.setEstimatedHours(request.getEstimatedHours());
        workItem.setMechanicName(request.getMechanicName());

        return toResponse(workItemRepository.save(workItem));
    }

    public WorkItemResponse finishWorkItem(Long id){
        WorkItem workItem = workItemRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(()->new ResourceNotFoundException("Work item not found"));
        if (workItem.getOrder().isCompleted()) {
            throw new BusinessException("Cannot change work items of a completed order");
        }

        workItem.setStatus(WorkItemStatus.DONE);

        return toResponse(workItemRepository.save(workItem));
    }

    // Used by the board: move a card to any column (TODO, IN_PROGRESS or DONE), in any direction
    public WorkItemResponse changeStatus(Long id, WorkItemStatus status) {
        WorkItem workItem = workItemRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work item not found"));
        if (workItem.getOrder().isCompleted()) {
            throw new BusinessException("Cannot change work items of a completed order");
        }

        workItem.setStatus(status);

        return toResponse(workItemRepository.save(workItem));
    }

    // All active work items across orders (for the board); orderStatus (optional) is OPEN or COMPLETED
    public List<WorkItemResponse> getWorkItems(String orderStatus) {
        String normalized = null;
        if (orderStatus != null && !orderStatus.isBlank()) {
            normalized = orderStatus.trim().toUpperCase();
            if (!ServiceOrder.STATUSES.contains(normalized)) {
                throw new BadRequestException("Unknown order status '" + orderStatus + "'. Use OPEN or COMPLETED");
            }
        }
        return workItemRepository.findActiveForBoard(normalized).stream().map(this::toResponse).toList();
    }

    public List<WorkItemResponse> getWorkItemsForOrder(Long orderId) {
        // A deleted (or unknown) order has no visible work items: 404 instead of listing them
        serviceOrderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        return workItemRepository.findAllByOrderIdAndDeletedAtIsNull(orderId)
                .stream().map(this::toResponse).toList();
    }

    public void deleteWorkItem(Long id) {
        WorkItem workItem = workItemRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work item not found"));
        if (workItem.getOrder().isCompleted()) {
            throw new BusinessException("Cannot delete work items of a completed order");
        }

        workItem.setDeletedAt(LocalDateTime.now());
        workItemRepository.save(workItem);
    }

    // One place that builds the response, so every endpoint returns the same fields
    private WorkItemResponse toResponse(WorkItem workItem) {
        WorkItemResponse response = new WorkItemResponse();
        response.setId(workItem.getId());
        response.setDescription(workItem.getDescription());
        response.setEstimatedHours(workItem.getEstimatedHours());
        response.setMechanicName(workItem.getMechanicName());
        response.setStatus(workItem.getStatus());
        response.setOrderId(workItem.getOrder().getId());
        response.setLicensePlate(workItem.getOrder().getVehicle().getLicensePlate());
        return response;
    }
}
