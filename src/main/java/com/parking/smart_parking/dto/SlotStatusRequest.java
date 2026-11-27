package com.parking.smart_parking.dto;

import jakarta.validation.constraints.NotNull;

public record SlotStatusRequest(@NotNull Boolean active) {}