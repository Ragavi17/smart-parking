package com.parking.smart_parking.service;

import com.parking.smart_parking.dto.SlotTypeRequest;
import com.parking.smart_parking.dto.SlotTypeResponse;
import com.parking.smart_parking.entity.SlotType;
import com.parking.smart_parking.repository.SlotTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SlotTypeService {

    private final SlotTypeRepository slotTypeRepository;

    @Transactional
    public SlotTypeResponse create(SlotTypeRequest request) {
        String name = request.name().trim().toUpperCase();
        if (slotTypeRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slot type already exists");
        }
        SlotType type = new SlotType();
        type.setName(name);
        type.setPricePerHour(request.pricePerHour());
        return toResponse(slotTypeRepository.save(type));
    }

    @Transactional(readOnly = true)
    public List<SlotTypeResponse> findAll() {
        return slotTypeRepository.findAll().stream().map(this::toResponse).toList();
    }

    private SlotTypeResponse toResponse(SlotType t) {
        return new SlotTypeResponse(t.getId(), t.getName(), t.getPricePerHour());
    }
}