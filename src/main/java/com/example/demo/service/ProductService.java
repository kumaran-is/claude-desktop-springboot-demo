package com.example.demo.service;

import com.example.demo.dto.CreateProductDto;
import com.example.demo.dto.ProductResponse;
import com.example.demo.entity.Product;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public Flux<ProductResponse> findAll() {
        return repository.findAll()
                .map(ProductResponse::from);
    }

    public Mono<ProductResponse> findById(Long id) {
        return repository.findById(id)
                .map(ProductResponse::from)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Product not found with id: " + id)));
    }

    public Mono<ProductResponse> create(CreateProductDto dto) {
        Product product = new Product(dto.name(), dto.price(), dto.category());
        return repository.save(product)
                .map(ProductResponse::from)
                .doOnSuccess(p -> log.info("Created product id={}", p.id()));
    }

    public Mono<ProductResponse> update(Long id, CreateProductDto dto) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Product not found with id: " + id)))
                .flatMap(existing -> {
                    existing.setName(dto.name());
                    existing.setPrice(dto.price());
                    existing.setCategory(dto.category());
                    return repository.save(existing);
                })
                .map(ProductResponse::from)
                .doOnSuccess(p -> log.info("Updated product id={}", p.id()));
    }

    public Flux<ProductResponse> findByCategory(String category) {
        return repository.findByCategory(category)
                .map(ProductResponse::from);
    }

    public Mono<Void> delete(Long id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Product not found with id: " + id)))
                .flatMap(repository::delete)
                .doOnSuccess(v -> log.info("Deleted product id={}", id));
    }
}
