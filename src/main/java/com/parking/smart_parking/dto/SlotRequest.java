package com.parking.smart_parking.dto;

import jakarta.validation.constraints.*;

public record SlotRequest(
        @NotBlank @Size(max = 20) String slotNumber,
        @NotNull Long slotTypeId
) {}