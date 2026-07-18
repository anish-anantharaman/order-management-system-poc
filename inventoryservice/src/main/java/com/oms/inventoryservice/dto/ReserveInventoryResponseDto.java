package com.oms.inventoryservice.dto;

import java.util.List;
import java.util.UUID;

public record ReserveInventoryResponseDto(
        UUID orderId,
        String status,
        List<ItemDto> reservations
) {
}
