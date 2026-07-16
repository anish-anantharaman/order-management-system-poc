package com.oms.orderservice.repository;

import com.oms.orderservice.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Query("SELECT o FROM Order o WHERE (:customerId IS NULL OR o.customerId = :customerId) "
            + "AND (:status IS NULL OR o.status = :status)")
    Page<Order> findByCustomerIdAndStatus(@Param("customerId") UUID customerId,
                                           @Param("status") String status,
                                           Pageable pageable);
}
