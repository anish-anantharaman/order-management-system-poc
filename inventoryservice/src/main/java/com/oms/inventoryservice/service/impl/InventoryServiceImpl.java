package com.oms.inventoryservice.service.impl;

import com.oms.inventoryservice.dto.ItemDto;
import com.oms.inventoryservice.dto.ReserveInventoryRequestDto;
import com.oms.inventoryservice.dto.ReserveInventoryResponseDto;
import com.oms.inventoryservice.entity.InventoryItem;
import com.oms.inventoryservice.entity.InventoryReservation;
import com.oms.inventoryservice.exception.InsufficientInventoryException;
import com.oms.inventoryservice.exception.SkuNotFoundException;
import com.oms.inventoryservice.repository.InventoryRepository;
import com.oms.inventoryservice.repository.InventoryReservationRepository;
import com.oms.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private static final String RESERVED_STATUS = "RESERVED";

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository inventoryReservationRepository;

    @Override
    @Transactional
    public ReserveInventoryResponseDto reserveInventory(ReserveInventoryRequestDto reserveInventoryRequestDto) {
        Map<String, InventoryItem> inventoryBySku = lockInventoryItems(reserveInventoryRequestDto.items());
        List<InventoryReservation> reservations = applyReservations(reserveInventoryRequestDto, inventoryBySku);

        inventoryRepository.saveAll(inventoryBySku.values());
        inventoryReservationRepository.saveAll(reservations);

        return new ReserveInventoryResponseDto(
                reserveInventoryRequestDto.orderId(),
                RESERVED_STATUS,
                reserveInventoryRequestDto.items()
        );
    }

    private Map<String, InventoryItem> lockInventoryItems(List<ItemDto> items) {
        List<String> skus = items.stream()
                .map(ItemDto::sku)
                .toList();

        return inventoryRepository.findBySkuInForUpdate(skus).stream()
                .collect(Collectors.toMap(InventoryItem::getSku, Function.identity()));
    }

    private List<InventoryReservation> applyReservations(ReserveInventoryRequestDto reserveInventoryRequestDto,
                                                           Map<String, InventoryItem> inventoryBySku) {
        List<InventoryReservation> reservations = new ArrayList<>();
        for (ItemDto item : reserveInventoryRequestDto.items()) {
            InventoryItem inventoryItem = getInventoryItemOrThrow(inventoryBySku, item.sku());
            debitAvailableQuantity(inventoryItem, item.quantity());
            reservations.add(buildReservation(reserveInventoryRequestDto.orderId(), item));
        }
        return reservations;
    }

    private InventoryItem getInventoryItemOrThrow(Map<String, InventoryItem> inventoryBySku, String sku) {
        InventoryItem inventoryItem = inventoryBySku.get(sku);
        if (inventoryItem == null) {
            log.warn("SKU not found: {}", sku);
            throw new SkuNotFoundException("SKU not found: " + sku);
        }
        return inventoryItem;
    }

    private void debitAvailableQuantity(InventoryItem inventoryItem, int quantity) {
        if (inventoryItem.getAvailableQuantity() < quantity) {
            log.warn("Insufficient inventory for SKU {}: requested={}, available={}",
                    inventoryItem.getSku(), quantity, inventoryItem.getAvailableQuantity());
            throw new InsufficientInventoryException(
                    String.format(
                            "Insufficient inventory for SKU %s: requested=%d, available=%d",
                            inventoryItem.getSku(), quantity, inventoryItem.getAvailableQuantity()
                    )
            );
        }

        inventoryItem.setAvailableQuantity(inventoryItem.getAvailableQuantity() - quantity);
        inventoryItem.setReservedQuantity(inventoryItem.getReservedQuantity() + quantity);
    }

    private InventoryReservation buildReservation(UUID orderId, ItemDto item) {
        InventoryReservation reservation = new InventoryReservation();
        reservation.setOrderId(orderId);
        reservation.setSku(item.sku());
        reservation.setQuantity(item.quantity());
        reservation.setStatus(RESERVED_STATUS);
        return reservation;
    }
}
