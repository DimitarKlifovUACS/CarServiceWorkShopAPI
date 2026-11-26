package com.example.CarService.repository;

import com.example.CarService.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    // Like findById, but ignores soft-deleted records (deletedAt is set)
    Optional<Vehicle> findByIdAndDeletedAtIsNull(Long id);
    Page<Vehicle> findAllByDeletedAtIsNull(Pageable pageable);
    boolean existsByLicensePlate(String licensePlate);
}
