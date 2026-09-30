package com.shopstream.order.order;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * @EntityGraph loads the items in the same query (a JOIN). Without it, listing
     * 20 orders would run 1 query for the orders + 20 queries for their items:
     * the classic "N+1 query problem".
     */
    @EntityGraph(attributePaths = "items")
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = "items")
    Optional<Order> findWithItemsById(Long id);
}
