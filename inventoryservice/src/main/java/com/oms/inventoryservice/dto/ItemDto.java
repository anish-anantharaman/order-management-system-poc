package com.oms.inventoryservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ItemDto(
        @NotBlank
        String sku,

        @NotNull
        Integer quantity
) { }