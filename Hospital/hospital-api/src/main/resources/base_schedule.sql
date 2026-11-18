-- 为所有医生生成保底排班
-- 每个医生每天设置两个1小时的排班时段：上午9:00-10:00，下午14:00-15:00
-- 这些排班作为保底时间，后续可更换，但保底时间始终可预约

-- 清空现有排班（可选）
-- TRUNCATE TABLE schedule;

-- 创建存储过程来生成保底排班
DELIMITER //

DROP PROCEDURE IF EXISTS GenerateBaseSchedule //

CREATE PROCEDURE GenerateBaseSchedule(IN days INT)
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_doctor_id VARCHAR(50);
    DECLARE v_hospital_id VARCHAR(50);
    DECLARE i INT DEFAULT 0;
    
    -- 声明游标
    DECLARE doctor_cursor CURSOR FOR 
        SELECT doctor_id, hospital_id FROM doctor WHERE status = 1;
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    OPEN doctor_cursor;
    
    read_loop: LOOP
        FETCH doctor_cursor INTO v_doctor_id, v_hospital_id;
        
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        -- 为每个医生生成未来days天的排班
        SET i = 0;
        WHILE i < days DO
            -- 上午9:00-10:00
            INSERT IGNORE INTO schedule (schedule_id, hospital_id, doctor_id, work_date, time_slot, max_patients, booked_count, status, version)
            VALUES (
                CONCAT('SCH_', v_doctor_id, '_', DATE_FORMAT(CURDATE() + INTERVAL i DAY, '%Y%m%d'), '_AM'),
                v_hospital_id,
                v_doctor_id,
                CURDATE() + INTERVAL i DAY,
                '9:00-10:00',
                10,
                0,
                1,
                0
            );
            
            -- 下午14:00-15:00
            INSERT IGNORE INTO schedule (schedule_id, hospital_id, doctor_id, work_date, time_slot, max_patients, booked_count, status, version)
            VALUES (
                CONCAT('SCH_', v_doctor_id, '_', DATE_FORMAT(CURDATE() + INTERVAL i DAY, '%Y%m%d'), '_PM'),
                v_hospital_id,
                v_doctor_id,
                CURDATE() + INTERVAL i DAY,
                '14:00-15:00',
                10,
                0,
                1,
                0
            );
            
            SET i = i + 1;
        END WHILE;
        
    END LOOP;
    
    CLOSE doctor_cursor;
END //

DELIMITER ;

-- 调用存储过程生成未来7天的保底排班
CALL GenerateBaseSchedule(7);

-- 删除存储过程
DROP PROCEDURE IF EXISTS GenerateBaseSchedule;

SELECT '保底排班生成完成' AS result;