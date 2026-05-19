package com.example.CarService.repository;

import com.example.CarService.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Page<Vehicle> findAllByDeletedAtIsNull(Pageable pageable);
}
