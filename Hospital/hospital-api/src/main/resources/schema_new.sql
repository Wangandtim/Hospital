CREATE TABLE IF NOT EXISTS hospital (
    hospital_id VARCHAR(32) PRIMARY KEY COMMENT '医院ID',
    hospital_name VARCHAR(200) NOT NULL COMMENT '医院名称',
    address VARCHAR(500) COMMENT '地址',
    phone VARCHAR(50) COMMENT '联系电话',
    level VARCHAR(50) COMMENT '医院等级',
    description TEXT COMMENT '医院描述',
    flagship_dept TEXT COMMENT '王牌科室',
    website_url VARCHAR(200) COMMENT '官网地址',
    latitude DOUBLE COMMENT '纬度',
    longitude DOUBLE COMMENT '经度',
    status INT DEFAULT 1 COMMENT '状态：0禁用，1启用',
    create_time DATETIME COMMENT '创建时间',
    province VARCHAR(100) COMMENT '省份',
    city VARCHAR(100) COMMENT '城市',
    district VARCHAR(100) COMMENT '区县'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医院表';

CREATE TABLE IF NOT EXISTS department (
    id VARCHAR(64) PRIMARY KEY COMMENT '科室ID',
    hospital_id VARCHAR(32) NOT NULL COMMENT '医院ID',
    name VARCHAR(100) NOT NULL COMMENT '科室名称',
    description VARCHAR(500) COMMENT '科室描述',
    diseases TEXT COMMENT '诊治疾病',
    service_time VARCHAR(100) COMMENT '服务时间',
    is_flagship INT DEFAULT 0 COMMENT '是否王牌科室：0否，1是',
    create_time DATETIME COMMENT '创建时间',
    FOREIGN KEY (hospital_id) REFERENCES hospital(hospital_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='科室表';

CREATE TABLE IF NOT EXISTS doctor (
    doctor_id VARCHAR(32) PRIMARY KEY COMMENT '医生ID',
    hospital_id VARCHAR(32) NOT NULL COMMENT '医院ID',
    name VARCHAR(100) NOT NULL COMMENT '医生姓名',
    gender VARCHAR(10) COMMENT '性别',
    dept_id VARCHAR(64) COMMENT '科室ID',
    title VARCHAR(50) COMMENT '职称',
    expertise VARCHAR(500) COMMENT '专长',
    phone VARCHAR(50) COMMENT '联系电话',
    education TEXT COMMENT '教育背景',
    moral TEXT COMMENT '医德评价',
    experience TEXT COMMENT '工作经历',
    status INT DEFAULT 1 COMMENT '状态：0禁用，1启用',
    consultation_fee INT DEFAULT 30 COMMENT '挂号费',
    username VARCHAR(100) COMMENT '用户名',
    password VARCHAR(200) COMMENT '密码',
    FOREIGN KEY (hospital_id) REFERENCES hospital(hospital_id) ON DELETE CASCADE,
    FOREIGN KEY (dept_id) REFERENCES department(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生表';

CREATE TABLE IF NOT EXISTS schedule (
    schedule_id VARCHAR(100) PRIMARY KEY COMMENT '排班ID',
    hospital_id VARCHAR(32) NOT NULL COMMENT '医院ID',
    doctor_id VARCHAR(32) NOT NULL COMMENT '医生ID',
    work_date DATE NOT NULL COMMENT '工作日期',
    time_slot VARCHAR(50) NOT NULL COMMENT '时间段',
    max_patients INT DEFAULT 20 COMMENT '最大预约人数',
    booked_count INT DEFAULT 0 COMMENT '已预约人数',
    status INT DEFAULT 1 COMMENT '状态：0禁用，1启用',
    version INT DEFAULT 0 COMMENT '版本号',
    FOREIGN KEY (hospital_id) REFERENCES hospital(hospital_id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id) REFERENCES doctor(doctor_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排班表';

CREATE TABLE IF NOT EXISTS appointment (
    appointment_id VARCHAR(64) PRIMARY KEY COMMENT '预约ID',
    patient_id VARCHAR(32) COMMENT '患者ID',
    openid VARCHAR(100) COMMENT '微信OpenID',
    schedule_id VARCHAR(100) NOT NULL COMMENT '排班ID',
    doctor_id VARCHAR(32) NOT NULL COMMENT '医生ID',
    appointment_date DATE NOT NULL COMMENT '预约日期',
    time_slot VARCHAR(50) COMMENT '时间段',
    status INT DEFAULT 1 COMMENT '状态：0已取消，1待就诊，2已完成，3进行中',
    create_time DATETIME COMMENT '创建时间',
    cancel_time DATETIME COMMENT '取消时间',
    questionnaire_data TEXT COMMENT '问卷数据',
    FOREIGN KEY (schedule_id) REFERENCES schedule(schedule_id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id) REFERENCES doctor(doctor_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约表';

CREATE TABLE IF NOT EXISTS patient (
    id VARCHAR(32) PRIMARY KEY COMMENT '患者ID',
    patient_id VARCHAR(32) COMMENT '患者编号',
    name VARCHAR(100) NOT NULL COMMENT '患者姓名',
    gender VARCHAR(10) COMMENT '性别',
    age INT COMMENT '年龄',
    phone VARCHAR(50) COMMENT '手机号',
    medical_history TEXT COMMENT '病历信息',
    id_card VARCHAR(50) COMMENT '身份证号',
    password VARCHAR(200) COMMENT '密码',
    create_time DATETIME COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='患者表';