package com.example.CarService.repository;

import com.example.CarService.entity.ServiceOrder;
import com.example.CarService.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ServiceOrderRepository extends JpaRepository<ServiceOrder,Long> {
    // Like findById, but ignores soft-deleted records (deletedAt is set)
    Optional<ServiceOrder> findByIdAndDeletedAtIsNull(Long id);
    List<ServiceOrder> findAllByDeletedAtIsNull();
    List<ServiceOrder> findAllByVehicleIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long vehicleId);

    // Orders are hidden when the order or its vehicle is deleted
    List<ServiceOrder> findAllByDeletedAtIsNullAndVehicleDeletedAtIsNullOrderByCreatedAtDesc();
    List<ServiceOrder> findAllByStatusAndDeletedAtIsNullAndVehicleDeletedAtIsNullOrderByCreatedAtDesc(String status);

    @Query("SELECT MONTH(o.completedAt), YEAR(o.completedAt), SUM(p.quantity * p.unitPrice) " +
            "FROM ServiceOrder o JOIN Part p ON p.order = o " +
            "WHERE o.status = 'COMPLETED' AND o.deletedAt IS NULL AND p.deletedAt IS NULL " +
            "GROUP BY YEAR(o.completedAt), MONTH(o.completedAt) " +
            "ORDER BY YEAR(o.completedAt), MONTH(o.completedAt)")
    List<Object[]> revenueByMonth();
}
