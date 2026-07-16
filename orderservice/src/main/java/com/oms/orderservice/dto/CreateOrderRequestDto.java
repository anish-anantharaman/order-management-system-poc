package com.oms.orderservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequestDto(
  @NotNull
  UUID customerId,

  @NotEmpty
  List<@Valid OrderItemDto> items
) { }