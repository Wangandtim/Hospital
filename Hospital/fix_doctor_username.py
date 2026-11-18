import subprocess
from config.db_secrets import db_password

mysql_path = r"D:\Hospital\Hospital\delivery\mysql-8.0.33-winx64\bin\mysql.exe"

pinyin_map = {
    '张': 'zhang', '李': 'li', '王': 'wang', '刘': 'liu', '陈': 'chen',
    '杨': 'yang', '赵': 'zhao', '黄': 'huang', '周': 'zhou', '吴': 'wu',
    '徐': 'xu', '孙': 'sun', '马': 'ma', '朱': 'zhu', '胡': 'hu',
    '郭': 'guo', '何': 'he', '林': 'lin', '罗': 'luo', '高': 'gao',
    '郑': 'zheng', '梁': 'liang', '谢': 'xie', '宋': 'song', '唐': 'tang',
    '许': 'xu', '韩': 'han', '冯': 'feng', '邓': 'deng', '曹': 'cao',
    '伟': 'wei', '强': 'qiang', '磊': 'lei', '敏': 'min', '静': 'jing',
    '芳': 'fang', '丽': 'li', '杰': 'jie', '涛': 'tao', '军': 'jun',
    '明': 'ming', '华': 'hua', '亮': 'liang', '勇': 'yong', '文': 'wen',
    '燕': 'yan', '霞': 'xia', '峰': 'feng', '刚': 'gang', '平': 'ping',
    '辉': 'hui', '娜': 'na', '鹏': 'peng', '鑫': 'xin', '婷': 'ting',
    '宇': 'yu', '洋': 'yang', '浩': 'hao', '雨': 'yu'
}

result = subprocess.run(
    [mysql_path, '--default-character-set=utf8mb4', '-u', 'root', f'-p{db_password()}', '-P', '3306', '-D', 'hospital_db', '-e', 'SELECT doctor_id, name FROM doctor'],
    capture_output=True,
    text=True,
    encoding='utf-8'
)

lines = result.stdout.strip().split('\n')[1:]

updated_count = 0
for line in lines:
    if line.strip():
        parts = line.split('\t')
        if len(parts) >= 2:
            doctor_id = parts[0]
            name = parts[1]
            
            pinyin_username = ''
            for char in name:
                pinyin_username += pinyin_map.get(char, char)
            
            sql = f"UPDATE doctor SET username = '{pinyin_username}' WHERE doctor_id = '{doctor_id}'"
            result = subprocess.run(
                [mysql_path, '--default-character-set=utf8mb4', '-u', 'root', f'-p{db_password()}', '-P', '3306', '-D', 'hospital_db', '-e', sql],
                capture_output=True,
                text=True
            )
            
            if result.returncode == 0:
                updated_count += 1
                if updated_count % 100 == 0:
                    print(f"已更新 {updated_count} 个医生用户名")

print(f"完成！共更新 {updated_count} 个医生用户名")