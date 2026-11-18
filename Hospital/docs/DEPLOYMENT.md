# 部署手册（DEPLOYMENT）

版本：v1.0　适用：Windows Server / Windows 10+，内网部署

---

## 1. 环境要求

| 组件 | 版本 | 校验命令 |
|---|---|---|
| JDK | 17（开发用 17.0.17；交付包内置 JRE） | `java -version` |
| Maven | 3.9+（仅构建需要） | `mvn -v` |
| Node.js | 构建/校验脚本需要（本仓库无前端构建工程，可不装） | `node -v` |
| Python | 3.10+（Flask 3.x） | `python --version` |
| MySQL | 8.0+ | `mysql --version` |
| 端口 | 8080、5000、3306 可用 | 见 §6 |

---

## 2. 部署拓扑

```
浏览器 / 小程序 ──▶ 8080 hospital-api ──▶ 3306 MySQL
                          └──▶ 127.0.0.1:5000 hospital-crawler ──▶ 高德 API
```

单机部署。爬虫与 MySQL 不对外暴露。

---

## 3. 首次部署步骤

### 3.1 数据库

```powershell
# 1) 建库并导入权威结构
mysql -h 127.0.0.1 -P 3306 -u root -p -e "CREATE DATABASE IF NOT EXISTS hospital_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -h 127.0.0.1 -P 3306 -u root -p hospital_db < db\schema\hospital_db_schema.sql

# 2) 导入基础数据（与当前表结构一致的历史快照）
mysql -h 127.0.0.1 -P 3306 -u root -p --default-character-set=utf8mb4 hospital_db < hospital-api\backups\hospital_db_20261116.sql

# 3) 执行结构迁移（Agent 表 + 剩余排序规则统一）
mysql -h 127.0.0.1 -P 3306 -u root -p hospital_db < db\migration\V2__triage_agent.sql

# 4) 导入多地区演示数据（成都 / 邛崃 / 西安周边；虚构机构，仅演示用）
mysql -h 127.0.0.1 -P 3306 -u root -p --default-character-set=utf8mb4 hospital_db < db\seed\demo_regions.sql

# 5) 旧库补统一迁移（仅当未执行过 V1 时）
mysql -h 127.0.0.1 -P 3306 -u root -p hospital_db < db\migration\V1__unify_collation_and_indexes.sql
```

执行后必须确认 `triage_session`、`triage_lock` 两张表存在；否则分诊 Agent 会返回“分诊服务暂时不可用”。

`db/seed/demo_regions.sql` 可重复执行，ID 均以 `DEMO_` 开头，便于正式合作后整体清除演示数据。

也可直接执行 `init_db.bat`（读取 `DB_PORT`/`DB_USER`/`DB_PASSWORD` 环境变量；未设置时从 `config\local-secrets.bat` 读取）。

数据库密码统一通过环境变量 `DB_PASSWORD` 注入，仓库内不含任何明文密码。本地首次运行前：

```powershell
copy config\local-secrets.example.bat config\local-secrets.bat
# 编辑 local-secrets.bat 填入密码（该文件已被 .gitignore 排除）
```

- 根目录 `init_db.bat`、`reinit_db.bat`、`import_schedule.bat` 会自动读取 `config\local-secrets.bat`；该文件不存在时必须先设置 `DB_PASSWORD` 环境变量
- Python 维护脚本读取环境变量 `DB_PASSWORD`，或 `config/db_password.txt`（不入库）
- 后端本地敏感项放在 `hospital-api/config/application.yml`（不入库），模板见 `hospital-api/config/application.yml.example`

本地实例数据目录为 `d:\Hospital\Hospital\data`（3306，库 `hospital_db`，`utf8mb4_unicode_ci`）；交付包内置实例数据目录为 `delivery\data`（同样 3306）。

### 3.2 后端

```powershell
cd hospital-api
mvn -DskipTests package
java -jar target\hospital-api-0.0.1-SNAPSHOT.jar
```

生产建议显式传入环境变量（避免默认密码）：

```powershell
$env:DB_HOST="127.0.0.1"; $env:DB_PORT="3306"; $env:DB_NAME="hospital_db"
$env:DB_USER="root"; $env:DB_PASSWORD="<你的数据库密码>"
$env:CRAWLER_BASE_URL="http://127.0.0.1:5000"
java -jar target\hospital-api-0.0.1-SNAPSHOT.jar
```

### 3.3 爬虫

```powershell
cd hospital-crawler
pip install -r requirements.txt      # flask requests
python app.py
```

默认监听 `127.0.0.1:5000`；可用 `CRAWLER_HOST` / `CRAWLER_PORT` / `AMAP_KEY` 覆盖。

### 3.4 小程序

1. 微信开发者工具导入 `hospital-miniprogram`。
2. 确认 `config/index.js` 的 `apiBase`（默认 `http://localhost:8080`）。
3. 真机调试：控制台执行 `wx.setStorageSync('API_BASE','http://<内网IP>:8080')`。
4. 上传前在「详情 → 本地设置」勾选「不校验合法域名」，生产需在微信后台配置 8080 域名的 request 合法域名。

---

## 4. 一键交付启动（delivery）

```powershell
cd delivery
start-all.bat      # 依次启动 MySQL → 爬虫 → API，并等待就绪
stop-all.bat       # 按端口精确停止
```

端口与数据库账号只改 `delivery\config\env.bat`。

`start.bat`、`start_8081.bat`、`start_final.bat`、`start_simple.bat`、`rebuild_and_start.bat`、`start.ps1` 均已废弃，仅为兼容旧习惯保留（内部转调 `start-all.bat`）。

---

## 5. 启动顺序与健康检查

| 顺序 | 服务 | 就绪判据 |
|---|---|---|
| 1 | MySQL | `mysql -e "SELECT 1"` 返回 1 |
| 2 | hospital-crawler | `GET http://127.0.0.1:5000/api/health` → `status=ok` |
| 3 | hospital-api | `GET http://localhost:8080/api/hospitals` → `200` |

```powershell
# 一键健康检查
Invoke-WebRequest http://localhost:8080/api/hospitals -UseBasicParsing | Select-Object StatusCode
Invoke-WebRequest http://localhost:8080/api/nearby/health -UseBasicParsing | Select-Object -Expand Content
Invoke-WebRequest http://localhost:8080/ -UseBasicParsing | Select-Object StatusCode
```

预期：`/api/hospitals` 200；`/api/nearby/health` 中 `status=up`；首页 200（加载 `/css/app.css`、`/js/app.js`）。

---

## 6. 端口占用排查

```powershell
netstat -ano | findstr :8080
Get-Process -Id <PID> | Select-Object Id,ProcessName,Path
taskkill /f /pid <PID>
```

---

## 7. 常见故障

| 现象 | 原因 | 处理 |
|---|---|---|
| 启动报 `Could not resolve placeholder 'crawler.base-url'` | 仍在使用旧 `application.properties` | 删除旧配置，确认 `application.yml` 存在且 jar 为最新构建 |
| 启动报 `Access denied for user 'root'` | 密码与实例不一致 | 设置 `DB_PASSWORD` 环境变量 |
| `/api/nearby/hospitals` 返回 `success=false` | 爬虫未启动 | 启动爬虫；查 `backup/crawler-err.log` |
| 周边医院返回 500 | 爬虫内部异常 | 查看爬虫 stderr；已修复 `DEFAULT_HOSPITALS` 缺 `longitude` 的历史缺陷 |
| 首页样式丢失 | `/css/app.css` 404 | 确认 `static/css/`、`static/js/` 已打包进 jar；重新 `mvn package` |
| 小程序接口 404 | `apiBase` 指向旧 8081 | 清除 `API_BASE` 缓存或改 `config/index.js` |
| 管理端页面跳转失败 | 分包路径未更新 | 参考 `docs/MIGRATION_ROLLBACK.md` §4 校验 `subpackages/admin` 路径 |

---

## 8. 日志位置

| 服务 | 位置 |
|---|---|
| hospital-api | 控制台标准输出（生产建议重定向：`java -jar app.jar > api.log 2>&1`） |
| hospital-crawler | 控制台标准输出/标准错误 |
| 本次重构验证记录 | `backup/api-run.log`、`backup/crawler-out.log`、`backup/crawler-err.log` |
