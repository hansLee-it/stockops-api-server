# StockOps API EB Deployment Handoff

작성 시각: 2026-05-12 18:37 KST

## 현재 상태

- 작업 디렉토리: `/Users/hans/Documents/gitlab_workspace/stockops-api-server`
- 최신 커밋: `4415b1a Fix EB build and health deployment`
- EB 리전: `ap-northeast-2` (서울)
- EB 실행 환경은 비용 방지를 위해 모두 종료됨.
- EB 애플리케이션 버전도 모두 삭제됨.
- EB 태그가 붙은 EC2 인스턴스와 ALB/NLB는 남아 있지 않음.
- API 최종 통신 테스트는 정리 요청으로 인해 재수행하지 않음.

## 커밋된 주요 수정

커밋 `4415b1a`에 포함된 내용:

- EB 헬스체크 경로를 `/actuator/health`로 지정
  - `.ebextensions/healthcheck.config`
- Flyway `V20__ai_recommendation_engine.sql`에서 `analytics` 스키마를 먼저 생성하도록 수정
- Spring Boot 3.4 계열 컴파일 오류 수정
  - `RestTemplateBuilder` timeout API 변경 대응
  - WebSocket destination prefix API 사용 방식 수정
  - `CategoryRepository` 누락 메서드 추가
  - `ExcelImportService` 요청 DTO 생성자 인자 보정
  - `InventoryTurnoverReportService` 시간 타입 변환 보정
  - 테스트 컴파일 오류 최소 수정

## EB 배포 이력

기존 `stockops-api-test-v2`는 Rolling update 중 Red/Severe 상태로 막혀 새 환경을 생성했다.

새 환경 생성:

```bash
eb create stockops-api-test-v3 \
  --region ap-northeast-2 \
  --envvars SPRING_PROFILES_ACTIVE=local,STOCKOPS_MQTT_INGESTION_ENABLED=false,STOCKOPS_ANALYTICS_ENABLED=false,STOCKOPS_AI_ENABLED=false
```

생성 자체는 성공했으나 초기 API 요청은 `502 Bad Gateway`가 발생했다.

## 분석된 장애 원인

502의 직접 원인은 보안그룹/포트 문제가 아니라, nginx가 upstream 컨테이너의 `8080`에 연결하지 못한 것이다.

로그 근거:

```text
connect() failed (111: Connection refused) while connecting to upstream
upstream: "http://172.17.0.2:8080/actuator/health"
```

즉 EB nginx와 Docker 포트 매핑은 `8080`을 보고 있었지만, Spring Boot 앱이 기동 예외로 내려가고 있었다.

확인된 런타임 예외 순서:

1. `JWT_SECRET` 기본값이 너무 짧아 `WeakKeyException` 발생
2. `local` 프로필의 H2에서 Flyway `V10` SQL 문법 오류 발생
3. Flyway를 끈 뒤 `JavaMailSender` 빈이 없어 `EmailService` 생성 실패

## 마지막으로 적용한 EB 환경변수

마지막 `eb setenv`는 EB에서 성공 처리됐다.

```text
2026-05-12 09:35:48 INFO Environment update is starting.
2026-05-12 09:36:54 INFO Instance deployment completed successfully.
2026-05-12 09:37:32 INFO Successfully deployed new configuration to environment.
```

적용 취지:

- 충분히 긴 `JWT_SECRET` 설정
- 아직 RDS 연결값이 없으므로 `local` 프로필 유지
- 외부 연동성 기능 비활성화
- H2 로컬 기동을 위해 Flyway 비활성화
- JPA `ddl-auto=create` 사용
- `JavaMailSender` 빈 생성을 위해 더미 메일 호스트 설정

현재 설정 방향:

```bash
SPRING_PROFILES_ACTIVE=local
STOCKOPS_MQTT_INGESTION_ENABLED=false
STOCKOPS_ANALYTICS_ENABLED=false
STOCKOPS_AI_ENABLED=false
SPRING_FLYWAY_ENABLED=false
SPRING_JPA_HIBERNATE_DDL_AUTO=create
SPRING_MAIL_HOST=localhost
SPRING_MAIL_PORT=25
```

`JWT_SECRET`는 EB 환경변수에 설정했지만 문서에는 전체 값을 남기지 않는다.

## 다음 재개 시 할 일

1. 필요하면 EB 환경 재생성

```bash
eb create stockops-api-test-v4 \
  --region ap-northeast-2 \
  --envvars SPRING_PROFILES_ACTIVE=local,STOCKOPS_MQTT_INGESTION_ENABLED=false,STOCKOPS_ANALYTICS_ENABLED=false,STOCKOPS_AI_ENABLED=false
```

2. 재생성 후 API 통신 테스트

```bash
curl -i --max-time 20 \
  http://<new-eb-cname>/actuator/health

curl -i --max-time 20 \
  http://<new-eb-cname>/v3/api-docs
```

3. 여전히 502면 로그 확인

```bash
eb logs <new-environment-name> --region ap-northeast-2 > /private/tmp/stockops-eb-tail.log
rg -n "APPLICATION FAILED|Exception|Caused by|ERROR|Started StockOps|Tomcat started|JavaMailSender|Flyway|Hikari" /private/tmp/stockops-eb-tail.log
```

## 비용 정리 이력

2026-05-12 18:40 KST 이후 아래 EB 환경을 종료했다.

- `stockops-api-test`
- `stockops-api-test-v2`
- `stockops-api-test-v3`

검증 결과:

- `aws elasticbeanstalk describe-environments --no-include-deleted`: 활성 환경 없음
- `aws elasticbeanstalk describe-application-versions`: 애플리케이션 버전 없음
- `aws ec2 describe-instances` EB 태그 기준: 실행/중지/종료중 인스턴스 없음
- `aws elbv2 describe-load-balancers`: ALB/NLB 없음
- `aws logs describe-log-groups --log-group-name-prefix /aws/elasticbeanstalk`: EB CloudWatch 로그 그룹 없음

남겨둔 것:

- EB 애플리케이션 껍데기와 로컬 `.elasticbeanstalk` 설정
- IAM 사용자/역할 등 기본 설정 요소

## 운영 배포 전 남은 결정

현재 `stockops-api-test-v3`는 RDS 연결값 없이 `local` 프로필과 H2 메모리 DB로 기동시키는 방향이다. API 통신 확인용으로는 충분하지만 데이터는 영속화되지 않는다.

실제 운영 형태로 전환하려면 아래 값을 EB 환경변수로 추가하고 `prod` 프로필로 배포하는 것이 맞다.

```bash
SPRING_PROFILES_ACTIVE=prod
STOCKOPS_DATASOURCE_URL=...
STOCKOPS_DATASOURCE_USERNAME=...
STOCKOPS_DATASOURCE_PASSWORD=...
```

이 경우 Flyway는 다시 켜야 하며, 커밋된 `analytics` 스키마 생성 수정이 적용된 상태로 마이그레이션을 확인하면 된다.
