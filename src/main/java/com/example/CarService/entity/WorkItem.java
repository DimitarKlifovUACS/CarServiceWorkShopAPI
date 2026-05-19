package com.example.CarService.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class WorkItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;
    private double estimatedHours;
    private String mechanicName;
    private String status;
    private LocalDateTime deletedAt;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private ServiceOrder order;


    public WorkItem() {
    }

    public WorkItem(String description, double estimatedHours, String mechanicName, String status, LocalDateTime deletedAt, ServiceOrder order) {
        this.description = description;
        this.estimatedHours = estimatedHours;
        this.mechanicName = mechanicName;
        this.status = status;
        this.deletedAt = deletedAt;
        this.order = order;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getEstimatedHours() {
        return estimatedHours;
    }

    public void setEstimatedHours(double estimatedHours) {
        this.estimatedHours = estimatedHours;
    }

    public String getMechanicName() {
        return mechanicName;
    }

    public void setMechanicName(String mechanicName) {
        this.mechanicName = mechanicName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public ServiceOrder getOrder() {
        return order;
    }

    public void setOrder(ServiceOrder order) {
        this.order = order;
    }
}
