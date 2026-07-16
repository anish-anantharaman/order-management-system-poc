package com.oms.orderservice.util;

public class SwaggerConstants {

    private SwaggerConstants() { }

    public static final String CREATE_ORDER_REQUEST_EXAMPLE = """
            {
              "customerId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              "items": [
                {
                  "sku": "SKU-1001",
                  "quantity": 2,
                  "unitPrice": 250.00
                },
                {
                  "sku": "SKU-1002",
                  "quantity": 1,
                  "unitPrice": 999.99
                }
              ]
            }
            """;

    public static final String CREATE_ORDER_RESPONSE_EXAMPLE = """
            {
              "statusCode": 201,
              "statusMessage": "Created",
              "message": "Order created successfully",
              "response": {
                "id": "9c858901-8a57-4791-81fe-4c455b099bc9",
                "status": "CREATED",
                "totalAmount": 1499.99
              }
            }
            """;

    public static final String CREATE_ORDER_VALIDATION_ERROR_EXAMPLE = """
            {
              "statusCode": 400,
              "statusMessage": "Bad Request",
              "message": "Validation failed",
              "response": {
                "customerId": "must not be null",
                "items": "must not be empty"
              }
            }
            """;

    public static final String CREATE_ORDER_SERVER_ERROR_EXAMPLE = """
            {
              "statusCode": 500,
              "statusMessage": "Internal Server Error",
              "message": "Failed to process the request",
              "response": false
            }
            """;

    public static final String GET_ORDER_RESPONSE_EXAMPLE = """
            {
              "statusCode": 200,
              "statusMessage": "OK",
              "message": "Order fetched successfully",
              "response": {
                "id": "9c858901-8a57-4791-81fe-4c455b099bc9",
                "customerId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                "status": "CREATED",
                "totalAmount": 1499.99,
                "items": [
                  {
                    "sku": "SKU-1001",
                    "quantity": 2,
                    "unitPrice": 250.00
                  },
                  {
                    "sku": "SKU-1002",
                    "quantity": 1,
                    "unitPrice": 999.99
                  }
                ]
              }
            }
            """;

    public static final String GET_ORDER_NOT_FOUND_EXAMPLE = """
            {
              "statusCode": 404,
              "statusMessage": "Not Found",
              "message": "Order not found with id: 9c858901-8a57-4791-81fe-4c455b099bc9",
              "response": false
            }
            """;

    public static final String GET_ORDER_SERVER_ERROR_EXAMPLE = """
            {
              "statusCode": 500,
              "statusMessage": "Internal Server Error",
              "message": "An unexpected error occurred while processing the request",
              "response": false
            }
            """;
}
