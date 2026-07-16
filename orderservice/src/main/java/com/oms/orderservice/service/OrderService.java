package com.oms.orderservice.service;

import com.oms.orderservice.dto.CreateOrderRequestDto;
import com.oms.orderservice.dto.CreateOrderResponseDto;

public interface OrderService {

    CreateOrderResponseDto createOrder(CreateOrderRequestDto orderRequestDto);
}