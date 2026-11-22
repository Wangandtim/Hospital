-- =============================================================
-- V5：医生账号与介绍内容
-- 1) doctor 增加工号（可空，按医院唯一；不强制回填、不伪造）
-- 2) doctor_detail 增加学术任职
-- =============================================================

ALTER TABLE doctor
    ADD COLUMN doctor_no VARCHAR(32) NULL COMMENT '医院医生工号（可空）';

ALTER TABLE doctor
    ADD UNIQUE KEY uk_doctor_hospital_no (hospital_id, doctor_no);

ALTER TABLE doctor_detail
    ADD COLUMN academic_appointments TEXT NULL COMMENT '学术任职';

-- 3) 管理端建号统一初始密码时，首登强制改密
ALTER TABLE doctor
    ADD COLUMN must_change_password TINYINT NOT NULL DEFAULT 0 COMMENT '1=首次登录需改密';
