package com.shoppingmall.domain.order.repository;

import com.shoppingmall.domain.order.entity.Order;
import com.shoppingmall.domain.order.dto.ProductSalesResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"orderItems", "orderItems.product"})
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);

    Page<Order> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :startOfDay AND o.status != 'CANCELLED'")
    long countTodayOrders(@Param("startOfDay") LocalDateTime startOfDay);

    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM Order o WHERE o.status != 'CANCELLED'")
    long sumTotalRevenue();

    @Query("""
            SELECT new com.shoppingmall.domain.order.dto.ProductSalesResponse(
                oi.product.id,
                oi.product.name,
                SUM(oi.quantity),
                SUM(CAST(oi.quantity AS long) * oi.orderPrice)
            )
            FROM OrderItem oi
            JOIN oi.order o
            WHERE o.status != 'CANCELLED'
            GROUP BY oi.product.id, oi.product.name
            ORDER BY SUM(oi.quantity) DESC
            """)
    List<ProductSalesResponse> findProductSales();
}
