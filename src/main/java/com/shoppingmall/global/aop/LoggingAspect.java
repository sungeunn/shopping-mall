package com.shoppingmall.global.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    static final String REQUEST_ID_MDC_KEY = "requestId";
    static final long SLOW_API_THRESHOLD_MS = 1000;

    @Around("execution(* com.shoppingmall..*Controller.*(..))")
    public Object logApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = getCurrentRequest();
        // X-Request-Id 헤더가 있으면 재사용 (RequestIdFilter에서 설정), 없으면 MDC에서 가져옴
        String requestId = MDC.get(REQUEST_ID_MDC_KEY);
        if (requestId == null) {
            requestId = "-";
        }

        String method = request != null ? request.getMethod() : "-";
        String uri = request != null ? request.getRequestURI() : "-";
        String userId = resolveUserId();

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            logResult(requestId, userId, method, uri, elapsed, null);
            return result;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            logResult(requestId, userId, method, uri, elapsed, e);
            throw e;
        }
    }

    private void logResult(String requestId, String userId, String method,
                           String uri, long elapsed, Exception e) {
        if (e != null) {
            log.warn("[API] requestId={} userId={} method={} uri={} time={}ms status=FAILED error={}",
                    requestId, userId, method, uri, elapsed, e.getMessage());
        } else if (elapsed >= SLOW_API_THRESHOLD_MS) {
            log.warn("[API] requestId={} userId={} method={} uri={} time={}ms status=SLOW",
                    requestId, userId, method, uri, elapsed);
        } else {
            log.info("[API] requestId={} userId={} method={} uri={} time={}ms status=SUCCESS",
                    requestId, userId, method, uri, elapsed);
        }
    }

    private String resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Long userId) {
            return String.valueOf(userId);
        }
        return "anonymous";
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }
}
