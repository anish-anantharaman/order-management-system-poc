package com.oms.orderservice.controller;

import com.oms.orderservice.dto.CreateOrderRequestDto;
import com.oms.orderservice.dto.CreateOrderResponseDto;
import com.oms.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/v1")
public class OrderController {

    private final OrderService orderService;

    @PostMapping(path = "/orders", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> createOrder(@Valid @RequestBody CreateOrderRequestDto orderRequestDto) {
        CreateOrderResponseDto orderResponseDto = orderService.createOrder(orderRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderResponseDto);
    }
}