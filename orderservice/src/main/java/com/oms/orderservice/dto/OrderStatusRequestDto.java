package com.oms.orderservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to update the status of an order")
public record OrderStatusRequestDto(
        @Schema(description = "New status to set for the order", example = "CONFIRMED")
        @NotBlank
        String status
) { }
