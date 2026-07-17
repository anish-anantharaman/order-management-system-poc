package com.oms.orderservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Response returned after updating or cancelling an order's status")
public record OrderStatusResponseDto(
        @Schema(description = "Unique identifier of the order", example = "9c858901-8a57-4791-81fe-4c455b099bc9")
        UUID id,

        @Schema(description = "Current status of the order", example = "CONFIRMED")
        String status
) { }
