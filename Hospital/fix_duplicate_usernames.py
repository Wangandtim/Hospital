import os
import subprocess
from config.db_secrets import db_password as read_db_password

mysql_path = os.environ.get(
    'MYSQL_EXE',
    r"delivery\mysql-8.0.33-winx64\bin\mysql.exe"
)
db_port = os.environ.get('DB_PORT', '3306')
db_user = os.environ.get('DB_USER', 'root')
db_password = read_db_password()

result = subprocess.run(
    [mysql_path, '--default-character-set=utf8mb4', '-u', db_user, f'-p{db_password}', '-P', db_port, '-D', 'hospital_db', '-e', "SELECT username, GROUP_CONCAT(doctor_id ORDER BY doctor_id) as ids FROM doctor GROUP BY username HAVING COUNT(*) > 1"],
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
            username = parts[0]
            doctor_ids = parts[1].split(',')
            
            for i, doctor_id in enumerate(doctor_ids, 1):
                if i == 1:
                    new_username = username
                else:
                    new_username = f"{username}{i}"
                
                sql = f"UPDATE doctor SET username = '{new_username}' WHERE doctor_id = '{doctor_id}'"
                result = subprocess.run(
                    [mysql_path, '--default-character-set=utf8mb4', '-u', db_user, f'-p{db_password}', '-P', db_port, '-D', 'hospital_db', '-e', sql],
                    capture_output=True,
                    text=True
                )
                
                if result.returncode == 0:
                    updated_count += 1

print(f"完成！共更新 {updated_count} 个医生用户名")