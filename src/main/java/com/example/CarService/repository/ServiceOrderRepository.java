package com.example.CarService.repository;

import com.example.CarService.entity.ServiceOrder;
import com.example.CarService.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ServiceOrderRepository extends JpaRepository<ServiceOrder,Long> {
    List<ServiceOrder> findAllByDeletedAtIsNull();
    List<ServiceOrder> findAllByVehicleIdAndDeletedAtIsNull(Long vehicleId);

    @Query("SELECT MONTH(o.completedAt), YEAR(o.completedAt), SUM(p.quantity * p.unitPrice) " +
            "FROM ServiceOrder o JOIN Part p ON p.order = o " +
            "WHERE o.status = 'COMPLETED' " +
            "GROUP BY YEAR(o.completedAt), MONTH(o.completedAt)")
    List<Object[]> revenueByMonth();
}
