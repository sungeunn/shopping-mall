package com.shoppingmall.domain.product.repository;

import com.shoppingmall.domain.product.entity.Product;
import com.shoppingmall.domain.product.entity.ProductStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByStatusAndCategoryContaining(ProductStatus status, String category, Pageable pageable);

    Page<Product> findByStatusAndNameContaining(ProductStatus status, String keyword, Pageable pageable);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    /**
     * 재고 차감 시 비관적 락으로 동시성 처리
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithPessimisticLock(@Param("id") Long id);
}
