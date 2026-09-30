package com.shopstream.product.product;

import com.shopstream.common.events.ProductCreatedEvent;
import com.shopstream.product.web.ApiException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;

@Service
public class ProductService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ProductService(ProductRepository productRepository, ApplicationEventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(String search, String category, int page, int size) {
        Specification<Product> spec = ProductSpecifications.isActive();
        if (StringUtils.hasText(search)) {
            spec = spec.and(ProductSpecifications.nameOrDescriptionContains(search.trim()));
        }
        if (StringUtils.hasText(category)) {
            spec = spec.and(ProductSpecifications.hasCategory(category.trim()));
        }
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by("name"));
        Page<ProductResponse> result = productRepository.findAll(spec, pageable).map(ProductResponse::from);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return ProductResponse.from(findOrThrow(id));
    }

    /** Used by order-service to look up current prices. Includes inactive products so it can reject them clearly. */
    @Transactional(readOnly = true)
    public List<ProductResponse> getByIds(Collection<Long> ids) {
        return productRepository.findAllById(ids).stream().map(ProductResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<String> categories() {
        return productRepository.findActiveCategories();
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new ApiException(HttpStatus.CONFLICT, "A product with SKU " + request.sku() + " already exists");
        }
        Product product = productRepository.save(new Product(request.sku(), request.name(), request.description(),
                request.category(), request.price(), request.imageUrl()));

        int initialStock = request.initialStock() == null ? 0 : request.initialStock();
        // This is an in-process Spring event, not a Kafka message yet.
        // ProductEventPublisher forwards it to Kafka only AFTER the transaction commits,
        // so we never announce a product that was rolled back.
        eventPublisher.publishEvent(ProductCreatedEvent.of(product.getId(), product.getSku(), product.getName(), initialStock));
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findOrThrow(id);
        product.update(request.name(), request.description(), request.category(), request.price(), request.imageUrl());
        // No save() call needed: the entity is "managed", and JPA writes the changes at commit ("dirty checking").
        return ProductResponse.from(product);
    }

    @Transactional
    public void deactivate(Long id) {
        findOrThrow(id).deactivate();
    }

    private Product findOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product " + id + " not found"));
    }
}
