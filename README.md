# RealTeeth Project: Reliable Image Processing System

본 프로젝트는 대규모 트래픽 환경에서도 안정적으로 동작하며, 메시지 발행 및 작업 처리를 보장하는 **Transactional Outbox Pattern** 기반의 이미지 처리 시스템입니다.

---

## 1. 프로젝트 아키텍처 (Architecture)

본 프로젝트는 **헥사고날 아키텍처(Hexagonal Architecture)**를 채택하여 비즈니스 로직과 외부 인프라를 엄격히 분리하였습니다.

### 모듈 구성 (Multi-Module)
- `modules/domain`: 업무 핵심 로직 및 엔티티 (`ImageJob`, `OutboxEvent`) 정의
- `modules/application`: 비즈니스 UseCase 인터페이스 및 서비스 레이어 구현
- `modules/adapter-in-api`: REST API 진입점 (`RestController`)
- `modules/adapter-out-persistence`: JPA 기반 데이터베이스 접근 영속성 어댑터
- `modules/adapter-out-worker`: 외부 이미지 처리 Worker와의 통신 어댑터 (Mock 연동)
- `modules/adapter-batch`: 아웃박스 이벤트 발행 및 장애 복구를 위한 스케줄러

---

## 2. 실행 방법 (How to Run)

### 2.1 사전 요구사항
- Java 21
- Docker & Docker Compose

### 2.2 인프라 실행
```bash
docker-compose up -d
```
> MySQL(3306)과 RabbitMQ(5672)가 실행됩니다. `init.sql`에 의해 초기 테이블이 생성됩니다.

### 2.3 어플리케이션 빌드 및 실행
```bash
./gradlew clean build
./gradlew :modules:adapter-in-api:bootRun
```

---

## 3. 설계 상세 (Design Specification)

### 3.1 중복 요청 처리 (4.1)
시스템은 동일한 요청이 중복 전달되는 상황을 고려하여 **데이터베이스 수준의 원자적 상태 변경**을 수행합니다.
- `ImageJobJpaRepository`에서 업데이트 시 `WHERE status = :currentStatus` 조건을 포함하여, 한 번 상태가 변경된 작업은 중복 요청이 들어와도 영향을 받지 않도록 설계되었습니다.
- **멱등성 보장**: 이미 터미널 상태(Succeeded, Failed)에 도달한 작업은 다시 처리되지 않도록 도메인 로직에서 제어합니다.

### 3.2 상태 전이 (4.2)
`ImageJob`은 정해진 규칙에 따라서만 상태가 전이됩니다.

- **흐름**: `ACCEPTED` → `PUBLISH_PENDING` → `PUBLISHED` → `DISPATCHING` → `PROCESSING` → (`SUCCEEDED` / `FAILED`)
- **불허 전이**: 한 번 `SUCCEEDED`나 `FAILED`가 된 작업은 어떤 상태로도 변경될 수 없습니다. 또한 `PROCESSING`에서 다시 `PUBLISHED`로 돌아가는 식의 비정상적인 전이는 `ImageJob.transitionTo`에서 차단됩니다.

### 3.3 처리 보장 모델 (4.3)
본 시스템은 **"최소 한 번 이상 전달(At-Least-Once Delivery)"** 모델을 따릅니다.
- **판단 근거**: Transactional Outbox 패턴을 적용하여 DB 트랜잭션과 이벤트 기록을 원자적으로 처리합니다. 메시지 브로커로의 실제 발행은 별도의 스케줄러가 수행하며, 발행에 실패하더라도 성공할 때까지 재시도하므로 유실을 방지합니다. 중복은 3.1의 설계로 방어합니다.

### 3.4 서버 재시작 시 동작 (4.4)
서버가 갑자기 종료되거나 재시작되어도 데이터 정합성은 유지됩니다.
- **복구 메커니즘**: `ImageJobRecoverScheduler`가 주기적으로 `DISPATCHING`이나 `PROCESSING` 상태에서 오랫동안 멈춰있는 작업을 탐색합니다.
- **정합성 경계**: DB 트랜잭션에 의해 각 상태는 기록되어 있으나, 외부 Worker 호출 직후 서버가 죽으면 Worker측에는 작업이 생성되었으나 시스템은 아직 `DISPATCHING`일 수 있습니다. 이 경우 복구 스케줄러가 상태를 대기 상태로 돌리거나 재시도하여 Worker 결과와 동기화합니다.

---

## 4. 설계 의도 및 주요 판단 근거 (Design Rationale)

- **상태 모델 설계 의도**: 작업의 생명주기를 가시화하고, 예외 상황에서도 시스템이 각 단계별로 어디까지 진행되었는지 추적 가능하도록 세분화했습니다.
- **실패 처리 전략**: 외부 시스템 연동 실패 시 최대 **5회의 재시도**를 거친 후 최종 `FAILED` 처리합니다. 일시적인 인프라 장애는 `RecoverScheduler`를 통해 보정합니다.
- **동시 요청 발생 시 고려 사항**: DB의 Atomic Update(Update with Where)를 활용하여 분산 환경에서도 단일 작업을 한 번만 점유(Claiming)하도록 구현했습니다.
- **트래픽 증가 시 병목 가능 지점**:
    - DB의 Outbox 테이블 조회 부하 (인덱스 최적화 및 샤딩 필요성).
    - 단일 복구 스케줄러의 처리 한계 (분산 락을 통한 스케줄러 확장 필요).
- **외부 시스템 연동 방식**: 비동기 폴링(Polling) 방식을 선택했습니다. 외부 Worker의 처리 시간이 길어질 수 있으므로, 커넥션을 잡아두는 대신 Outbox를 통해 작업 생성 지시를 내리고 주기적으로 상태를 확인하는 것이 시스템 안정성에 유리하다고 판단했습니다.
