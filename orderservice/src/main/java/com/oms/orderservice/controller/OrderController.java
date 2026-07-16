package com.oms.orderservice.controller;


import com.oms.orderservice.dto.*;
import com.oms.orderservice.service.OrderService;
import com.oms.orderservice.util.SwaggerConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Orders", description = "APIs for managing orders")
public class OrderController {

    private final OrderService orderService;

    @Operation(
            summary = "Create new order",
            description = "Creates a new order with status set as CREATED. " +
                    "Returns 201 if successful, 400 if validation fails, " +
                    "and 500 if the server fails to process the request"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(name = "Order created", value = SwaggerConstants.CREATE_ORDER_RESPONSE_EXAMPLE))),
            @ApiResponse(responseCode = "400", description = "Validation failed for the request",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "Validation error", value = SwaggerConstants.CREATE_ORDER_VALIDATION_ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "500", description = "Server failed to process the request",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "Server error", value = SwaggerConstants.CREATE_ORDER_SERVER_ERROR_EXAMPLE)))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CreateOrderRequestDto.class),
                    examples = @ExampleObject(name = "Create order request", value = SwaggerConstants.CREATE_ORDER_REQUEST_EXAMPLE))
    )
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

    @Operation(
            summary = "Get order by id",
            description = "Fetches the order details for the given order id. " +
                    "Returns 200 if found, 404 if no order exists with the given id, " +
                    "and 500 if the server fails to process the request"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order fetched successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(name = "Order fetched", value = SwaggerConstants.GET_ORDER_RESPONSE_EXAMPLE))),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "Order not found", value = SwaggerConstants.GET_ORDER_NOT_FOUND_EXAMPLE))),
            @ApiResponse(responseCode = "500", description = "Server failed to process the request",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "Server error", value = SwaggerConstants.GET_ORDER_SERVER_ERROR_EXAMPLE)))
    })
    @GetMapping(path = "/orders/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getOrderById(@Parameter(description = "Unique identifier of the order",
            example = "9c858901-8a57-4791-81fe-4c455b099bc9") @PathVariable UUID id) {
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