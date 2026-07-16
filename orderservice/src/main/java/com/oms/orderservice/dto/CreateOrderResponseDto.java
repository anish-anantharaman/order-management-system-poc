package com.oms.orderservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateOrderResponseDto(
    UUID id,
    String status,
    BigDecimal totalAmount
) { }