package com.oms.orderservice.controller;


import com.oms.orderservice.dto.*;
import com.oms.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/v1")
@Validated
public class OrderController {

    private final OrderService orderService;

    @PostMapping(path = "/orders", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> createOrder(@Valid @RequestBody CreateOrderRequestDto orderRequestDto) {
        CreateOrderResponseDto orderResponseDto = orderService.createOrder(orderRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponseDto(
                        HttpStatus.CREATED.value(),
                        HttpStatus.CREATED.getReasonPhrase(),
                        "Order created successfully",
                        orderResponseDto
                ));
    }

    @GetMapping(path = "/orders/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getOrderById(@PathVariable UUID id) {
        OrderResponseDto orderResponseDto = orderService.getOrderById(id);
        return ResponseEntity.ok()
                .body(new ApiResponseDto(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        "Order fetched successfully",
                        orderResponseDto
                ));
    }

    @GetMapping(path = "/orders", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> fetchOrders(@RequestParam(required = false) UUID customerId,
               @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page,
               @RequestParam(defaultValue = "20") int size) {

        PagedResponseDto<OrderSummaryDto> orders = orderService.fetchOrders(customerId, status, page, size);
        return ResponseEntity.ok()
                .body(new ApiResponseDto(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        "Orders fetched successfully",
                        orders
                ));
    }

    @PatchMapping(path="/orders/{id}/status", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> updateOrderStatus(@PathVariable UUID id,
                                                      @Valid @RequestBody OrderStatusRequestDto statusRequestDto) {
        OrderStatusResponseDto orderStatusResponseDto = orderService.updateOrderStatus(id, statusRequestDto);
        return ResponseEntity.ok()
                .body(new ApiResponseDto(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        "Order status updated successfully",
                        orderStatusResponseDto
                ));
    }

    @PatchMapping(path = "/orders/{id}/cancel", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> cancelOrder(@PathVariable UUID id,
                                              @Valid @RequestBody CancelOrderRequestDto cancelOrderRequestDto) {
        OrderStatusResponseDto orderStatusResponseDto = orderService.cancelOrder(id, cancelOrderRequestDto);
        return ResponseEntity.ok()
                .body(new ApiResponseDto(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        "Order cancellation request submitted",
                        orderStatusResponseDto
                ));
    }
}