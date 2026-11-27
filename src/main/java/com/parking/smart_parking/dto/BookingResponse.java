package com.parking.smart_parking.dto;

import com.parking.smart_parking.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingResponse(
        Long id, Long slotId, String slotNumber, String slotTypeName,
        LocalDateTime startTime, LocalDateTime endTime,
        BigDecimal pricePerHour, BigDecimal totalAmount,
        BookingStatus status, LocalDateTime createdAt
) {}