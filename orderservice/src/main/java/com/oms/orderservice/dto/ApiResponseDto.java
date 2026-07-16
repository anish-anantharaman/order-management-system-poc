package com.oms.orderservice.dto;

public record ApiResponseDto(
        int statusCode,
        String statusMessage,
        String message,
        Object response
) { }