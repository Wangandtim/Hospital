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

with open(os.path.join('hospital-api', 'src', 'main', 'resources', 'data_new.sql'), 'r', encoding='utf-8') as f:
    content = f.read()

lines = content.split('\n')
schedule_lines = []
in_schedule_section = False

for line in lines:
    if '-- 排班数据' in line:
        in_schedule_section = True
        continue
    if in_schedule_section and line.startswith('INSERT INTO schedule'):
        schedule_lines.append(line)

print(f"找到 {len(schedule_lines)} 条排班数据")

batch_size = 100
total_imported = 0

for i in range(0, len(schedule_lines), batch_size):
    batch = schedule_lines[i:i+batch_size]
    batch_sql = '\n'.join(batch)
    
    result = subprocess.run(
        [mysql_path, '--default-character-set=utf8mb4', '-u', db_user, f'-p{db_password}', '-P', db_port, '-D', 'hospital_db'],
        input=batch_sql,
        capture_output=True,
        text=True,
        encoding='utf-8'
    )
    
    if result.returncode != 0:
        print(f"批次 {i//batch_size + 1} 导入失败: {result.stderr[:200]}")
        break
    
    total_imported += len(batch)
    if total_imported % 5000 == 0:
        print(f"已导入 {total_imported} 条...")

print(f"完成！共导入 {total_imported} 条排班数据")