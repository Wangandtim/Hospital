-- 检查并修复schedule表结构
ALTER TABLE schedule ADD COLUMN IF NOT EXISTS work_date DATE NULL;
ALTER TABLE schedule ADD COLUMN IF NOT EXISTS hospital_id VARCHAR(32) NULL;
ALTER TABLE schedule ADD COLUMN IF NOT EXISTS status INT DEFAULT 1;
ALTER TABLE schedule ADD COLUMN IF NOT EXISTS version INT DEFAULT 0;

-- 更新现有数据的work_date字段（如果为空）
UPDATE schedule SET work_date = CURDATE() WHERE work_date IS NULL;
