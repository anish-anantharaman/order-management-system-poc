package com.oms.orderservice.service.impl;

import com.oms.orderservice.dto.*;
import com.oms.orderservice.entity.Order;
import com.oms.orderservice.entity.OrderItem;
import com.oms.orderservice.entity.OrderStatus;
import com.oms.orderservice.exception.InvalidOrderStatusException;
import com.oms.orderservice.exception.InvalidOrderTransitionException;
import com.oms.orderservice.exception.OrderNotFoundException;
import com.oms.orderservice.mapper.OrderMapper;
import com.oms.orderservice.repository.OrderItemRepository;
import com.oms.orderservice.repository.OrderRepository;
import com.oms.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public CreateOrderResponseDto createOrder(CreateOrderRequestDto orderRequestDto) {
        Order order = orderMapper.toOrder(orderRequestDto);
        BigDecimal total = orderRequestDto.items()
                .stream()
                .map(item -> item.unitPrice()
                        .multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(total);
        order.setStatus(String.valueOf(OrderStatus.CREATED));
        Order savedOrder = orderRepository.save(order);

        List<OrderItem> items = orderMapper.toOrderItems(orderRequestDto.items());
        items.forEach(item -> item.setOrder(savedOrder));
        orderItemRepository.saveAll(items);
        return new CreateOrderResponseDto(savedOrder.getId(),
                savedOrder.getStatus(), savedOrder.getTotalAmount());
    }

    @Override
    public OrderResponseDto getOrderById(UUID id) {
        Order order = findOrderOrThrow(id);

        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        return new OrderResponseDto(order.getId(), order.getCustomerId(), order.getStatus(),
                order.getTotalAmount(), orderMapper.toOrderItemDtos(items));
    }

    @Override
    public PagedResponseDto<OrderSummaryDto> fetchOrders(UUID customerId, String status, int page, int size) {
        Page<Order> orders = orderRepository.findByCustomerIdAndStatus(customerId, status, PageRequest.of(page, size));
        return new PagedResponseDto<>(
                orderMapper.toOrderSummaryDtos(orders.getContent()),
                orders.getNumber(),
                orders.getSize(),
                orders.getTotalElements(),
                orders.getTotalPages()
        );
    }

    @Override
    @Transactional
    public OrderStatusResponseDto updateOrderStatus(UUID id, OrderStatusRequestDto statusRequestDto) {
        Order order = findOrderOrThrow(id);

        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(statusRequestDto.status().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidOrderStatusException("Invalid order status: " + statusRequestDto.status());
        }

        OrderStatus currentStatus = OrderStatus.valueOf(order.getStatus());
        validateStatusTransition(currentStatus, newStatus);

        order.setStatus(newStatus.name());
        Order updatedOrder = orderRepository.save(order);
        return new OrderStatusResponseDto(updatedOrder.getId(), updatedOrder.getStatus());
    }

    @Override
    @Transactional
    public OrderStatusResponseDto cancelOrder(UUID id, CancelOrderRequestDto cancelOrderRequestDto) {
        Order order = findOrderOrThrow(id);
        OrderStatus newStatus = OrderStatus.CANCEL_REQUESTED;
        OrderStatus currentStatus = OrderStatus.valueOf(order.getStatus());

        validateCancellable(currentStatus);

        /**
         *         implement logic to check if shipment is completed, i.e. if the product is `DELIVERED`.
         *         If Yes, don't process further.
        */

        order.setStatus(newStatus.name());
        order.setCancellationReason(cancelOrderRequestDto.reason());
        Order updatedOrder = orderRepository.save(order);
        return new OrderStatusResponseDto(updatedOrder.getId(), updatedOrder.getStatus());
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (newStatus.ordinal() != currentStatus.ordinal() + 1) {
            log.warn("Cannot transition from {} to {}", currentStatus, newStatus);
            throw new InvalidOrderTransitionException(
                    "Cannot transition order status from " + currentStatus + " to " + newStatus);
        }
    }

    private void validateCancellable(OrderStatus currentStatus) {
        if (currentStatus.ordinal() >= OrderStatus.CANCEL_REQUESTED.ordinal()) {
            log.warn("Cannot cancel order in status {}", currentStatus);
            throw new InvalidOrderTransitionException(
                    "Cannot cancel order in status " + currentStatus);
        }
    }

    private Order findOrderOrThrow(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Order not found for id={}", id);
                    return new OrderNotFoundException("Order not found for id=" + id);
                });
    }
}