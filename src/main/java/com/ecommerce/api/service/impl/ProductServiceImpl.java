package com.ecommerce.api.service.impl;

import com.ecommerce.api.exception.ResourceNotFoundException;
import com.ecommerce.api.model.Product;
import com.ecommerce.api.repository.ProductRepository;
import com.ecommerce.api.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public List<Product> getProducts(String category, String search) {
        boolean hasCategory = StringUtils.hasText(category);
        boolean hasSearch = StringUtils.hasText(search);

        if (hasCategory && hasSearch) {
            return productRepository.findByCategoryAndKeyword(category, search);
        } else if (hasCategory) {
            return productRepository.findByCategory(category);
        } else if (hasSearch) {
            return productRepository.searchByKeyword(search);
        } else {
            return productRepository.findAll();
        }
    }

    @Override
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }

    @Override
    @Transactional
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    @Override
    @Transactional
    public Product updateProduct(Long id, Product updatedProduct) {
        Product existing = getProductById(id);
        existing.setName(updatedProduct.getName());
        existing.setDescription(updatedProduct.getDescription());
        existing.setPrice(updatedProduct.getPrice());
        existing.setCategory(updatedProduct.getCategory());
        existing.setStockQuantity(updatedProduct.getStockQuantity());
        return productRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        productRepository.delete(product);
    }
}
