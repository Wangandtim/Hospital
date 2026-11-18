import subprocess
from config.db_secrets import db_password

mysql_path = r"D:\Hospital\Hospital\delivery\mysql-8.0.33-winx64\bin\mysql.exe"

hospital_names = [
    ('H001', '西安交通大学第一附属医院'),
    ('H002', '空军军医大学西京医院'),
    ('H003', '陕西省人民医院'),
    ('H004', '西安医学院第二附属医院'),
    ('H005', '西安市中心医院'),
    ('H006', '西安市第一医院'),
    ('H007', '西安市第三医院'),
    ('H008', '西北妇女儿童医院'),
    ('H009', '西安市第四医院'),
    ('H010', '西安唐都医院'),
    ('H011', '西安高新医院'),
    ('H012', '西安国际医学中心医院'),
    ('H013', '四川大学华西医院'),
    ('H014', '四川省人民医院'),
    ('H015', '成都中医药大学附属医院'),
    ('H016', '成都市第三人民医院'),
    ('H017', '成都军区总医院'),
    ('H018', '成都市第二人民医院'),
    ('H019', '四川省肿瘤医院'),
    ('H020', '成都大学附属医院'),
    ('H021', '四川省妇幼保健院'),
    ('H022', '北京大学第一医院'),
    ('H023', '北京协和医院'),
    ('H024', '中国人民解放军总医院'),
    ('H025', '首都医科大学附属北京天坛医院'),
    ('H026', '首都医科大学附属北京友谊医院'),
    ('H027', '复旦大学附属中山医院'),
    ('H028', '上海交通大学医学院附属瑞金医院'),
    ('H029', '复旦大学附属华山医院'),
    ('H030', '上海交通大学医学院附属仁济医院'),
    ('H031', '中山大学附属第一医院'),
    ('H032', '南方医科大学南方医院'),
    ('H033', '广东省人民医院'),
    ('H034', '广州市第一人民医院'),
    ('H035', '浙江大学医学院附属第一医院'),
    ('H036', '浙江大学医学院附属第二医院'),
    ('H037', '浙江省人民医院'),
    ('H038', '江苏省人民医院'),
    ('H039', '南京鼓楼医院'),
    ('H040', '华中科技大学同济医学院附属同济医院'),
    ('H041', '武汉大学人民医院'),
    ('H042', '中南大学湘雅医院'),
    ('H043', '湖南省人民医院'),
    ('H044', '山东大学齐鲁医院'),
    ('H045', '山东省立医院'),
    ('H046', '郑州大学第一附属医院'),
    ('H047', '河南省人民医院'),
    ('H048', '山西省人民医院'),
    ('H049', '河北医科大学第二医院'),
    ('H050', '安徽省立医院'),
    ('H051', '福建医科大学附属协和医院'),
    ('H052', '南昌大学第一附属医院'),
    ('H053', '广西医科大学第一附属医院'),
    ('H054', '云南省第一人民医院'),
    ('H055', '贵州省人民医院'),
    ('H056', '甘肃省人民医院'),
    ('H057', '新疆医科大学第一附属医院')
]

names = ['张伟', '李强', '王芳', '刘洋', '陈静', '杨帆', '赵明', '黄丽', '周杰', '吴涛',
         '徐敏', '孙燕', '马丽', '朱伟', '胡杰', '郭涛', '何芳', '林强', '罗峰', '高峰',
         '郑伟', '梁芳', '谢敏', '宋杰', '唐丽', '许强', '韩伟', '冯敏', '邓杰', '曹芳']

total_created = 0
for i, (hospital_id, hospital_name) in enumerate(hospital_names, 1):
    admin_id = f"ADMIN{hospital_id[1:]}"
    username = f"hospital_{hospital_id}"
    password = "123456"
    real_name = names[i % len(names)] + "（管理员）"
    
    sql = f"INSERT INTO admin (admin_id, username, password, real_name, hospital_id, role, audit_status, create_time) VALUES ('{admin_id}', '{username}', '{password}', '{real_name}', '{hospital_id}', 'HOSPITAL_ADMIN', 1, NOW())"
    
    result = subprocess.run(
        [mysql_path, '--default-character-set=utf8mb4', '-u', 'root', f'-p{db_password()}', '-P', '3306', '-D', 'hospital_db', '-e', sql],
        capture_output=True,
        text=True
    )
    
    if result.returncode == 0:
        total_created += 1
        if total_created % 10 == 0:
            print(f"已创建 {total_created} 个医院管理员账号")
    else:
        print(f"创建 {hospital_id} 管理员失败: {result.stderr}")

print(f"完成！共创建 {total_created} 个医院管理员账号")