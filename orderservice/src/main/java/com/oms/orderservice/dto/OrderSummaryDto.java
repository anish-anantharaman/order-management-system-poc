package com.oms.orderservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Summary information about an order")
public record OrderSummaryDto(
        @Schema(description = "Unique identifier of the order", example = "9c858901-8a57-4791-81fe-4c455b099bc9")
        UUID id,

        @Schema(description = "Unique identifier of the customer who placed the order",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID customerId,

        @Schema(description = "Current status of the order", example = "CREATED")
        String status,

        @Schema(description = "Total amount for the order", example = "1499.99")
        BigDecimal totalAmount
) { }
