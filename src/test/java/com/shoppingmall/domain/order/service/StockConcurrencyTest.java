package com.shoppingmall.domain.order.service;

import com.shoppingmall.config.TestSecurityConfig;
import com.shoppingmall.domain.cart.repository.CartRepository;
import com.shoppingmall.domain.order.dto.OrderRequest;
import com.shoppingmall.domain.order.repository.OrderRepository;
import com.shoppingmall.domain.product.entity.Product;
import com.shoppingmall.domain.product.repository.ProductRepository;
import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.domain.user.entity.UserRole;
import com.shoppingmall.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class StockConcurrencyTest {

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Autowired private OrderService orderService;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private CartRepository cartRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // FK 순서에 맞게 삭제: order_items(cascade) → orders → carts → products → users
        orderRepository.deleteAll();
        cartRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(User.builder()
                .email("concurrent@test.com")
                .password("password")
                .name("테스터")
                .role(UserRole.ROLE_USER)
                .build());
    }

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll();
        cartRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("재고 10개 - 10명 동시 주문 → 전원 성공, 재고 정확히 0 (비관적 락 검증)")
    void concurrentOrder_allSuccess() throws InterruptedException {
        // given - 재고 10개 상품
        Product product = productRepository.save(Product.builder()
                .name("노트북").description("설명").price(1_000_000).stock(10).category("전자기기").build());

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);  // 동시 출발 신호총
        CountDownLatch doneLatch = new CountDownLatch(threadCount); // 완료 대기
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // 모든 스레드가 여기서 대기 → countDown() 호출 시 동시 출발
                    OrderRequest request = new OrderRequest(
                            List.of(new OrderRequest.OrderItemRequest(product.getId(), 1, product.getPrice())),
                            "홍길동", "010-1234-5678", "서울시 강남구"
                    );
                    orderService.createOrder(testUser.getId(), request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 동시 출발!
        doneLatch.await();      // 모든 스레드 완료까지 대기
        executor.shutdown();

        // then
        assertThat(successCount.get()).isEqualTo(10);
        assertThat(failCount.get()).isEqualTo(0);
        Product result = productRepository.findById(product.getId()).orElseThrow();
        assertThat(result.getStock()).isEqualTo(0);
    }

    @Test
    @DisplayName("재고 5개 - 10명 동시 주문 → 5개 성공, 5개 실패, 재고 정확히 0 (비관적 락 검증)")
    void concurrentOrder_partialSuccess() throws InterruptedException {
        // given - 재고 5개 상품
        Product product = productRepository.save(Product.builder()
                .name("마우스").description("설명").price(50_000).stock(5).category("전자기기").build());

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    OrderRequest request = new OrderRequest(
                            List.of(new OrderRequest.OrderItemRequest(product.getId(), 1, product.getPrice())),
                            "홍길동", "010-1234-5678", "서울시 강남구"
                    );
                    orderService.createOrder(testUser.getId(), request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 재고 부족 → INSUFFICIENT_STOCK 예외 → 주문 롤백
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        // then - 비관적 락으로 순차 처리되어 정확히 5개만 성공
        assertThat(successCount.get()).isEqualTo(5);
        assertThat(failCount.get()).isEqualTo(5);
        Product result = productRepository.findById(product.getId()).orElseThrow();
        assertThat(result.getStock()).isEqualTo(0);
    }
}
