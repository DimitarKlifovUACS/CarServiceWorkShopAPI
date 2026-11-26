package com.example.CarService.repository;

import com.example.CarService.entity.Vehicle;
import com.example.CarService.entity.WorkItem;
import org.hibernate.jdbc.Work;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkItemRepository extends JpaRepository<WorkItem, Long> {
    // Like findById, but ignores soft-deleted records (deletedAt is set)
    Optional<WorkItem> findByIdAndDeletedAtIsNull(Long id);
    List<WorkItem> findAllByDeletedAtIsNull();
    List<WorkItem> findAllByOrderIdAndDeletedAtIsNull(Long orderId);

}
