# StockOps AI Work History

## Entry Template

- Date:
- Phase:
- Summary:
- Files changed:
- Decisions:
- Blockers:
- Verification:
- Next step:

---

## 2026-06-09 | Documentation Task

- Date: 2026-06-09
- Phase: Documentation Task
- Summary: 작업추적 아티팩트(work-history.md, test-scenarios.md, phase-reviews.md) 생성. Phase Governance 게이트 충족.
- Files changed:
  - docs/ai-bedrock/work-history.md (신규)
  - docs/ai-bedrock/test-scenarios.md (신규)
  - docs/ai-bedrock/phase-reviews.md (신규)
- Decisions:
  - 문서 경로: stockops-api-server/docs/ai-bedrock/ (설계문서 기준)
  - 문서 구조: 계획서 템플릿 그대로 사용
- Blockers: 없음
- Verification: 파일 생성 확인
- Next step: Phase 0 (stockops-ai-module 안정화)

---

## 2026-06-09 | Phase 0

- Date: 2026-06-09
- Phase: Phase 0 - FastAPI/Prophet Forecast Service Stabilization
- Summary: Prophet 모델 캐시 테스트(TTL/LRU), 예측 서비스 테스트(fill_missing_dates, forecast_async), bulk 부분 실패 테스트 추가. README API 계약 완성, 환경변수 표 보완.
- Files changed (stockops-ai-module):
  - tests/test_prophet_model_cache.py (신규)
  - tests/test_forecasting_service.py (신규)
  - tests/test_api_contract.py (bulk partial failure 테스트 추가)
  - README.md (API 계약, 환경변수 표 완성)
- Decisions:
  - .env.example은 이미 모든 필요 변수 포함 → 수정 불필요
  - Python이 호스트에 없어서 Docker 컨테이너 내에서 pytest 실행
- Blockers: 없음
- Verification: pytest 18/18 PASS (Docker exec 확인)
- Next step: Phase 1 - Spring Forecast Client Contract / Task 1

---

## 2026-06-09 | Phase 1 - Task 1 (Spring Config Foundations)

- Date: 2026-06-09
- Phase: Phase 1 - Spring AI Infrastructure
- Summary: Spring Boot AI 기반 설정 작업 완료. AWS SDK 및 Google GenAI 의존성 추가, 설정 프로퍼티 클래스 생성, application.yml 확장.
- Files changed:
  - pom.xml (AWS SDK 2.44.13 bedrockruntime+agentruntime, google-genai 1.5.0 추가)
  - src/main/resources/application.yml (stockops.ai.bedrock, stockops.ai.vertex 블록 추가)
  - src/main/java/com/stockops/ai/bedrock/BedrockAiProperties.java (신규)
  - src/main/java/com/stockops/ai/gcp/VertexAiProperties.java (신규)
  - src/main/java/com/stockops/ai/forecast/AiForecastProperties.java (api-key 필드 추가)
  - src/main/java/com/stockops/ai/forecast/AiForecastClient.java (X-API-Key 헤더 주입)
  - src/test/java/com/stockops/ai/bedrock/BedrockAiPropertiesTest.java (신규 - 3 tests)
  - src/test/java/com/stockops/ai/forecast/AiForecastClientTest.java (신규 - 5 tests)
  - src/test/java/com/stockops/ai/forecast/ProphetForecastModelTest.java (신규 - 3 tests)
- Decisions:
  - enabled: false 기본값 - 프로덕션 자격증명 없이 안전한 배포 가능
  - generationModelReference() 헬퍼: inferenceProfileArn > modelId 우선순위
  - Maven/Python 호스트 미설치 → Docker 컨테이너 빌드 방식 확립 (MSYS_NO_PATHCONV=1, //c/ 경로)
- Blockers: 없음
- Verification: mvn -DskipTests compile 성공 (0 errors). 11개 신규 테스트 모두 PASS.
- Next step: Tasks 2-7A (AI Provider 구현체, Facade, Controller)

---

## 2026-06-09 | Phase 1 - Tasks 2-7A (AI Providers, Facades, Controllers)

- Date: 2026-06-09
- Phase: Phase 1 - Spring AI Infrastructure
- Summary: AI 생성 공급자 인터페이스 및 구현체, 프롬프트 빌더, Bedrock 파사드, Chat 컨트롤러 등 전체 AI 레이어 완성.
- Files changed (main):
  - src/main/java/com/stockops/ai/bedrock/dto/BedrockRecommendationExplanationResponse.java (신규)
  - src/main/java/com/stockops/ai/bedrock/dto/BedrockOpsSummaryResponse.java (신규)
  - src/main/java/com/stockops/ai/bedrock/dto/BedrockRagQueryRequest.java (신규)
  - src/main/java/com/stockops/ai/bedrock/dto/BedrockRagQueryResponse.java (신규)
  - src/main/java/com/stockops/ai/bedrock/dto/BedrockAgentInvokeRequest.java (신규)
  - src/main/java/com/stockops/ai/bedrock/dto/BedrockAgentInvokeResponse.java (신규)
  - src/main/java/com/stockops/ai/bedrock/BedrockPromptBuilder.java (신규)
  - src/main/java/com/stockops/ai/provider/AiServiceStatus.java (신규)
  - src/main/java/com/stockops/ai/provider/AiGenerationRequest.java (신규)
  - src/main/java/com/stockops/ai/provider/AiGenerationResponse.java (신규)
  - src/main/java/com/stockops/ai/provider/AiGenerationProvider.java (신규)
  - src/main/java/com/stockops/ai/bedrock/BedrockGenerationProvider.java (신규)
  - src/main/java/com/stockops/ai/gcp/VertexAiGenerationProvider.java (신규)
  - src/main/java/com/stockops/ai/provider/AiProviderFacade.java (신규)
  - src/main/java/com/stockops/ai/bedrock/BedrockAgentRuntimeClientAdapter.java (신규)
  - src/main/java/com/stockops/ai/bedrock/BedrockAiFacade.java (신규)
  - src/main/java/com/stockops/controller/BedrockAiController.java (신규)
  - src/main/java/com/stockops/ai/chat/dto/AiChatRequest.java (신규)
  - src/main/java/com/stockops/ai/chat/dto/AiChatResponse.java (신규)
  - src/main/java/com/stockops/controller/AiChatController.java (신규)
  - src/main/java/com/stockops/service/ai/AIRecommendationService.java (detailRecommendation 메서드 추가)
- Files changed (test):
  - src/test/java/com/stockops/ai/bedrock/BedrockPromptBuilderTest.java (신규 - 3 tests)
  - src/test/java/com/stockops/ai/provider/AiProviderFacadeTest.java (신규 - 4 tests)
  - src/test/java/com/stockops/ai/bedrock/BedrockAiFacadeTest.java (신규 - 2 tests)
  - src/test/java/com/stockops/controller/AiChatControllerTest.java (신규 - 2 tests)
- Decisions:
  - AiProviderFacade: Bedrock 실패 시 Vertex 폴백, 인증 실패 판단은 메시지 키워드 기반
  - chatVisible=false 요청 (추천 설명 등)은 serviceNotice/fallbackNotice 숨김 → UI 오염 방지
  - BedrockPromptBuilder: productName에 이중인용부호 escape 처리
  - BedrockAiFacade: properties.isEnabled()==false 시 "fallback" modelId 즉시 반환
  - BedrockAgentRuntimeClientAdapter: invokeAgent는 pilot stub (KnowledgeBase만 실제 연동)
  - AIRecommendationService.detailRecommendation: readOnly 트랜잭션, ScopeGuard 적용
  - 모든 엔드포인트: @PreAuthorize("@permissionChecker.hasPermission('AI_RECOMMENDATION_READ')")
- Blockers: 없음
- Verification: mvn -DskipTests compile 성공. 테스트 22/22 PASS (BedrockPromptBuilderTest 3, AiProviderFacadeTest 4, BedrockAiFacadeTest 2, AiChatControllerTest 2, + Task1 기존 11 PASS)
- Next step: Task 7B (프론트엔드 Chat 폴백 배너), Task 8 (Agent→AISuggestion 연동), Task 9 (Bedrock Live Smoke Test)

---

## 2026-06-09 | Phase 1 - Task 7B (Frontend Chat Fallback Banner)

- Date: 2026-06-09
- Phase: Phase 1 - Spring AI Infrastructure (Task 7B)
- Summary: React 채팅 페이지 + AI 공급자 폴백 배너 완성. stockops-admin-web에 AiChatPage, API 클라이언트, 타입 정의 추가.
- Files changed (stockops-admin-web):
  - src/types/aiChat.ts (신규 - AiChatResponse, AiChatRequest, ChatMessage 타입)
  - src/api/aiChat.ts (신규 - sendChatMessage API 클라이언트)
  - src/pages/AiChatPage.tsx (신규 - 채팅 UI + 폴백 배너, role="status")
  - src/pages/AiChatPage.test.tsx (신규 - 4 tests)
  - src/App.tsx (ai/chat 라우트 추가)
- Decisions:
  - providerStatusNotice 상태: serviceNotice || fallbackNotice 우선순위로 배너 노출
  - Bedrock 복구 시 (fallbackUsed=false, notice 없음) → 배너 자동 해제
  - scrollIntoView에 optional chaining 사용 → jsdom 테스트 환경 안전
  - banner에 role="status" → 접근성, 테스트 쿼리 용이
  - Node.js 미호스트 → Docker node:20-alpine으로 vitest 실행
- Blockers: 없음
- Verification: vitest 4/4 PASS (AiChatPage.test.tsx). 기존 248 테스트 모두 유지.
- Next step: Task 8 (Agent→AISuggestion 연동)

---

## 2026-06-09 | Phase 1 - Task 8 (Agent→AISuggestion 연동)

- Date: 2026-06-09
- Phase: Phase 1 - Spring AI Infrastructure (Task 8)
- Summary: Bedrock Agent 제안을 AISuggestion 승인 흐름에 연동. BedrockAiFacade.invokeAgent가 actionSuggested=true 시 AISuggestion.PENDING 생성.
- Files changed:
  - src/main/java/com/stockops/ai/bedrock/dto/BedrockAgentInvokeRequest.java (targetScopeType, targetScopeId 필드 추가)
  - src/main/java/com/stockops/ai/bedrock/BedrockAiFacade.java (invokeAgent AISuggestion 생성 로직 추가)
  - src/test/java/com/stockops/ai/bedrock/BedrockAiFacadeTest.java (3 테스트 추가)
- Decisions:
  - scope 미제공 시 AISuggestion 생성 생략 (silent skip) → 스코프 없는 호출은 오류 없이 통과
  - AISuggestion create 실패 시 warn 로그만 출력 → agent 응답 자체는 항상 반환
  - currentUser=null 전달 → AI_AGENT 소스로 tool-created 경로 사용
  - approvalMode="MANUAL_APPROVAL_REQUIRED" → 자동 승인 없음
- Blockers: 없음
- Verification: mvn test 14/14 PASS (BedrockAiFacadeTest 포함)
- Next step: Task 9 (Bedrock Live Smoke Test)

---

## 2026-06-09 | Phase 1 - Task 9 (Bedrock Live Smoke Test)

- Date: 2026-06-09
- Phase: Phase 1 - Spring AI Infrastructure (Task 9)
- Summary: BedrockLiveSmokeTest 생성 (기본 비활성화). README에 Bedrock 환경변수 및 라이브 테스트 실행 방법 문서화.
- Files changed:
  - src/test/java/com/stockops/ai/bedrock/BedrockLiveSmokeTest.java (신규)
  - README.md (AI 공급자 환경변수 표, Bedrock Live Smoke Tests 섹션 추가)
- Decisions:
  - @EnabledIfEnvironmentVariable(named = "STOCKOPS_BEDROCK_LIVE_TESTS", matches = "true") → CI 기본 실행 제외
  - @Tag("bedrock-live") + mvn -Dgroups=bedrock-live test → 선택적 실행
- Blockers: 없음
- Verification: 일반 mvn test 실행 시 BedrockLiveSmokeTest 제외 확인 (환경변수 미설정)
- Next step: 최종 QA 및 전체 테스트 스위트 실행

---

## 2026-06-09 | 기존 테스트 실패 수정 (Bugfix)

- Date: 2026-06-09
- Phase: Bugfix (Phase 1 완료 후)
- Summary: Phase 1 AI 작업과 무관한 기존 테스트 실패 2건 수정. 257/257 PASS 달성.
- Files changed:
  - src/main/java/com/stockops/service/ai/AISuggestionService.java — recordFailedExecution: toJsonString() 제거, plain string 직접 저장
  - src/test/java/com/stockops/service/AuditLogServiceTest.java — Mock stub 교정: findAll(Sort) → findAll(Pageable), findById → findAllById
  - src/test/java/com/stockops/service/ai/AISuggestionServiceTest.java — executionResult 기대값 교정: JSON 인코딩된 문자열 → plain string
- Decisions:
  - AISuggestionIntegrationTest line 225/229이 진짜 스펙: executionResult는 plain string 저장이 맞음.
  - AISuggestionServiceTest는 기존 잘못된 구현을 테스트하고 있었음 → 함께 교정.
  - AuditLogService는 이미 findAllById 배치 조회를 사용 중이었으나 테스트가 findById를 mocking하고 있어 PotentialStubbingProblem 발생.
- Blockers: 없음
- Verification: mvn test 257/257 PASS (Skipped: BedrockLiveSmokeTest 환경변수 미설정)
- Next step: 원격 저장소 push (사용자 확인 필요)
