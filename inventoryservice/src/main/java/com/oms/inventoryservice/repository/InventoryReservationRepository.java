package com.oms.inventoryservice.repository;

import com.oms.inventoryservice.entity.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, UUID> {

    List<InventoryReservation> findByOrderId(UUID orderId);
}
