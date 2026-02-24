package com.example.demo.dto;

import com.example.demo.entity.Product;

public record ProductResponse(
        Long id,
        String name,
        Double price,
        String category
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getCategory()
        );
    }
}
