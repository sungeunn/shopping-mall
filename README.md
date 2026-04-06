# 쇼핑몰 백엔드 API

Spring Boot 기반 쇼핑몰 백엔드 프로젝트입니다.
JWT 인증, Redis 캐싱/세션, 비관적 락 기반 재고 동시성 제어 등 실무에서 자주 마주치는 기술적 문제를 직접 구현하고 테스트합니다.

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| ORM | Spring Data JPA, QueryDSL 5.0 |
| Security | Spring Security, JWT (jjwt 0.12) |
| Cache / Session | Redis |
| Database | MySQL 8.0 (운영), H2 (테스트) |
| Build | Gradle |
| Test | JUnit 5, Mockito, Testcontainers |
| CI | GitHub Actions |
| Infra | Docker, Docker Compose |

---

## 주요 기능 및 기술적 구현 포인트

### 인증 / 보안
- **JWT Access + Refresh Token** — Refresh Token은 Redis에 저장(7일 TTL), 재발급 시 토큰 로테이션으로 탈취 감지
- **Access Token 블랙리스트** — 로그아웃 시 남은 만료 시간만큼 Redis에 등록하여 토큰 재사용 차단
- **로그인 실패 횟수 제한** — 5회 실패 시 10분 잠금 (Redis TTL 활용)

### 상품
- **QueryDSL 동적 쿼리** — 키워드, 카테고리, 가격 범위 조건을 BooleanBuilder로 조합
- **Redis 캐싱** — 상품 단건/목록 조회 캐싱, 등록/수정/삭제 시 캐시 무효화

### 주문
- **비관적 락(Pessimistic Lock)** — 동시 주문 시 재고 초과 방지 (`SELECT ... FOR UPDATE`)
- **결제 금액 검증** — 클라이언트가 전달한 단가와 서버 DB 가격 불일치 시 주문 거부
- **주문 상태 흐름** — PENDING → CONFIRMED → SHIPPED → COMPLETED / CANCELLED

### 리뷰
- **주문 연계 검증** — COMPLETED 상태 주문 + 해당 주문에 포함된 상품만 리뷰 작성 가능
- **중복 방지** — (order_id, product_id, user_id) unique 제약으로 이중 리뷰 차단

### 관리자
- **통계 API** — 오늘 주문 수, 총 매출, 상품별 판매량 (JPQL 집계 쿼리)
- **주문 상태 변경** — SHIPPED / COMPLETED 처리

### 운영 / 관찰 가능성
- **AOP 요청 로깅** — 모든 API 호출에 userId, URI, 처리시간, 성공/실패 자동 기록
- **MDC requestId** — 요청마다 고유 ID 생성, 로그 추적 및 `X-Request-Id` 응답 헤더 제공
- **Slow API 감지** — 처리시간 1000ms 초과 시 WARN 로그

---

## API 목록

<details>
<summary>인증</summary>

| Method | URI | 설명 | 인증 |
|--------|-----|------|------|
| POST | `/api/auth/signup` | 회원가입 | ❌ |
| POST | `/api/auth/login` | 로그인 | ❌ |
| POST | `/api/auth/reissue` | 토큰 재발급 | ❌ |
| POST | `/api/auth/logout` | 로그아웃 | ✅ |
| GET | `/api/auth/check-email` | 이메일 중복 확인 | ❌ |

</details>

<details>
<summary>회원</summary>

| Method | URI | 설명 | 인증 |
|--------|-----|------|------|
| GET | `/api/users/me` | 내 정보 조회 | ✅ |
| PUT | `/api/users/me` | 내 정보 수정 | ✅ |
| PATCH | `/api/users/me/password` | 비밀번호 변경 | ✅ |

</details>

<details>
<summary>상품</summary>

| Method | URI | 설명 | 인증 |
|--------|-----|------|------|
| GET | `/api/products` | 상품 목록 (키워드/카테고리/가격 필터) | ❌ |
| GET | `/api/products/{id}` | 상품 단건 조회 | ❌ |
| POST | `/api/admin/products` | 상품 등록 | 관리자 |
| PUT | `/api/admin/products/{id}` | 상품 수정 | 관리자 |
| DELETE | `/api/admin/products/{id}` | 상품 삭제 | 관리자 |

</details>

<details>
<summary>장바구니</summary>

| Method | URI | 설명 | 인증 |
|--------|-----|------|------|
| GET | `/api/cart` | 장바구니 조회 | ✅ |
| POST | `/api/cart` | 상품 추가/수량 변경 | ✅ |
| DELETE | `/api/cart/{productId}` | 상품 제거 | ✅ |
| POST | `/api/cart/order` | 장바구니 바로 주문 | ✅ |

</details>

<details>
<summary>주문</summary>

| Method | URI | 설명 | 인증 |
|--------|-----|------|------|
| POST | `/api/orders` | 주문 생성 | ✅ |
| GET | `/api/orders` | 내 주문 목록 | ✅ |
| GET | `/api/orders/{id}` | 주문 상세 | ✅ |
| POST | `/api/orders/{id}/cancel` | 주문 취소 | ✅ |
| PATCH | `/api/admin/orders/{id}/status` | 주문 상태 변경 | 관리자 |

</details>

<details>
<summary>리뷰 / 관리자 통계</summary>

| Method | URI | 설명 | 인증 |
|--------|-----|------|------|
| POST | `/api/reviews` | 리뷰 작성 (COMPLETED 주문만) | ✅ |
| GET | `/api/products/{id}/reviews` | 상품 리뷰 목록 | ❌ |
| GET | `/api/admin/stats` | 통계 (오늘 주문 수, 총 매출, 상품별 판매량) | 관리자 |

</details>

---

## 아키텍처

```
┌─────────────┐     ┌──────────────────────────────────────────┐
│   Client    │────▶│              Spring Boot App              │
└─────────────┘     │                                          │
                    │  Filter         Controller               │
                    │  ┌──────────┐   ┌──────────────────────┐ │
                    │  │RequestId │   │ Auth / User / Product│ │
                    │  │Filter    │   │ Order / Cart / Review│ │
                    │  └──────────┘   └──────────────────────┘ │
                    │                         │                │
                    │  Security               │ Service        │
                    │  ┌──────────┐   ┌──────────────────────┐ │
                    │  │JWT Filter│   │  비즈니스 로직        │ │
                    │  │Blacklist │   │  (락, 캐시, 검증)    │ │
                    │  └──────────┘   └──────────────────────┘ │
                    │                         │                │
                    │  AOP                    │ Repository     │
                    │  ┌──────────┐   ┌──────────────────────┐ │
                    │  │Logging   │   │  JPA / QueryDSL      │ │
                    │  │Aspect    │   └──────────────────────┘ │
                    │  └──────────┘            │               │
                    └─────────────────────────┼───────────────┘
                                              │
                          ┌───────────────────┼────────────┐
                          │                   │            │
                    ┌─────▼─────┐      ┌──────▼──────┐    │
                    │  MySQL    │      │    Redis     │    │
                    │ (주문/상품│      │ (토큰/캐시/  │    │
                    │  /리뷰)   │      │  로그인실패) │    │
                    └───────────┘      └─────────────┘    │
```

---

## 로컬 실행 방법

### 사전 요구사항
- Java 17
- Docker

### 1. 인프라 실행 (MySQL + Redis)

```bash
docker-compose up -d
```

### 2. 애플리케이션 실행

```bash
SPRING_PROFILES_ACTIVE=docker ./gradlew bootRun
```

서버 시작 후 Swagger UI: http://localhost:8081/swagger-ui.html

### 3. 종료

```bash
docker-compose down        # 컨테이너만 종료
docker-compose down -v     # 컨테이너 + 데이터 삭제
```

---

## 테스트

```bash
./gradlew test
```

- **단위 테스트** — Mockito 기반 서비스/컨트롤러 테스트
- **통합 테스트** — Testcontainers Redis로 캐싱, 동시성 테스트
- **동시성 테스트** — 10개 스레드 동시 주문으로 비관적 락 검증

---

## CI

`main` 브랜치 push 및 PR 시 GitHub Actions 자동 실행

- JDK 17 빌드
- 전체 테스트 (`./gradlew test`)
- 실패 시 테스트 리포트 아티팩트 업로드
