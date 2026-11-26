package com.example.CarService.repository;

import com.example.CarService.entity.Vehicle;
import com.example.CarService.entity.WorkItem;
import org.hibernate.jdbc.Work;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkItemRepository extends JpaRepository<WorkItem, Long> {
    // Like findById, but ignores soft-deleted records (deletedAt is set)
    Optional<WorkItem> findByIdAndDeletedAtIsNull(Long id);
    List<WorkItem> findAllByDeletedAtIsNull();
    List<WorkItem> findAllByOrderIdAndDeletedAtIsNull(Long orderId);

    // Work items for the board: hidden when the item, its order or its vehicle is deleted.
    // orderStatus is optional: null means orders with any status.
    @Query("SELECT w FROM WorkItem w " +
            "WHERE w.deletedAt IS NULL AND w.order.deletedAt IS NULL AND w.order.vehicle.deletedAt IS NULL " +
            "AND (:orderStatus IS NULL OR w.order.status = :orderStatus) " +
            "ORDER BY w.id")
    List<WorkItem> findActiveForBoard(@Param("orderStatus") String orderStatus);

}
