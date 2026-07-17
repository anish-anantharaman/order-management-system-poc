package com.oms.orderservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to cancel an order")
public record CancelOrderRequestDto(
        @Schema(description = "Reason for cancelling the order", example = "Customer requested cancellation")
        String reason
) { }
