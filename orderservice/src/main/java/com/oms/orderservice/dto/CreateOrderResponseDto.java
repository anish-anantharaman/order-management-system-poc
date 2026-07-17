package com.oms.orderservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Response returned after successfully creating an order")
public record CreateOrderResponseDto(
    @Schema(description = "Unique identifier of the created order", example = "9c858901-8a57-4791-81fe-4c455b099bc9")
    UUID id,

    @Schema(description = "Current status of the order", example = "CREATED")
    String status,

    @Schema(description = "Total amount for the order", example = "1499.99")
    BigDecimal totalAmount
) { }
