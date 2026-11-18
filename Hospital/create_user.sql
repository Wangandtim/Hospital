-- 用法：先把 <YOUR_PASSWORD> 替换为本地数据库密码再执行；真实密码不要提交到仓库。
-- 本地项目脚本统一从 config\local-secrets.bat 或 DB_PASSWORD 环境变量读取密码。
ALTER USER 'root'@'localhost' IDENTIFIED BY '<YOUR_PASSWORD>';
CREATE USER IF NOT EXISTS 'root'@'127.0.0.1' IDENTIFIED BY '<YOUR_PASSWORD>';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'127.0.0.1' WITH GRANT OPTION;
FLUSH PRIVILEGES;