package com.shoppingmall.global.exception;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {

    MockMvc mockMvc;

    // ── 테스트용 컨트롤러 ─────────────────────────────────────────────────────
    @RestController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/business-exception")
        public void throwBusiness() {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        @PostMapping("/validation")
        public void requireBody(@RequestBody @jakarta.validation.Valid SampleBody body) {}

        @PostMapping("/bad-json")
        public void badJson(@RequestBody SampleBody body) {}

        @GetMapping("/type-mismatch/{id}")
        public void typeMismatch(@PathVariable Long id) {}

        @PostMapping("/missing-header")
        public void requireHeader(@RequestHeader("X-Required-Header") String header) {}

        @GetMapping("/missing-param")
        public void requireParam(@RequestParam String keyword) {}

        @PostMapping("/media-type")
        public void requireJson(@RequestBody SampleBody body) {}

        @GetMapping("/constraint-violation")
        public void throwConstraintViolation() {
            throw new ConstraintViolationException("제약 조건 위반", Set.of());
        }

        @GetMapping("/data-integrity")
        public void throwDataIntegrity() {
            throw new DataIntegrityViolationException("unique constraint violation");
        }
    }

    record SampleBody(@jakarta.validation.constraints.NotBlank String name) {}

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ── 테스트 ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BusinessException → 해당 ErrorCode의 HTTP 상태 + 메시지 반환")
    void handleBusinessException() throws Exception {
        mockMvc.perform(get("/test/business-exception"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(ErrorCode.PRODUCT_NOT_FOUND.getMessage()));
    }

    @Test
    @DisplayName("MethodArgumentNotValidException → 400 + 필드 검증 메시지 반환")
    void handleValidationException() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("HttpMessageNotReadableException → 400 + '요청 형식이 올바르지 않습니다.'")
    void handleHttpMessageNotReadable() throws Exception {
        mockMvc.perform(post("/test/bad-json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("요청 형식이 올바르지 않습니다."));
    }

    @Test
    @DisplayName("MethodArgumentTypeMismatchException → 400 + '요청 파라미터 타입이 올바르지 않습니다.'")
    void handleMethodArgumentTypeMismatch() throws Exception {
        mockMvc.perform(get("/test/type-mismatch/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("요청 파라미터 타입이 올바르지 않습니다."));
    }

    @Test
    @DisplayName("MissingRequestHeaderException → 400 + '필수 헤더가 누락되었습니다.'")
    void handleMissingRequestHeader() throws Exception {
        mockMvc.perform(post("/test/missing-header"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("필수 헤더가 누락되었습니다: X-Required-Header"));
    }

    @Test
    @DisplayName("HttpRequestMethodNotSupportedException → 405 + '지원하지 않는 HTTP 메서드입니다.'")
    void handleMethodNotAllowed() throws Exception {
        // GET만 허용된 엔드포인트에 DELETE 요청 → 405
        mockMvc.perform(delete("/test/business-exception"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("지원하지 않는 HTTP 메서드입니다."));
    }

    @Test
    @DisplayName("MissingServletRequestParameterException → 400 + '필수 파라미터가 누락되었습니다.'")
    void handleMissingServletRequestParameter() throws Exception {
        mockMvc.perform(get("/test/missing-param"))  // keyword 파라미터 없이 요청
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("필수 파라미터가 누락되었습니다: keyword"));
    }

    @Test
    @DisplayName("HttpMediaTypeNotSupportedException → 415 + '지원하지 않는 Content-Type입니다.'")
    void handleHttpMediaTypeNotSupported() throws Exception {
        mockMvc.perform(post("/test/media-type")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("지원하지 않는 Content-Type입니다.")));
    }

    @Test
    @DisplayName("ConstraintViolationException → 400")
    void handleConstraintViolation() throws Exception {
        mockMvc.perform(get("/test/constraint-violation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("DataIntegrityViolationException → 409 + '데이터 무결성 제약 조건을 위반했습니다.'")
    void handleDataIntegrityViolation() throws Exception {
        mockMvc.perform(get("/test/data-integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("데이터 무결성 제약 조건을 위반했습니다."));
    }
}
