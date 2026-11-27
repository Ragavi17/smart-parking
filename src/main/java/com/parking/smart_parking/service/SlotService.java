package com.parking.smart_parking.service;

import com.parking.smart_parking.dto.SlotRequest;
import com.parking.smart_parking.dto.SlotResponse;
import com.parking.smart_parking.dto.SlotStatusRequest;
import com.parking.smart_parking.entity.Slot;
import com.parking.smart_parking.entity.SlotType;
import com.parking.smart_parking.repository.SlotRepository;
import com.parking.smart_parking.repository.SlotTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SlotService {

    private final SlotRepository slotRepository;
    private final SlotTypeRepository slotTypeRepository;

    @Transactional
    public SlotResponse create(SlotRequest request) {
        String number = request.slotNumber().trim().toUpperCase();
        if (slotRepository.existsBySlotNumberIgnoreCase(number)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slot number already exists");
        }
        SlotType type = slotTypeRepository.findById(request.slotTypeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot type not found"));
        Slot slot = new Slot();
        slot.setSlotNumber(number);
        slot.setSlotType(type);
        return toResponse(slotRepository.save(slot));
    }

    @Transactional
    public SlotResponse updateStatus(Long id, SlotStatusRequest request) {
        Slot slot = slotRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found"));
        slot.setActive(request.active());
        return toResponse(slot);
    }

    @Transactional(readOnly = true)
    public List<SlotResponse> findAll() {
        return slotRepository.findAllWithType().stream().map(this::toResponse).toList();
    }

    private SlotResponse toResponse(Slot s) {
        SlotType t = s.getSlotType();
        return new SlotResponse(s.getId(), s.getSlotNumber(), s.isActive(),
                t.getId(), t.getName(), t.getPricePerHour());
    }
}