package com.shopstream.inventory.stock;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    /**
     * SELECT ... FOR UPDATE: locks the rows until the transaction ends, so two
     * orders for the last unit cannot both see "1 available" and both reserve it.
     *
     * Rows are always locked in productId order. If two transactions locked the
     * same rows in different orders they could wait for each other forever (a deadlock).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventoryItem i where i.productId in :ids order by i.productId")
    List<InventoryItem> findAllForUpdate(@Param("ids") Collection<Long> ids);

    List<InventoryItem> findAllByOrderByProductIdAsc();
}
