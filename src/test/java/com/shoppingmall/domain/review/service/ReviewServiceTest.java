package com.shoppingmall.domain.review.service;

import com.shoppingmall.domain.order.entity.Order;
import com.shoppingmall.domain.order.entity.OrderItem;
import com.shoppingmall.domain.order.entity.OrderStatus;
import com.shoppingmall.domain.order.repository.OrderRepository;
import com.shoppingmall.domain.product.entity.Product;
import com.shoppingmall.domain.review.dto.ReviewRequest;
import com.shoppingmall.domain.review.dto.ReviewResponse;
import com.shoppingmall.domain.review.entity.Review;
import com.shoppingmall.domain.review.repository.ReviewRepository;
import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.domain.user.entity.UserRole;
import com.shoppingmall.domain.user.repository.UserRepository;
import com.shoppingmall.global.exception.BusinessException;
import com.shoppingmall.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @InjectMocks
    private ReviewService reviewService;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    private User testUser;
    private Product testProduct;
    private Order completedOrder;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@test.com").password("pw").name("홍길동").role(UserRole.ROLE_USER).build();
        ReflectionTestUtils.setField(testUser, "id", 1L);

        testProduct = Product.builder()
                .name("노트북").description("설명").price(1_500_000).stock(10).category("전자기기").build();
        ReflectionTestUtils.setField(testProduct, "id", 1L);

        completedOrder = Order.builder()
                .user(testUser).receiverName("홍길동").receiverPhone("010-1234-5678").address("서울").build();
        ReflectionTestUtils.setField(completedOrder, "id", 1L);
        ReflectionTestUtils.setField(completedOrder, "status", OrderStatus.COMPLETED);

        OrderItem orderItem = OrderItem.builder()
                .order(completedOrder).product(testProduct).quantity(1).build();
        completedOrder.getOrderItems().add(orderItem);
    }

    @Test
    @DisplayName("리뷰 작성 성공 - 배송 완료 주문")
    void createReview_success() {
        // given
        ReviewRequest request = new ReviewRequest(1L, 1L, 5, "정말 좋아요!");
        given(orderRepository.findByIdWithItems(1L)).willReturn(Optional.of(completedOrder));
        given(reviewRepository.existsByOrderIdAndProductIdAndUserId(1L, 1L, 1L)).willReturn(false);
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(reviewRepository.save(any(Review.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        ReviewResponse response = reviewService.createReview(1L, request);

        // then
        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.content()).isEqualTo("정말 좋아요!");
        assertThat(response.userName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("리뷰 작성 실패 - COMPLETED 상태가 아닌 주문")
    void createReview_orderNotCompleted() {
        // given
        ReflectionTestUtils.setField(completedOrder, "status", OrderStatus.SHIPPED);
        ReviewRequest request = new ReviewRequest(1L, 1L, 5, "좋아요");
        given(orderRepository.findByIdWithItems(1L)).willReturn(Optional.of(completedOrder));

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.ORDER_NOT_COMPLETED.getMessage());
    }

    @Test
    @DisplayName("리뷰 작성 실패 - 주문에 없는 상품")
    void createReview_productNotInOrder() {
        // given
        ReviewRequest request = new ReviewRequest(1L, 99L, 5, "좋아요"); // productId=99 없음
        given(orderRepository.findByIdWithItems(1L)).willReturn(Optional.of(completedOrder));

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.PRODUCT_NOT_IN_ORDER.getMessage());
    }

    @Test
    @DisplayName("리뷰 작성 실패 - 이미 리뷰 작성한 주문")
    void createReview_alreadyExists() {
        // given
        ReviewRequest request = new ReviewRequest(1L, 1L, 4, "좋아요");
        given(orderRepository.findByIdWithItems(1L)).willReturn(Optional.of(completedOrder));
        given(reviewRepository.existsByOrderIdAndProductIdAndUserId(1L, 1L, 1L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.REVIEW_ALREADY_EXISTS.getMessage());
    }

    @Test
    @DisplayName("리뷰 작성 실패 - 다른 사용자의 주문")
    void createReview_accessDenied() {
        // given
        ReviewRequest request = new ReviewRequest(1L, 1L, 5, "좋아요");
        given(orderRepository.findByIdWithItems(1L)).willReturn(Optional.of(completedOrder));

        // when & then - userId=2L (다른 사용자)
        assertThatThrownBy(() -> reviewService.createReview(2L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.ORDER_ACCESS_DENIED.getMessage());
    }
}
