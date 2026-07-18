package com.oms.inventoryservice.repository;

import com.oms.inventoryservice.entity.InventoryItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<InventoryItem, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventoryItem i where i.sku in :skus")
    List<InventoryItem> findBySkuInForUpdate(@Param("skus") List<String> skus);

    List<InventoryItem> findBySkuIn(List<String> skus);
}