-- =============================================================
-- R1__unify_collation_and_indexes.sql  （V1 回滚脚本）
-- 作用：删除 V1 新增的业务索引；从 hospital_db_archive 还原 3 张遗留表。
-- 幂等：可重复执行。
-- 注意：整库级灾难恢复请使用 backup/hospital_db_*.sql
-- =============================================================
SET NAMES utf8mb4;

-- ---------- 1. 删除 V1 新增索引 ----------
DROP PROCEDURE IF EXISTS `_drop_index`;
DELIMITER $$
CREATE PROCEDURE `_drop_index`(IN tbl VARCHAR(64), IN idx VARCHAR(64))
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.statistics
             WHERE table_schema = 'hospital_db' AND table_name = tbl AND index_name = idx) THEN
    SET @s = CONCAT('ALTER TABLE `hospital_db`.`', tbl, '` DROP INDEX `', idx, '`');
    PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;
  END IF;
END$$
DELIMITER ;

CALL `_drop_index`('schedule', 'idx_schedule_doctor_date');
CALL `_drop_index`('schedule', 'idx_schedule_hospital_date');
CALL `_drop_index`('appointment', 'idx_appointment_patient');
CALL `_drop_index`('appointment', 'idx_appointment_openid');
CALL `_drop_index`('appointment', 'idx_appointment_doctor_date');
CALL `_drop_index`('appointment', 'idx_appointment_schedule');
CALL `_drop_index`('doctor', 'idx_doctor_hospital');
CALL `_drop_index`('doctor', 'idx_doctor_dept');
CALL `_drop_index`('doctor', 'idx_doctor_username');
CALL `_drop_index`('department', 'idx_department_hospital');
CALL `_drop_index`('hospital', 'idx_hospital_region');
CALL `_drop_index`('hospital', 'idx_hospital_status');
CALL `_drop_index`('patient', 'idx_patient_phone');
CALL `_drop_index`('doctor_schedule_setting', 'idx_dss_doctor');
CALL `_drop_index`('emergency_log', 'idx_emergency_log_hospital_time');

DROP PROCEDURE IF EXISTS `_drop_index`;

-- ---------- 2. 从归档库还原遗留表 ----------
DROP PROCEDURE IF EXISTS `_restore_table`;
DELIMITER $$
CREATE PROCEDURE `_restore_table`(IN src VARCHAR(64))
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables
             WHERE table_schema = 'hospital_db_archive' AND table_name = src)
     AND NOT EXISTS (SELECT 1 FROM information_schema.tables
                     WHERE table_schema = 'hospital_db' AND table_name = src) THEN
    SET @s = CONCAT('CREATE TABLE `hospital_db`.`', src, '` LIKE `hospital_db_archive`.`', src, '`');
    PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;
    SET @s = CONCAT('INSERT INTO `hospital_db`.`', src, '` SELECT * FROM `hospital_db_archive`.`', src, '`');
    PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;
  END IF;
END$$
DELIMITER ;

-- 注意顺序：先还原被引用的 user，再还原 registration_attachment
CALL `_restore_table`('user');
CALL `_restore_table`('registration_attachment');
CALL `_restore_table`('emergency_login_log');
DROP PROCEDURE IF EXISTS `_restore_table`;
