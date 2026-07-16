package com.oms.orderservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "A single line item within an order")
public record OrderItemDto(
  @Schema(description = "Stock keeping unit identifying the item", example = "SKU-1001",
          requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank
  String sku,

  @Schema(description = "Number of units ordered", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull
  Integer quantity,

  @Schema(description = "Price per unit", example = "250.00", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull
  BigDecimal unitPrice
) { }