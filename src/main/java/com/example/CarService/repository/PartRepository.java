package com.example.CarService.repository;

import com.example.CarService.entity.Part;
import com.example.CarService.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartRepository extends JpaRepository<Part, Long> {
    List<Part> findAllByDeletedAtIsNull();
}
