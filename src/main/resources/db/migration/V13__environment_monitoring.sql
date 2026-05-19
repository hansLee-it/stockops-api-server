-- V13: Environment Monitoring Schema
-- 센서 장치, 환경 제어기, 측정값, 알림, 제어 명령 테이블 추가

-- ============================================
-- 1. SENSOR DEVICES TABLE
-- ============================================
CREATE TABLE sensor_devices (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    location VARCHAR(255) NOT NULL,
    sensor_type VARCHAR(100) NOT NULL,
    external_sensor_id VARCHAR(255) NOT NULL,
    mqtt_topic VARCHAR(500),
    source_channel VARCHAR(100),
    unit VARCHAR(50),
    calibration JSONB,
    noise_sigma DOUBLE PRECISION,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sensor_devices IS '환경 모니터링 센서 장치 마스터';
COMMENT ON COLUMN sensor_devices.id IS '센서 장치 PK';
COMMENT ON COLUMN sensor_devices.name IS '센서 장치 이름';
COMMENT ON COLUMN sensor_devices.location IS '센서가 설치된 위치 설명';
COMMENT ON COLUMN sensor_devices.sensor_type IS '센서 유형 코드 (예: TEMPERATURE, HUMIDITY)';
COMMENT ON COLUMN sensor_devices.external_sensor_id IS '외부 시스템에서 사용하는 센서 식별자';
COMMENT ON COLUMN sensor_devices.mqtt_topic IS '센서 데이터 수신 MQTT 토픽';
COMMENT ON COLUMN sensor_devices.source_channel IS '센서 원본 채널 또는 포트 정보';
COMMENT ON COLUMN sensor_devices.unit IS '센서 기본 측정 단위';
COMMENT ON COLUMN sensor_devices.calibration IS '보정 파라미터 JSON';
COMMENT ON COLUMN sensor_devices.noise_sigma IS '센서 노이즈 표준편차';
COMMENT ON COLUMN sensor_devices.deleted IS '소프트 삭제 여부';
COMMENT ON COLUMN sensor_devices.active IS '재활성화 가능 여부를 포함한 현재 사용 상태';
COMMENT ON COLUMN sensor_devices.created_at IS '생성 시각 (UTC 기준 저장)';
COMMENT ON COLUMN sensor_devices.updated_at IS '수정 시각 (UTC 기준 저장)';

CREATE UNIQUE INDEX IF NOT EXISTS uk_sensor_devices_external_sensor_id_active
    ON sensor_devices (external_sensor_id, deleted);

CREATE INDEX IF NOT EXISTS idx_sensor_devices_deleted
    ON sensor_devices (deleted);

-- ============================================
-- 2. ENVIRONMENT CONTROLLERS TABLE
-- ============================================
CREATE TABLE environment_controllers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    external_controller_id VARCHAR(255) NOT NULL,
    controller_type VARCHAR(50) NOT NULL DEFAULT 'ventilation',
    target_axis VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'INACTIVE',
    output_level INTEGER NOT NULL DEFAULT 0 CHECK (output_level >= 0 AND output_level <= 100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (controller_type IN ('cooling', 'heating', 'humidifying', 'dehumidifying', 'ventilation', 'air_purifier'))
);

COMMENT ON TABLE environment_controllers IS '환경 제어 장치 마스터';
COMMENT ON COLUMN environment_controllers.id IS '환경 제어 장치 PK';
COMMENT ON COLUMN environment_controllers.name IS '제어 장치 이름';
COMMENT ON COLUMN environment_controllers.external_controller_id IS '외부 시스템에서 사용하는 제어기 식별자';
COMMENT ON COLUMN environment_controllers.controller_type IS '제어기 유형 (cooling/heating/humidifying/dehumidifying/ventilation/air_purifier)';
COMMENT ON COLUMN environment_controllers.target_axis IS '제어 대상 축 (예: temperature, humidity, air_quality)';
COMMENT ON COLUMN environment_controllers.status IS '제어기 현재 상태 (INACTIVE, READY, RUNNING, ERROR 등)';
COMMENT ON COLUMN environment_controllers.output_level IS '현재 출력 레벨 (0-100)';
COMMENT ON COLUMN environment_controllers.deleted IS '소프트 삭제 여부';
COMMENT ON COLUMN environment_controllers.active IS '재활성화 가능 여부를 포함한 현재 사용 상태';
COMMENT ON COLUMN environment_controllers.created_at IS '생성 시각 (UTC 기준 저장)';
COMMENT ON COLUMN environment_controllers.updated_at IS '수정 시각 (UTC 기준 저장)';

CREATE UNIQUE INDEX IF NOT EXISTS uk_environment_controllers_external_controller_id_active
    ON environment_controllers (external_controller_id, deleted);

CREATE INDEX IF NOT EXISTS idx_environment_controllers_deleted
    ON environment_controllers (deleted);

-- ============================================
-- 3. SENSOR READINGS TABLE
-- ============================================
CREATE TABLE sensor_readings (
    id BIGSERIAL PRIMARY KEY,
    sensor_device_id BIGINT NOT NULL REFERENCES sensor_devices(id),
    value DOUBLE PRECISION NOT NULL,
    value_kind VARCHAR(50) NOT NULL,
    unit VARCHAR(50),
    status VARCHAR(30) NOT NULL DEFAULT 'NORMAL',
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    sequence_id BIGINT,
    raw_payload JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sensor_readings IS '센서 측정 이력 데이터';
COMMENT ON COLUMN sensor_readings.id IS '센서 측정 이력 PK';
COMMENT ON COLUMN sensor_readings.sensor_device_id IS '측정값을 생성한 센서 장치 ID';
COMMENT ON COLUMN sensor_readings.value IS '측정값';
COMMENT ON COLUMN sensor_readings.value_kind IS '측정값 종류 (예: raw, averaged, compensated)';
COMMENT ON COLUMN sensor_readings.unit IS '측정 단위';
COMMENT ON COLUMN sensor_readings.status IS '측정 상태 (NORMAL, WARN, ERROR 등)';
COMMENT ON COLUMN sensor_readings.recorded_at IS '센서가 측정한 시각 (UTC 기준 저장)';
COMMENT ON COLUMN sensor_readings.sequence_id IS '외부 페이로드 순번';
COMMENT ON COLUMN sensor_readings.raw_payload IS '원본 센서 페이로드 JSON';
COMMENT ON COLUMN sensor_readings.created_at IS '레코드 생성 시각 (UTC 기준 저장)';

CREATE INDEX IF NOT EXISTS idx_sensor_readings_recorded_at
    ON sensor_readings (recorded_at);

CREATE INDEX IF NOT EXISTS idx_sensor_readings_sensor_device_recorded_at
    ON sensor_readings (sensor_device_id, recorded_at);

-- ============================================
-- 3.1 SENSOR LATEST PROJECTION TABLE
-- ============================================
CREATE TABLE sensor_latest (
    sensor_device_id BIGINT PRIMARY KEY REFERENCES sensor_devices(id),
    value DOUBLE PRECISION,
    value_kind VARCHAR(50),
    unit VARCHAR(50),
    status VARCHAR(30),
    recorded_at TIMESTAMP WITH TIME ZONE,
    sequence_id BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sensor_latest IS '센서 최신 상태 프로젝션';
COMMENT ON COLUMN sensor_latest.sensor_device_id IS '센서 장치 ID (PK/FK)';
COMMENT ON COLUMN sensor_latest.value IS '최신 측정값';
COMMENT ON COLUMN sensor_latest.value_kind IS '최신 측정값 종류';
COMMENT ON COLUMN sensor_latest.unit IS '최신 측정 단위';
COMMENT ON COLUMN sensor_latest.status IS '최신 측정 상태';
COMMENT ON COLUMN sensor_latest.recorded_at IS '최신 센서 측정 시각 (UTC 기준 저장)';
COMMENT ON COLUMN sensor_latest.sequence_id IS '최신 외부 페이로드 순번';
COMMENT ON COLUMN sensor_latest.updated_at IS '프로젝션 갱신 시각 (UTC 기준 저장)';

-- ============================================
-- 4. ENVIRONMENT ALERTS TABLE
-- ============================================
CREATE TABLE environment_alerts (
    id BIGSERIAL PRIMARY KEY,
    sensor_device_id BIGINT REFERENCES sensor_devices(id),
    alert_type VARCHAR(100) NOT NULL,
    severity VARCHAR(30) NOT NULL DEFAULT 'INFO',
    message TEXT NOT NULL,
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    acknowledged_by VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE environment_alerts IS '환경 이상 감지 및 운영 알림';
COMMENT ON COLUMN environment_alerts.id IS '환경 알림 PK';
COMMENT ON COLUMN environment_alerts.sensor_device_id IS '관련 센서 장치 ID (시스템 알림은 NULL 가능)';
COMMENT ON COLUMN environment_alerts.alert_type IS '알림 유형 코드';
COMMENT ON COLUMN environment_alerts.severity IS '알림 심각도 (INFO, WARNING, CRITICAL 등)';
COMMENT ON COLUMN environment_alerts.message IS '알림 메시지 본문';
COMMENT ON COLUMN environment_alerts.acknowledged IS '알림 확인 여부';
COMMENT ON COLUMN environment_alerts.acknowledged_at IS '알림 확인 시각 (UTC 기준 저장)';
COMMENT ON COLUMN environment_alerts.acknowledged_by IS '알림 확인자';
COMMENT ON COLUMN environment_alerts.created_at IS '알림 생성 시각 (UTC 기준 저장)';

CREATE INDEX IF NOT EXISTS idx_environment_alerts_created_at
    ON environment_alerts (created_at);

CREATE INDEX IF NOT EXISTS idx_environment_alerts_sensor_device_id
    ON environment_alerts (sensor_device_id);

-- ============================================
-- 5. CONTROLLER COMMANDS TABLE
-- ============================================
CREATE TABLE controller_commands (
    id BIGSERIAL PRIMARY KEY,
    controller_id BIGINT NOT NULL REFERENCES environment_controllers(id),
    requested_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    requested_output_level INTEGER CHECK (requested_output_level >= 0 AND requested_output_level <= 100),
    result_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    result_message TEXT,
    sensimul_response_code VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (result_status IN ('PENDING', 'FORWARDED', 'APPLIED', 'FAILED_RETRYABLE'))
);

COMMENT ON TABLE controller_commands IS '환경 제어기 제어 명령 이력';
COMMENT ON COLUMN controller_commands.id IS '제어 명령 이력 PK';
COMMENT ON COLUMN controller_commands.controller_id IS '명령 대상 제어기 ID';
COMMENT ON COLUMN controller_commands.requested_status IS '요청한 제어 상태';
COMMENT ON COLUMN controller_commands.requested_output_level IS '요청한 출력 레벨 (0-100)';
COMMENT ON COLUMN controller_commands.result_status IS '명령 처리 결과 상태 (PENDING, FORWARDED, SUCCESS, FAILED_RETRYABLE)';
COMMENT ON COLUMN controller_commands.result_message IS '명령 처리 결과 메시지';
COMMENT ON COLUMN controller_commands.sensimul_response_code IS 'Sensimul 응답 코드';
COMMENT ON COLUMN controller_commands.created_at IS '명령 생성 시각 (UTC 기준 저장)';

CREATE INDEX IF NOT EXISTS idx_controller_commands_controller_id_created_at
    ON controller_commands (controller_id, created_at);
