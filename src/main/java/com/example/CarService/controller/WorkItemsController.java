package com.example.CarService.controller;

import com.example.CarService.dto.request.WorkItemRequest;
import com.example.CarService.dto.response.WorkItemResponse;
import com.example.CarService.service.WorkItemService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class WorkItemsController {

    private final WorkItemService workItemService;

    public WorkItemsController(WorkItemService workItemService) {
        this.workItemService = workItemService;
    }

    @PostMapping("orders/{orderId}/work-items")
    public ResponseEntity<WorkItemResponse> addWorkItem(@PathVariable Long orderId, @Valid @RequestBody WorkItemRequest request){
        return ResponseEntity.status(201).body(workItemService.addWorkItem(orderId, request));
    }

    @PatchMapping("/work-items/{id}/finish")
    public ResponseEntity<WorkItemResponse> finishWorkItem(@PathVariable Long id){
        return ResponseEntity.ok(workItemService.finishWorkItem(id));
    }


    @GetMapping("/orders/{orderId}/work-items")
    public ResponseEntity<List<WorkItemResponse>> getWorkItemsForOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(workItemService.getWorkItemsForOrder(orderId));
    }


    @DeleteMapping("/work-items/{id}")
    public ResponseEntity<Void> deleteWorkItem(@PathVariable Long id) {
        workItemService.deleteWorkItem(id);
        return ResponseEntity.noContent().build();
    }
}
