package com.oms.inventoryservice.dto;

public record ApiResponseDto(
        int statusCode,
        String statusMessage,
        String message,
        Object response
) { }