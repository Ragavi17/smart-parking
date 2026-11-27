package com.parking.smart_parking.dto;

import java.math.BigDecimal;

public record SlotResponse(Long id, String slotNumber, boolean active,
                           Long slotTypeId, String slotTypeName, BigDecimal pricePerHour) {}