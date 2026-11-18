@echo off
REM =============================================================
REM  本地凭据模板（不提交到仓库）
REM
REM  用法：复制为同目录下的 local-secrets.bat 并填入自己的值。
REM      copy local-secrets.example.bat local-secrets.bat
REM  根目录的 init_db.bat / reinit_db.bat / import_schedule.bat 会自动调用它。
REM
REM  不创建该文件也可以：直接在执行脚本前设置同名环境变量即可。
REM =============================================================

set "DB_HOST=localhost"
set "DB_PORT=3306"
set "DB_USER=root"
set "DB_PASSWORD=<你的数据库密码>"
