package com.parking.smart_parking.repository;

import com.parking.smart_parking.entity.SlotType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SlotTypeRepository extends JpaRepository<SlotType, Long> {
    boolean existsByNameIgnoreCase(String name);
}