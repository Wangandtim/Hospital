-- 医院表
CREATE TABLE IF NOT EXISTS hospital (
    id VARCHAR(32) PRIMARY KEY COMMENT '医院ID',
    name VARCHAR(200) NOT NULL COMMENT '医院名称',
    address VARCHAR(500) COMMENT '地址',
    phone VARCHAR(50) COMMENT '联系电话',
    daily_key VARCHAR(20) COMMENT '当日管理密钥',
    daily_key_expire DATETIME COMMENT '密钥过期时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    status TINYINT DEFAULT 1 COMMENT '状态：0禁用，1启用'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医院表';

-- 用户表
CREATE TABLE IF NOT EXISTS user (
    id VARCHAR(32) PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(100) UNIQUE NOT NULL COMMENT '用户名',
    password VARCHAR(200) NOT NULL COMMENT '密码（BCrypt加密）',
    real_name VARCHAR(100) COMMENT '真实姓名',
    phone VARCHAR(50) COMMENT '手机号',
    role VARCHAR(50) NOT NULL COMMENT '角色：super_admin, hospital_admin',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态：pending, active, rejected',
    hospital_id VARCHAR(32) COMMENT '所属医院ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 科室表
CREATE TABLE IF NOT EXISTS department (
    id VARCHAR(32) PRIMARY KEY COMMENT '科室ID',
    hospital_id VARCHAR(32) NOT NULL COMMENT '医院ID',
    name VARCHAR(100) NOT NULL COMMENT '科室名称',
    code VARCHAR(50) UNIQUE COMMENT '科室编码',
    head VARCHAR(100) COMMENT '负责人',
    phone VARCHAR(50) COMMENT '联系电话',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    status TINYINT DEFAULT 1 COMMENT '状态：0禁用，1启用',
    FOREIGN KEY (hospital_id) REFERENCES hospital(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='科室表';

-- 注册附件表
CREATE TABLE IF NOT EXISTS registration_attachment (
    id VARCHAR(32) PRIMARY KEY COMMENT '附件ID',
    user_id VARCHAR(32) NOT NULL COMMENT '用户ID',
    file_name VARCHAR(200) COMMENT '文件名',
    file_path VARCHAR(500) COMMENT '文件路径',
    file_type VARCHAR(50) COMMENT '文件类型',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='注册附件表';

-- 应急登录日志表
CREATE TABLE IF NOT EXISTS emergency_login_log (
    id VARCHAR(32) PRIMARY KEY COMMENT '日志ID',
    hospital_id VARCHAR(32) NOT NULL COMMENT '医院ID',
    hospital_name VARCHAR(200) COMMENT '医院名称',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    login_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    token_expire DATETIME COMMENT 'Token过期时间',
    status VARCHAR(20) DEFAULT 'success' COMMENT '状态：success, failed'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应急登录日志表';

-- 插入超级管理员
INSERT INTO user (id, username, password, real_name, role, status) VALUES 
('admin001', 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjzqAKL9xL5jvMFVdNJHvGCgTq/VEq', '超级管理员', 'super_admin', 'active');

-- 插入示例医院数据
INSERT INTO hospital (id, name, address, phone) VALUES 
('H001', '邛崃市医疗中心医院', '四川省成都市邛崃市文君街道文昌街70号', '028-88791120'),
('H002', '西安交通大学第一附属医院', '陕西省西安市雁塔区雁塔西路277号', '029-85323201'),
('H003', '西京医院', '陕西省西安市新城区长乐西路127号', '029-84775500'),
('H004', '陕西省人民医院', '陕西省西安市碑林区友谊西路256号', '029-85276000'),
('H005', '四川大学华西医院', '四川省成都市武侯区国学巷37号', '028-85422114'),
('H006', '西安医学院第二附属医院', '陕西省西安市新城区尚勤路3号', '029-87679360'),
('H007', '西安市中心医院', '陕西省西安市新城区西五路161号', '029-87218536'),
('H008', '西安市第一医院', '陕西省西安市碑林区南大街2号', '029-87252000'),
('H009', '西安市第三医院', '陕西省西安市未央区凤城三路东段10号', '029-61810000'),
('H010', '西北妇女儿童医院', '陕西省西安市