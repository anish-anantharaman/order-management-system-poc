package com.oms.orderservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

@Schema(description = "Request payload for creating a new order")
public record CreateOrderRequestDto(
  @Schema(description = "Unique identifier of the customer placing the order",
          example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull
  UUID customerId,

  @Schema(description = "List of items included in the order", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotEmpty
  List<@Valid OrderItemDto> items
) { }