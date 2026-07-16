package com.oms.orderservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemDto(
  @NotBlank
  String sku,

  @NotNull
  Integer quantity,

  @NotNull
  BigDecimal unitPrice
) { }