@echo off
chcp 65001 >nul
REM ============================================================
REM  单独导入排班数据（data_new.sql）
REM  统一规范：端口 3306 / 库 hospital_db
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

mysql -h 127.0.0.1 -P %DB_PORT% -u %DB_USER% -p%DB_PASSWORD% --default-character-set=utf8mb4 -D hospital_db < "%ROOT%hospital-api\src\main\resources\data_new.sql"
if errorlevel 1 (
    echo [错误] 排班数据导入失败
    exit /b 1
)
echo 排班数据导入完成。
