package com.ecommerce.api.controller;

import com.ecommerce.api.controller.dto.ProductRequest;
import com.ecommerce.api.model.Product;
import com.ecommerce.api.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * GET /api/products?category=Electronics&search=laptop
     * Retrieve all products with optional filtering by category and/or keyword search.
     */
    @GetMapping
    public ResponseEntity<List<Product>> getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(productService.getProducts(category, search));
    }

    /**
     * GET /api/products/{id}
     * Retrieve a single product by ID. Returns 404 if not found.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    /**
     * POST /api/products
     * Create a new product. Request body is fully validated via @Valid.
     */
    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody ProductRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(request.getCategory())
                .stockQuantity(request.getStockQuantity())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(product));
    }

    /**
     * PUT /api/products/{id}
     * Full update of an existing product. Returns 404 if not found.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        Product updatedProduct = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(request.getCategory())
                .stockQuantity(request.getStockQuantity())
                .build();
        return ResponseEntity.ok(productService.updateProduct(id, updatedProduct));
    }

    /**
     * DELETE /api/products/{id}
     * Delete a product. Returns 204 No Content on success.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
