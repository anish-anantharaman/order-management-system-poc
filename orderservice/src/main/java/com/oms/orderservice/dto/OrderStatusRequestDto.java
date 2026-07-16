package com.oms.orderservice.dto;

import jakarta.validation.constraints.NotBlank;

public record OrderStatusRequestDto(
        @NotBlank
        String status
) { }