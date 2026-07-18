package com.oms.inventoryservice.service;

import com.oms.inventoryservice.dto.ReserveInventoryRequestDto;
import com.oms.inventoryservice.dto.ReserveInventoryResponseDto;

public interface InventoryService {

    ReserveInventoryResponseDto reserveInventory(ReserveInventoryRequestDto reserveInventoryRequestDto);
}