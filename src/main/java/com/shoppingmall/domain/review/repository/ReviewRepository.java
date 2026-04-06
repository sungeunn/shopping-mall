package com.shoppingmall.domain.review.repository;

import com.shoppingmall.domain.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByOrderIdAndProductIdAndUserId(Long orderId, Long productId, Long userId);

    Page<Review> findByProductId(Long productId, Pageable pageable);
}
