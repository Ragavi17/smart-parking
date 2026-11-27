package com.parking.smart_parking.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record SlotTypeRequest(
        @NotBlank @Size(max = 30) String name,
        @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal pricePerHour
) {}