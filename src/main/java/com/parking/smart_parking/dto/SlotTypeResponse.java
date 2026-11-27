package com.parking.smart_parking.dto;

import java.math.BigDecimal;

public record SlotTypeResponse(Long id, String name, BigDecimal pricePerHour) {}