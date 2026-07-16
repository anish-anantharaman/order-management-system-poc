package com.oms.orderservice.dto;

import java.util.UUID;

public record OrderStatusResponseDto(
        UUID id,
        String status
) { }