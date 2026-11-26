package com.example.CarService.dto.request;

import com.example.CarService.entity.WorkItemStatus;
import jakarta.validation.constraints.NotNull;

// Body of PATCH /work-items/{id}/status, e.g. {"status": "IN_PROGRESS"}
public class WorkItemStatusRequest {

    @NotNull
    private WorkItemStatus status;

    public WorkItemStatus getStatus() {
        return status;
    }

    public void setStatus(WorkItemStatus status) {
        this.status = status;
    }
}
