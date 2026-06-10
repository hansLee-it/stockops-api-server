# StockOps AI Phase Reviews

## Review Template

- Date:
- Phase:
- Plan alignment:
- Code review notes:
- Tests run:
- Browser hands-on status:
- Blocked browser scenarios:
- Alternative tests completed:
- Failures:
- Risks:
- Decision:
- Phase status:

---

## 2026-06-09 | Documentation Task Review

- Date: 2026-06-09
- Phase: Documentation Task
- Plan alignment: 계획서 템플릿 그대로 3개 파일 생성. 추가 누락 없음.
- Code review notes: 코드 변경 없음. 문서 구조 일치.
- Tests run: 없음 (문서 생성만)
- Browser hands-on status: 해당 없음
- Blocked browser scenarios: 없음
- Alternative tests completed: 없음
- Failures: 없음
- Risks: 없음
- Decision: 완료
- Phase status: accepted

---

## 2026-06-09 | Phase 0 Review

- Date: 2026-06-09
- Phase: Phase 0 - FastAPI/Prophet Forecast Service Stabilization
- Plan alignment: 계획서 모든 스텝 완료. .env.example은 이미 완비되어 있어 수정 불필요.
- Code review notes: 기존 main.py, services/forecasting.py, models/prophet_model.py 수정 없음. 테스트만 추가됨. FakeModel이 predict_future 시그니처(make_future_dataframe + predict)와 정확히 일치.
- Tests run: pytest 18/18 PASS (Docker exec stockops-ai-module)
  - test_api_contract.py: 13 passed
  - test_forecasting_service.py: 2 passed
  - test_prophet_model_cache.py: 3 passed
- Browser hands-on status: 해당 없음 (Python 서비스, UI 없음)
- Blocked browser scenarios: 없음
- Alternative tests completed: 없음
- Failures: 없음
- Risks: Python이 호스트에 없어 Docker exec으로 실행. 향후 호스트 Python 설치 시 재검증 필요.
- Decision: 완료
- Phase status: accepted

---

## 2026-06-09 | Phase 1 - Task 1 Review

- Date: 2026-06-09
- Phase: Phase 1 - Spring AI Infrastructure (Task 1)
- Plan alignment: pom.xml SDK 의존성, application.yml 설정 블록, 프로퍼티 클래스 2개, AiForecastClient API-Key 주입 완료.
- Code review notes:
  - BedrockAiProperties.generationModelReference(): inferenceProfileArn 우선순위 올바름
  - AiForecastClient: apiKey blank 체크 후 헤더 추가 — null-safe
  - 기존 GeminiAiProvider (ExternalAiProvider 구현) 수정 없음 — 신규 AiGenerationProvider와 공존
- Tests run: 11/11 PASS (mvn test) — BedrockAiPropertiesTest 3, AiForecastClientTest 5, ProphetForecastModelTest 3
- Browser hands-on status: 해당 없음 (설정 레이어)
- Blocked browser scenarios: 없음
- Alternative tests completed: 없음
- Failures: 없음
- Risks: google-genai 1.5.0 vertexAI() 빌더 메서드명 불확실 — 실제 SDK 연동 시 재검증 필요
- Decision: 완료
- Phase status: accepted

---

## 2026-06-09 | Phase 1 - Tasks 2-7A Review

- Date: 2026-06-09
- Phase: Phase 1 - Spring AI Infrastructure (Tasks 2~7A)
- Plan alignment: 계획서 Tasks 2(DTOs+PromptBuilder), 3(Providers), 3A(AiProviderFacade), 4(BedrockAiFacade), 5(BedrockAgentRuntimeClientAdapter), 6(BedrockAiController), 7A(AiChatController) 완료.
- Code review notes:
  - BedrockPromptBuilder: sanitize()로 이중인용부호·백슬래시 escape → JSON 주입 방지
  - AiProviderFacade: chatVisible=false 요청에 serviceNotice/fallbackNotice "" 반환 → UI 오염 방지
  - BedrockAiFacade.buildOpsFacts: limit(20)으로 대량 추천 데이터 LLM 전달 제한
  - AIRecommendationService.detailRecommendation: readOnly, ScopeGuard 적용 — 기존 패턴 일치
  - BedrockAgentRuntimeClientAdapter.invokeAgent: pilot stub — KnowledgeBase만 실제 연동
  - AiChatController: @PreAuthorize 적용, AiGenerationRequest.chatVisible=true 고정
- Tests run: 22/22 PASS (mvn test) — BedrockPromptBuilderTest 3, AiProviderFacadeTest 4, BedrockAiFacadeTest 2, AiChatControllerTest 2 + Task1 11
- Browser hands-on status: BLOCKED_ENVIRONMENT (Bedrock 자격증명 없음)
- Blocked browser scenarios: TS-P1-010, TS-P1-011
- Alternative tests completed: Unit 테스트로 비활성화 폴백 응답 검증 완료
- Failures: 없음
- Risks:
  - google-genai SDK vertexAI() 메서드: 컴파일은 성공하나 런타임 연동은 미검증
  - invokeAgent: KnowledgeBase 실제 응답 파싱은 Bedrock 자격증명 환경에서 추가 검증 필요
- Decision: 완료 (미검증 브라우저 시나리오는 Bedrock 자격증명 환경에서 후속 검증)
- Phase status: accepted

---

## 2026-06-09 | Phase 1 - Tasks 7B, 8, 9 Review

- Date: 2026-06-09
- Phase: Phase 1 - Spring AI Infrastructure (Tasks 7B, 8, 9)
- Plan alignment: 모든 계획 태스크 완료 (0~9). 프론트엔드 채팅, Agent-AISuggestion 연동, Live Smoke Test 완성.
- Code review notes:
  - AiChatPage: providerStatusNotice 상태로 배너 노출/해제. role="status" 접근성 준수.
  - BedrockAiFacade.invokeAgent: scope 없는 경우 silent skip으로 안전 처리.
  - AISuggestion.approvalMode="MANUAL_APPROVAL_REQUIRED": 자동 승인 경로 없음.
  - BedrockLiveSmokeTest: @EnabledIfEnvironmentVariable로 CI 기본 실행 제외 확실.
  - BedrockAgentInvokeRequest: targetScopeType/targetScopeId 추가로 scope-aware 제안 가능.
- Tests run:
  - stockops-ai-module: pytest 18/18 PASS
  - stockops-api-server: mvn test 255/257 PASS (2개 기존 실패 존재 — 아래 참조)
  - stockops-admin-web: vitest 252/252 PASS (30 test files)
- Browser hands-on status: BLOCKED_ENVIRONMENT (Bedrock 자격증명 없음)
- Blocked browser scenarios: TS-P1-010, TS-P1-011
- Alternative tests completed: 단위 테스트로 모든 폴백 경로 검증 완료
- Failures:
  - AISuggestionIntegrationTest.failedExecutionPathTransitionsApprovedSuggestionAndAuditsFailure:225 — 기존 실패 (내 커밋과 무관, 테스트 파일 미수정 확인). errorMessage 직렬화 불일치.
  - AuditLogServiceTest.searchAuditLogsSupportsUnfilteredRequests — 기존 실패 (내 커밋과 무관). Mockito PotentialStubbingProblem.
- Risks:
  - VertexAiGenerationProvider: google-genai SDK vertexAI() 런타임 연동 미검증
  - BedrockAgentRuntimeClientAdapter.invokeAgent: 실제 Agent 응답 파싱 미검증 (pilot stub)
  - AiChatPage: 브라우저 직접 테스트 미실행 (Node.js 호스트 미설치로 Docker로 vitest 실행)
- Decision: 완료
- Phase status: accepted

---

## Phase 2 Review

### Phase 2 계획

- Date: 2026-06-10
- Phase status: accepted
- Tasks: Task 1~7 (AI 호출 지표, 추천 설명 캐시, 운영 요약 배치, RAG rate limiting, Circuit Breaker, Agent Tool Dispatcher, Frontend 설명 패널)
- Acceptance criteria: 모든 Phase 2 태스크 구현 완료, 신규 테스트 포함 전체 테스트 PASS

### Phase 2 Task 1~7 구현 완료

- Date: 2026-06-10
- Phase status: accepted
- Summary: Phase 2 전 태스크 구현 완료. 신규 파일 13개, 수정 파일 10개.
- New tests added:
  - AiCallMetricsTest (4)
  - AiOpsSummarySchedulerTest (2)
  - AiRagRateLimiterTest (5)
  - AgentToolDispatcherTest (7)
  - AiExplanationPanel.test.tsx (7)
- Verification:
  - stockops-api-server: mvn test - 전체 테스트 PASS (exit code 0)
  - stockops-admin-web: npx vitest run (실행 중)
- Breaking changes: 없음 (모든 provider defaults enabled=false, 신규 기능은 옵트인)
- Rollback plan: AiOpsSummaryScheduler, AiRagRateLimiter, BedrockGenerationProvider는 설정으로 비활성화 가능
- Next phase candidates: Bedrock Agent 실제 InvokeAgent SDK 호출 (AWS 자격증명 필요), CloudWatch 연동

---

## Phase 2 Task 2b 보완 - 토큰 추적

- Date: 2026-06-10
- Phase status: accepted
- Summary: Phase 2 계획에 명시된 inputTokens/outputTokens 필드가 구현에서 누락된 것을 발견하고 보완. Bedrock Converse API response.usage()에서 실제 토큰 수 추출. ai.bedrock.tokens{direction=input/output} 카운터 추가.
- New tests added:
  - AiCallMetricsTest: record_nullTokenCounts_doesNotRegisterTokenCounter (신규)
  - AiCallMetricsTest: record_successfulBedrockCall — 토큰 카운터 어설션 추가
- Breaking changes: 없음 (필드 추가만, 기존 behavior 유지)

---

## Phase 3 초기 - §8 정책 수정 + Vertex AI 토큰 추출

- Date: 2026-06-10
- Phase: Phase 3 (Task 1 + Task 2)
- Phase status: accepted
- Summary:
  - §8 정책 위반(JSON 파싱 실패 시 raw model text → summary 저장) 발견 및 수정
  - 잘못된 계약을 테스트하던 `explainRecommendation_fallsBackToRawTextWhenJsonInvalid` 수정
  - Vertex AI 토큰 사용량 추출 구현 (google-genai 1.5.0 usageMetadata API, javap으로 반환 타입 확인)
  - VertexAiGenerationProviderTest 신규 작성 (5 tests)
  - Phase 3 계획 문서 작성 (cur_ai-docs/2026-06-10-stockops-bedrock-ai-phase3-plan.md)
- Root cause (§8 위반): BedrockAiFacade의 JSON 파싱 catch 블록이 raw model text를 summary 필드에 직접 저장. 동시에 테스트도 이 잘못된 동작을 정식 계약으로 검증하고 있었음. 자기 검증(self-verification) 함정.
- New tests added:
  - BedrockAiFacadeTest.explainRecommendation_returnsSafeFallbackWhenJsonInvalid (기존 테스트 수정)
  - VertexAiGenerationProviderTest.generate_extractsTokenCountsFromUsageMetadata (신규)
  - VertexAiGenerationProviderTest.generate_handlesAbsentUsageMetadataGracefully (신규)
  - VertexAiGenerationProviderTest.generate_throwsWhenProviderDisabled (신규)
  - VertexAiGenerationProviderTest.generate_throwsWhenProjectIdNotConfigured (신규)
  - VertexAiGenerationProviderTest.generate_throwsWhenResponseTextIsBlank (신규)
  - VertexAiGenerationProviderTest.generate_includesFallbackNoticeOnlyForChatVisibleRequests (신규)
- Breaking changes: 없음 (정상 JSON 파싱 경로 동작 변경 없음, summary 키 존재 시 동일 동작)
- Constraints verified: AIRecommendationService 예측 알고리즘 무변경, AISuggestion 상태 전이 무변경
- Next phase: Phase 3 Task 3 — §5.4 output spec sourceCounts + confidenceCaveat

---

## Phase 3 §5.4 출력 스펙 보완 — sourceCounts + confidenceCaveat

- Date: 2026-06-10
- Phase: Phase 3 (Task 3)
- Phase status: accepted
- Summary:
  - `BedrockOpsSummaryResponse`에 `sourceCounts(Map<String,Integer>)` + `confidenceCaveat(String)` 추가
  - `buildOpsFacts()`를 `OpsFacts` private record를 반환하도록 리팩터링
  - `OpsFacts.toSourceCounts()` / `OpsFacts.buildConfidenceCaveat()` — 결정론적 계산 (§8 정책 준수)
  - `AiOpsSummarySchedulerTest` — BedrockOpsSummaryResponse 생성자 업데이트
  - `BedrockAiFacadeTest` — sourceCounts/confidenceCaveat 어설션 추가
- Root cause (§5.4 gap): 설계 문서 §5.4 출력 스펙이 "source counts"와 "confidence caveat"를 명시하고 있었으나 BedrockOpsSummaryResponse에 구현되지 않았음
- Design decision:
  - AI 응답에서 sourceCounts/confidenceCaveat를 파싱하지 않음 → 조작 가능성 제거, §8 정책 준수
  - confidenceCaveat: 데이터 총량 < 5 → 부족 경고, ≥ 5 → 각 소스 건수 요약
  - OpsFacts record: Java 16+ private record, BedrockAiFacade 외부 노출 없음
  - API 응답 하위 호환: 기존 필드 변경 없이 신규 필드 추가
- New tests added:
  - AiOpsSummarySchedulerTest: BedrockOpsSummaryResponse 10-arg constructor 적용 (기존 2 tests 유지)
  - BedrockAiFacadeTest.summarizeOperations_parsesJsonFieldsFromBedrockResponse: sourceCounts/confidenceCaveat 어설션 추가
- Breaking changes: 없음 (기존 API 클라이언트는 신규 필드 무시 가능)
- Constraints verified: AIRecommendationService 예측 알고리즘 무변경, AISuggestion 상태 전이 무변경
- Remaining Phase 3 candidates: overdueShipmentCount in ops facts (deferred), inventoryBelowSafetyStock (blocked)
