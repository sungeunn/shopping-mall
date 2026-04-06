package com.shoppingmall.domain.order.service;

import com.shoppingmall.domain.order.dto.AdminStatsResponse;
import com.shoppingmall.domain.order.dto.ProductSalesResponse;
import com.shoppingmall.domain.order.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdminStatsServiceTest {

    @InjectMocks
    private AdminStatsService adminStatsService;

    @Mock
    private OrderRepository orderRepository;

    @Test
    @DisplayName("통계 조회 - 오늘 주문 수, 총 매출, 상품별 판매량 반환")
    void getStats_returnsCorrectData() {
        // given
        List<ProductSalesResponse> productSales = List.of(
                new ProductSalesResponse(1L, "노트북", 5L, 7_500_000L),
                new ProductSalesResponse(2L, "마우스", 10L, 500_000L)
        );
        given(orderRepository.countTodayOrders(any())).willReturn(3L);
        given(orderRepository.sumTotalRevenue()).willReturn(8_000_000L);
        given(orderRepository.findProductSales()).willReturn(productSales);

        // when
        AdminStatsResponse stats = adminStatsService.getStats();

        // then
        assertThat(stats.todayOrderCount()).isEqualTo(3L);
        assertThat(stats.totalRevenue()).isEqualTo(8_000_000L);
        assertThat(stats.productSales()).hasSize(2);
        assertThat(stats.productSales().get(0).productName()).isEqualTo("노트북");
        assertThat(stats.productSales().get(0).totalQuantity()).isEqualTo(5L);
    }

    @Test
    @DisplayName("통계 조회 - 주문이 없을 때 0 반환")
    void getStats_emptyOrders() {
        // given
        given(orderRepository.countTodayOrders(any())).willReturn(0L);
        given(orderRepository.sumTotalRevenue()).willReturn(0L);
        given(orderRepository.findProductSales()).willReturn(List.of());

        // when
        AdminStatsResponse stats = adminStatsService.getStats();

        // then
        assertThat(stats.todayOrderCount()).isEqualTo(0L);
        assertThat(stats.totalRevenue()).isEqualTo(0L);
        assertThat(stats.productSales()).isEmpty();
    }
}
