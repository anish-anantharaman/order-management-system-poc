package com.oms.inventoryservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record ReserveInventoryRequestDto(
        @NotBlank
        UUID orderId,

        @NotEmpty
        List<@NotBlank ItemDto> items
) { }