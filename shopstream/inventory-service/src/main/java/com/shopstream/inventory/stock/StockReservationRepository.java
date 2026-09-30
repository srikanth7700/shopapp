package com.shopstream.inventory.stock;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    Optional<StockReservation> findByOrderId(Long orderId);
}
