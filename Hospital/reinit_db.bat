@echo off
chcp 65001 >nul
REM ============================================================
REM  重建 hospital_db（危险：会删除现有数据）
REM  统一规范：端口 3306 / 库 hospital_db / utf8mb4_unicode_ci
REM  建议先执行 mysqldump 备份：见 docs\MIGRATION_ROLLBACK.md
REM ============================================================
setlocal
set "ROOT=%~dp0"
REM 本地凭据：优先使用已设置的环境变量；否则读取 config\local-secrets.bat（该文件不入库）
if exist "%ROOT%config\local-secrets.bat" call "%ROOT%config\local-secrets.bat"
if "%DB_PORT%"=="" set "DB_PORT=3306"
if "%DB_USER%"=="" set "DB_USER=root"
if "%DB_PASSWORD%"=="" (
    echo [错误] 未配置数据库密码：请设置环境变量 DB_PASSWORD，或将 config\local-secrets.example.bat 复制为 config\local-secrets.bat 并填写。
    exit /b 1
)
set "MYSQL=mysql -h 127.0.0.1 -P %DB_PORT% -u %DB_USER% -p%DB_PASSWORD% --default-character-set=utf8mb4"

echo [0/6] 确认重建？将清空 hospital_db 全部数据。
pause

echo [1/6] 重建库...
%MYSQL% -e "DROP DATABASE IF EXISTS hospital_db; CREATE DATABASE hospital_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
if errorlevel 1 goto fail

echo [2/6] 导入表结构...
%MYSQL% hospital_db < "%ROOT%db\schema\hospital_db_schema.sql"
if errorlevel 1 goto fail

echo [3/6] 放宽单包限制...
%MYSQL% hospital_db -e "SET GLOBAL max_allowed_packet=1073741824;"
if errorlevel 1 goto fail

echo [4/6] 导入基础数据（20261116 快照）...
%MYSQL% hospital_db < "%ROOT%hospital-api\backups\hospital_db_20261116.sql"
if errorlevel 1 goto fail

echo [5/6] 执行迁移（Agent 表 + 排序规则）...
%MYSQL% hospital_db < "%ROOT%db\migration\V2__triage_agent.sql"
if errorlevel 1 goto fail

echo [6/6] 导入多地区演示数据与示例患者...
%MYSQL% hospital_db < "%ROOT%db\seed\demo_regions.sql"
if errorlevel 1 goto fail
%MYSQL% hospital_db -e "INSERT INTO patient (patient_id, name, gender, age, phone, medical_history, id_card, password, create_time) VALUES ('P001', '王鹏睿', '男', 18, '13881897174', '无', '510183200707170000', '123456', NOW()) ON DUPLICATE KEY UPDATE name=VALUES(name), phone=VALUES(phone), password=VALUES(password);"
if errorlevel 1 goto fail

echo 完成。
exit /b 0

:fail
echo [错误] 重建失败，请检查 MySQL 与数据文件。
exit /b 1
