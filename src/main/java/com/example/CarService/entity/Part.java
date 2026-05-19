package com.example.CarService.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Part {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private int quantity;
    private double unitPrice;
    private LocalDateTime deletedAt;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private ServiceOrder order;

    public Part() {
    }

    public Part(String name, int quantity, double unitPrice, LocalDateTime deletedAt, ServiceOrder order) {
        this.name = name;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.deletedAt = deletedAt;
        this.order = order;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
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
