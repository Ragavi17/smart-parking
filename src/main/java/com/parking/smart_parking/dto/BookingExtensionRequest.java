package com.parking.smart_parking.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record BookingExtensionRequest(@NotNull LocalDateTime newEndTime) {}