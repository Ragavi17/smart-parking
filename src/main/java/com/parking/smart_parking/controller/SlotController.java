package com.parking.smart_parking.controller;

import com.parking.smart_parking.dto.SlotResponse;
import com.parking.smart_parking.dto.SlotTypeResponse;
import com.parking.smart_parking.service.SlotService;
import com.parking.smart_parking.service.SlotTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SlotController {

    private final SlotTypeService slotTypeService;
    private final SlotService slotService;

    @GetMapping("/slot-types")
    public List<SlotTypeResponse> slotTypes() {
        return slotTypeService.findAll();
    }

    @GetMapping("/slots")
    public List<SlotResponse> slots() {
        return slotService.findAll();
    }
}