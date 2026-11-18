-- 审计日志表（生产库 ddl-auto = none，需手工执行一次）
CREATE TABLE IF NOT EXISTS audit_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  action VARCHAR(64) NOT NULL,
  target_type VARCHAR(64),
  target_id VARCHAR(64),
  operator_role VARCHAR(32),
  operator_id VARCHAR(64),
  detail VARCHAR(1000),
  ip VARCHAR(64),
  created_at DATETIME NOT NULL,
  INDEX idx_audit_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
