-- 应急通道改造：申请即开通 + 事后稽核（一次性迁移，部署前手工执行）
ALTER TABLE emergency_request
  ADD COLUMN doctor_title        VARCHAR(50)  NULL,
  ADD COLUMN cert_id_path        VARCHAR(255) NULL,
  ADD COLUMN cert_license_path   VARCHAR(255) NULL,
  ADD COLUMN cert_verified       INT NOT NULL DEFAULT 0,
  ADD COLUMN cert_verified_by    VARCHAR(40)  NULL,
  ADD COLUMN cert_verified_time  DATETIME     NULL,
  ADD COLUMN cert_verify_remark  VARCHAR(255) NULL,
  ADD COLUMN auto_grant          INT NOT NULL DEFAULT 1,
  ADD COLUMN report_scope        TEXT         NULL,
  ADD COLUMN report_impact       TEXT         NULL,
  ADD COLUMN report_complete     INT NOT NULL DEFAULT 0,
  ADD COLUMN report_time         DATETIME     NULL,
  ADD COLUMN report_deadline     DATETIME     NULL,
  ADD COLUMN confirm_deadline    DATETIME     NULL,
  ADD COLUMN confirm_by          VARCHAR(40)  NULL,
  ADD COLUMN confirm_by_name     VARCHAR(80)  NULL,
  ADD COLUMN confirm_time        DATETIME     NULL,
  ADD COLUMN confirm_remark      VARCHAR(255) NULL,
  ADD COLUMN end_time            DATETIME     NULL,
  ADD COLUMN end_reason          VARCHAR(255) NULL;

ALTER TABLE emergency_request
  ADD KEY idx_er_status_expire (status, token_expire_time),
  ADD KEY idx_er_status_report (status, report_deadline),
  ADD KEY idx_er_status_confirm (status, confirm_deadline);

-- 旧审批流遗留数据统一关闭，避免与新状态语义冲突
UPDATE emergency_request
   SET status = 5,
       end_time = NOW(),
       end_reason = '旧审批流遗留，已停用'
 WHERE status IN (0, 1, 2, 3)
   AND end_time IS NULL;

CREATE TABLE IF NOT EXISTS emergency_operation (
  id          VARCHAR(40)  NOT NULL,
  request_id  VARCHAR(40)  NULL,
  hospital_id VARCHAR(20)  NULL,
  doctor_id   VARCHAR(40)  NULL,
  token       VARCHAR(80)  NULL,
  method      VARCHAR(10)  NULL,
  path        VARCHAR(255) NULL,
  target_type VARCHAR(20)  NULL,
  target_id   VARCHAR(40)  NULL,
  summary     VARCHAR(255) NULL,
  create_time DATETIME     NULL,
  PRIMARY KEY (id),
  KEY idx_eo_request (request_id, create_time),
  KEY idx_eo_token (token, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
