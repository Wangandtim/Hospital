import subprocess
from config.db_secrets import db_password

mysql_path = r"D:\Hospital\Hospital\delivery\mysql-8.0.33-winx64\bin\mysql.exe"

def run_sql(sql):
    return subprocess.run(
        [mysql_path, '--default-character-set=utf8mb4', '-u', 'root', f'-p{db_password()}', '-P', '3306', '-D', 'hospital_db', '-e', sql],
        capture_output=True,
        text=True,
        encoding='utf-8'
    )

result = run_sql("SELECT username, COUNT(*) as cnt FROM doctor GROUP BY username HAVING cnt > 1")
lines = result.stdout.strip().split('\n')[1:]

for line in lines:
    if line.strip():
        parts = line.split('\t')
        if len(parts) >= 2:
            username = parts[0]
            cnt = int(parts[1])
            
            result2 = run_sql(f"SELECT doctor_id FROM doctor WHERE username = '{username}' ORDER BY doctor_id")
            doctor_ids = result2.stdout.strip().split('\n')[1:]
            
            for i, doctor_id in enumerate(doctor_ids, 1):
                if i == 1:
                    new_username = username
                else:
                    new_username = f"{username}{i}"
                
                run_sql(f"UPDATE doctor SET username = '{new_username}' WHERE doctor_id = '{doctor_id.strip()}'")
                print(f"Updated {doctor_id.strip()} to {new_username}")

print("Done!")