package com.ecommerce.api.service;

import com.ecommerce.api.model.Product;

import java.util.List;

public interface ProductService {

    List<Product> getProducts(String category, String search);

    Product getProductById(Long id);

    Product createProduct(Product product);

    Product updateProduct(Long id, Product updatedProduct);

    void deleteProduct(Long id);
}
