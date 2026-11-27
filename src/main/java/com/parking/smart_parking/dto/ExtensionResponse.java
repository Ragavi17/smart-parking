package com.parking.smart_parking.dto;

import com.parking.smart_parking.enums.ExtensionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExtensionResponse(
        Long id, Long bookingId, LocalDateTime oldEndTime, LocalDateTime newEndTime,
        BigDecimal extraAmount, ExtensionStatus status, LocalDateTime createdAt
) {}