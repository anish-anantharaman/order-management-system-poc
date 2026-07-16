package com.oms.orderservice.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderResponseDto(
        UUID id,
        UUID customerId,
        String status,
        BigDecimal totalAmount,
        List<OrderItemDto> items
) { }