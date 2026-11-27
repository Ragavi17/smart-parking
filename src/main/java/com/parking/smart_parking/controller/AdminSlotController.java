package com.parking.smart_parking.controller;

import com.parking.smart_parking.dto.*;
import com.parking.smart_parking.service.SlotService;
import com.parking.smart_parking.service.SlotTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminSlotController {

    private final SlotTypeService slotTypeService;
    private final SlotService slotService;

    @PostMapping("/slot-types")
    public ResponseEntity<SlotTypeResponse> createSlotType(@Valid @RequestBody SlotTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(slotTypeService.create(request));
    }

    @PostMapping("/slots")
    public ResponseEntity<SlotResponse> createSlot(@Valid @RequestBody SlotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(slotService.create(request));
    }

    @PatchMapping("/slots/{id}/status")
    public SlotResponse updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody SlotStatusRequest request) {
        return slotService.updateStatus(id, request);
    }
}