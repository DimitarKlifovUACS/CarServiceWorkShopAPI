package com.example.CarService.repository;

import com.example.CarService.entity.Part;
import com.example.CarService.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartRepository extends JpaRepository<Part, Long> {
    // Like findById, but ignores soft-deleted records (deletedAt is set)
    Optional<Part> findByIdAndDeletedAtIsNull(Long id);
    List<Part> findAllByDeletedAtIsNull();
}
