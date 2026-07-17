package com.oms.orderservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Paginated response wrapper")
public record PagedResponseDto<T>(
        @Schema(description = "List of items in the current page")
        List<T> content,

        @Schema(description = "Current page number (0-indexed)", example = "0")
        int page,

        @Schema(description = "Number of items per page", example = "20")
        int size,

        @Schema(description = "Total number of elements across all pages", example = "1")
        long totalElements,

        @Schema(description = "Total number of pages", example = "1")
        int totalPages
) { }
