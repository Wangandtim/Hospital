-- =============================================================
-- 分诊预约 Agent 表结构（第一版）
-- 执行：mysql -uroot hospital_db < triage_v1.sql
-- 说明：项目 ddl-auto=none，新表必须手工建。
-- =============================================================

CREATE TABLE IF NOT EXISTS triage_session (
  triage_id VARCHAR(36) NOT NULL COMMENT '会话ID',
  patient_id VARCHAR(36) NOT NULL COMMENT '令牌归属患者',
  subject VARCHAR(12) NOT NULL DEFAULT 'SELF' COMMENT 'SELF/ELDER',
  mode VARCHAR(12) NOT NULL DEFAULT 'STANDARD' COMMENT 'STANDARD/ELDER',
  raw_input TEXT COMMENT '原始症状',
  normalized_input VARCHAR(512) COMMENT '归一化文本',
  structured_symptoms TEXT COMMENT '结构化症状JSON',
  red_flag TINYINT NOT NULL DEFAULT 0,
  red_flag_pattern VARCHAR(64) COMMENT '命中红旗规则',
  red_flag_reason VARCHAR(255) COMMENT '红旗原因',
  age INT COMMENT '本次分诊年龄',
  age_band VARCHAR(12) COMMENT 'CHILD/YOUNG/MIDDLE/ELDER',
  gender VARCHAR(8) COMMENT 'MALE/FEMALE',
  pregnant VARCHAR(12) COMMENT 'YES/NO/UNKNOWN',
  medical_history VARCHAR(512) COMMENT '既往史快照',
  dept_name VARCHAR(64) COMMENT '推荐科室',
  confidence DECIMAL(4,3) COMMENT '置信度0-1',
  confidence_tier VARCHAR(12) COMMENT 'HIGH/MEDIUM/LOW/RED_FLAG',
  need_expert VARCHAR(16) COMMENT 'REQUIRED/RECOMMENDED/NOT_NEEDED',
  expert_reason VARCHAR(255) COMMENT '专家判定依据',
  hospital_id VARCHAR(36),
  doctor_id VARCHAR(36),
  schedule_id VARCHAR(36),
  match_score INT,
  match_reason VARCHAR(512),
  alternatives TEXT COMMENT '降级备选JSON',
  evidence TEXT COMMENT '规则命中依据JSON',
  status VARCHAR(16) NOT NULL DEFAULT 'ASSESSED' COMMENT 'NEED_PROFILE/ASSESSED/CONFIRMED/REJECTED/LOCKED/BOOKED/FAILED',
  rule_version VARCHAR(32),
  create_time DATETIME NOT NULL,
  update_time DATETIME,
  confirm_time DATETIME,
  book_time DATETIME,
  PRIMARY KEY (triage_id),
  KEY idx_triage_session_patient (patient_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分诊会话';

CREATE TABLE IF NOT EXISTS triage_lock (
  lock_id VARCHAR(36) NOT NULL,
  triage_id VARCHAR(36) NOT NULL,
  patient_id VARCHAR(36),
  schedule_id VARCHAR(36),
  doctor_id VARCHAR(36),
  hospital_id VARCHAR(36),
  dept_id VARCHAR(36),
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1锁定 2已出单 0已释放/过期',
  expire_at DATETIME NOT NULL,
  release_reason VARCHAR(64),
  appointment_id VARCHAR(36),
  create_time DATETIME NOT NULL,
  release_time DATETIME,
  PRIMARY KEY (lock_id),
  KEY idx_triage_lock_triage (triage_id, status),
  KEY idx_triage_lock_patient (patient_id, status),
  KEY idx_triage_lock_expire (status, expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分诊号源锁';
