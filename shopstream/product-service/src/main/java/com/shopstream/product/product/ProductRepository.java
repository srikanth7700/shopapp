package com.shopstream.product.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * JpaSpecificationExecutor adds findAll(Specification, Pageable), which lets us
 * build the WHERE clause dynamically (see ProductSpecifications).
 */
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    boolean existsBySku(String sku);

    @Query("select distinct p.category from Product p where p.active = true order by p.category")
    List<String> findActiveCategories();
}
