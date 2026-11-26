package com.example.CarService.repository;

import com.example.CarService.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    // Like findById, but ignores soft-deleted records (deletedAt is set)
    Optional<Vehicle> findByIdAndDeletedAtIsNull(Long id);
    Page<Vehicle> findAllByDeletedAtIsNull(Pageable pageable);
    boolean existsByLicensePlateAndDeletedAtIsNull(String licensePlate);

    // Active vehicles whose plate, owner, make or model contains the search text (case-insensitive)
    @Query("SELECT v FROM Vehicle v WHERE v.deletedAt IS NULL AND (" +
            "LOWER(v.licensePlate) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(v.ownerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(v.make) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(v.model) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Vehicle> searchActive(@Param("search") String search, Pageable pageable);
}
