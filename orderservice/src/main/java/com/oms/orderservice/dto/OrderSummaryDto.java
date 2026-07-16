package com.oms.orderservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderSummaryDto(
        UUID id,
        UUID customerId,
        String status,
        BigDecimal totalAmount
) { }
