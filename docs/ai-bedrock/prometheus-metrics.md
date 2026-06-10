# StockOps AI — Prometheus 메트릭 참조

> 구현 위치: `AiCallMetrics.java` (Phase 2 Task 1)
> 기반 라이브러리: Micrometer + Prometheus (spring-boot-starter-actuator + micrometer-registry-prometheus)

---

## 1. 메트릭 목록

| 메트릭 이름 | 유형 | 태그 | 설명 |
|------------|------|------|------|
| `ai.bedrock.requests` | Counter | `operation`, `provider`, `status` | AI 생성 요청 수 (성공/실패/fallback 포함) |
| `ai.bedrock.latency` | Timer | `operation`, `provider` | AI 생성 소요 시간 (히스토그램 포함) |
| `ai.bedrock.tokens` | Counter | `operation`, `provider`, `direction` | 토큰 사용량 (`direction=input` 또는 `output`) |

### 태그 상세

| 태그 | 값 예시 | 설명 |
|------|---------|------|
| `operation` | `RECOMMENDATION_EXPLANATION`, `OPS_SUMMARY`, `RAG_QUERY`, `CHAT` | AI 호출 작업 유형 |
| `provider` | `bedrock`, `vertex-ai` | 실제로 응답한 공급자 |
| `status` | `success`, `failure` | AI 호출 결과 |
| `direction` | `input`, `output` | 토큰 방향 |

---

## 2. Spring Boot Actuator 설정

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: prometheus, health, info, metrics
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    tags:
      application: stockops-api-server
      env: ${SPRING_PROFILES_ACTIVE:local}
```

엔드포인트: `GET /actuator/prometheus`

---

## 3. Prometheus 스크레이프 설정 예시

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'stockops-api'
    static_configs:
      - targets: ['stockops-api:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s
    scrape_timeout: 10s
    metric_relabel_configs:
      - source_labels: [__name__]
        regex: 'ai_bedrock.*'
        action: keep
```

---

## 4. PromQL 예시

### 분당 AI 요청률 (공급자별)
```promql
rate(ai_bedrock_requests_total[1m]) by (provider, status)
```

### Bedrock → Vertex AI fallback 비율 (5분 이동 평균)
```promql
rate(ai_bedrock_requests_total{provider="vertex-ai"}[5m])
  / ignoring(provider) group_left rate(ai_bedrock_requests_total[5m])
```

### P99 응답 지연 (작업 유형별)
```promql
histogram_quantile(0.99,
  rate(ai_bedrock_latency_seconds_bucket[5m])) by (operation)
```

### 최근 1시간 토큰 사용량 (방향별, 공급자별)
```promql
sum(increase(ai_bedrock_tokens_total[1h])) by (provider, direction)
```

### 분당 평균 입력 토큰 수 (Bedrock)
```promql
rate(ai_bedrock_tokens_total{provider="bedrock",direction="input"}[1m])
/ rate(ai_bedrock_requests_total{provider="bedrock",status="success"}[1m])
```

---

## 5. 구현 메모

### Token null 처리
Bedrock Converse API는 항상 토큰 수를 반환한다. Vertex AI는 `usageMetadata()`가 absent일 수 있으며, 이 경우 `ai.bedrock.tokens` 카운터가 등록되지 않는다 (0으로 기록하지 않음).

### Circuit Breaker 지표
Resilience4j circuit breaker 상태는 `/actuator/health/circuitBreakers`와 Micrometer `resilience4j.circuitbreaker.*` 메트릭으로 확인 가능.

```yaml
management:
  health:
    circuitbreakers:
      enabled: true
```

### 주요 circuit breaker 메트릭
| 메트릭 | 설명 |
|--------|------|
| `resilience4j.circuitbreaker.state` | 0=CLOSED, 1=OPEN, 2=HALF_OPEN |
| `resilience4j.circuitbreaker.calls` | 호출 수 (kind=successful/failed/not_permitted) |
| `resilience4j.circuitbreaker.failure.rate` | 실패율 |

---

## 6. 구현 파일 참조

| 파일 | 역할 |
|------|------|
| `src/main/java/com/stockops/ai/metrics/AiCallMetrics.java` | Micrometer 메트릭 등록 + 감사 로그 |
| `src/main/java/com/stockops/ai/metrics/AiCallRecord.java` | 호출 레코드 DTO |
| `src/main/java/com/stockops/ai/provider/AiProviderFacade.java` | AiCallMetrics 호출 지점 |
| `src/main/java/com/stockops/ai/bedrock/BedrockGenerationProvider.java` | Bedrock 토큰 추출 (`response.usage()`) |
| `src/main/java/com/stockops/ai/gcp/VertexAiGenerationProvider.java` | Vertex AI 토큰 추출 (`response.usageMetadata()`) |
