package com.oms.orderservice.service;

import com.oms.orderservice.dto.*;

import java.util.UUID;

public interface OrderService {

    CreateOrderResponseDto createOrder(CreateOrderRequestDto orderRequestDto);

    OrderResponseDto getOrderById(UUID id);

    PagedResponseDto<OrderSummaryDto> fetchOrders(UUID customerId, String status, int page, int size);

    OrderStatusResponseDto updateOrderStatus(UUID id, OrderStatusRequestDto statusRequestDto);

    OrderStatusResponseDto cancelOrder(UUID id, CancelOrderRequestDto cancelOrderRequestDto);
}