package com.oms.orderservice.service.impl;

import com.oms.orderservice.dto.CreateOrderRequestDto;
import com.oms.orderservice.dto.CreateOrderResponseDto;
import com.oms.orderservice.entity.Order;
import com.oms.orderservice.entity.OrderItem;
import com.oms.orderservice.entity.OrderStatus;
import com.oms.orderservice.mapper.OrderMapper;
import com.oms.orderservice.repository.OrderItemRepository;
import com.oms.orderservice.repository.OrderRepository;
import com.oms.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
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
        order.setStatus(String.valueOf(OrderStatus.PENDING));
        Order savedOrder = orderRepository.save(order);

        List<OrderItem> items = orderMapper.toOrderItems(orderRequestDto.items());
        items.forEach(item -> item.setOrder(savedOrder));
        orderItemRepository.saveAll(items);
        return new CreateOrderResponseDto(savedOrder.getId(),
                savedOrder.getStatus(), savedOrder.getTotalAmount());
    }
}