-- =============================================================
-- V7：就诊凭证与报到
-- 1) appointment 增加排队号与报到时间
-- 2) 新增凭证使用记录表（一次性使用，主键为凭证随机数）
-- =============================================================

ALTER TABLE appointment
    ADD COLUMN queue_no VARCHAR(16) NULL COMMENT '报到排队号',
    ADD COLUMN checkin_time DATETIME NULL COMMENT '报到时间';

CREATE TABLE appointment_voucher_use (
    voucher_nonce  VARCHAR(32) NOT NULL COMMENT '凭证随机数（一次性）',
    appointment_id VARCHAR(32) NOT NULL COMMENT '预约ID',
    doctor_id      VARCHAR(20) NULL COMMENT '验码医生',
    used_at        DATETIME NOT NULL COMMENT '使用时间',
    PRIMARY KEY (voucher_nonce),
    KEY idx_avu_appointment (appointment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='就诊凭证使用记录';
