package com.oms.inventoryservice.controller;

import com.oms.inventoryservice.dto.ApiResponseDto;
import com.oms.inventoryservice.dto.ReserveInventoryRequestDto;
import com.oms.inventoryservice.dto.ReserveInventoryResponseDto;
import com.oms.inventoryservice.service.InventoryService;
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
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping(path = "/inventory/reserve", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> reserveInventory(
            @RequestBody @Valid ReserveInventoryRequestDto reserveInventoryRequestDto) {
        ReserveInventoryResponseDto reserveInventoryResponseDto =
                inventoryService.reserveInventory(reserveInventoryRequestDto);
        return ResponseEntity.ok()
                .body(new ApiResponseDto(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        "Stock reserved successfully",
                        reserveInventoryResponseDto
                ));
    }
}