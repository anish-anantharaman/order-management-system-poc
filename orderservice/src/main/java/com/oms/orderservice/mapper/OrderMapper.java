package com.oms.orderservice.mapper;

import com.oms.orderservice.dto.CreateOrderRequestDto;
import com.oms.orderservice.dto.OrderItemDto;
import com.oms.orderservice.entity.Order;
import com.oms.orderservice.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Order toOrder(CreateOrderRequestDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    OrderItem toOrderItem(OrderItemDto dto);

    List<OrderItem> toOrderItems(List<OrderItemDto> dto);
}