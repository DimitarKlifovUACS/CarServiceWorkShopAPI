package com.example.CarService.repository;

import com.example.CarService.entity.Vehicle;
import com.example.CarService.entity.WorkItem;
import org.hibernate.jdbc.Work;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkItemRepository extends JpaRepository<WorkItem, Long> {
    List<WorkItem> findAllByDeletedAtIsNull();
    List<WorkItem> findAllByOrderIdAndDeletedAtIsNull(Long orderId);

}
