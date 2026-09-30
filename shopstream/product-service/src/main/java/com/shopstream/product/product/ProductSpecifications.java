package com.shopstream.product.product;

import org.springframework.data.jpa.domain.Specification;

/**
 * Small reusable WHERE-clause pieces built with the JPA Criteria API.
 * ProductService combines only the ones it needs, e.g.
 * active AND name LIKE '%lamp%' AND category = 'Home & Kitchen'.
 * That avoids one query method per combination of filters.
 */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> isActive() {
        return (root, query, cb) -> cb.isTrue(root.<Boolean>get("active"));
    }

    public static Specification<Product> nameOrDescriptionContains(String text) {
        String pattern = "%" + text.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.<String>get("name")), pattern),
                cb.like(cb.lower(root.<String>get("description")), pattern));
    }

    public static Specification<Product> hasCategory(String category) {
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }
}
