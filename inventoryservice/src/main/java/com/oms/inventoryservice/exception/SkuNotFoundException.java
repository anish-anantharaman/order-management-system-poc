package com.oms.inventoryservice.exception;

public class SkuNotFoundException extends RuntimeException {

    public SkuNotFoundException(String message) {
        super(message);
    }
}
