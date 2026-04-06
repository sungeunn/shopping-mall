package com.shoppingmall.domain.order.service;

import com.shoppingmall.domain.order.dto.AdminStatsResponse;
import com.shoppingmall.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatsService {

    private final OrderRepository orderRepository;

    public AdminStatsResponse getStats() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

        return new AdminStatsResponse(
                orderRepository.countTodayOrders(startOfDay),
                orderRepository.sumTotalRevenue(),
                orderRepository.findProductSales()
        );
    }
}
