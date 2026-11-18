# 医院预约系统 架构重构规格说明（spec）

版本：v1.0　日期：2026-11-11　状态：待评审
适用工程：`d:\Desktop\Hospital`（hospital-api / hospital-crawler / hospital-miniprogram / delivery / MySQL 8.0.45）

---

## 1. 目标与非目标

### 1.1 目标
1. **数据库层统一**：单一 MySQL 实例（`localhost:3306`）、单一业务库（`hospital_db`）、统一字符集 `utf8mb4` / 排序规则 `utf8mb4_unicode_ci`、统一 InnoDB + HikariCP 连接池、统一主键/索引规范，清除遗留表与异构排序规则。
2. **端口统一**：全项目端口集中登记，小程序 / 后台 / 运维脚本只访问 **8080 单入口**；爬虫 5000 仅内网监听并由后端代理；MySQL 3306。清理 8081 / 3308 等分散、错误端口。
3. **前端工程化与性能**：小程序引入统一配置层与请求层、开启按需注入与分包；后台 `static/index.html` 拆分为独立 CSS/JS 资源、视觉按 hallmark 规范重构。
4. **可回滚**：所有变更前有源码与数据库备份，每层提供回滚脚本与回滚步骤。
5. **交付文档**：架构说明、部署手册、迁移与回滚手册、端口登记表、测试验证报告。

### 1.2 非目标（本次不做）
- 不改变任何对外业务字段语义（接口出入参、表字段名、状态机 0/1/2/3 含义）。
- 不引入 Redis / 消息队列 / 微服务拆分。
- 不重写小程序 47 个页面的业务代码，不改变现有页面视觉（仅工程化与配置层改造）。
- 不改动 `untitled/`、`data/`、`delivery/mysql-*`（MySQL 二进制）等无关或第三方内容。

---

## 2. 现状基线（已实测）

| 项 | 现状 |
|---|---|
| 源码/配置 | 无 Git 仓库，改动前需文件备份 |
| 后端 | Spring Boot 3.3.5 / Java 17 / JPA / Security / `spring-boot-starter-data-redis`（无实际使用） |
| 数据库 | 仅 `3306` 实例，库 `hospital_db`，15 张表 |
| 数据量 | hospital 54、doctor 1685、department 547、schedule 116190（18.58MB/0 索引）、appointment 89、patient 7、admin 5、wechat_user 3 |
| 排序规则 | `user`、`registration_attachment`、`emergency_login_log` 为 `utf8mb4_0900_ai_ci`，其余 `utf8mb4_unicode_ci` |
| 索引 | 除主键/唯一键外几乎无业务索引；`appointment`/`doctor`/`department`/`hospital` 无查询索引 |
| 遗留表 | `user`(1 行)、`registration_attachment`(0)、`emergency_login_log`(0) 无实体映射 |
| 端口 | API 8080；爬虫 5000（`0.0.0.0`）；小程序 6 个页面硬编码 `localhost:8081/api`；`nearby` 直连 `localhost:5000`；根目录脚本连 `3308` |
| 配置分散 | `application.properties` + `application-enhanced.properties`（后者 `ddl-auto=update`、`sql.init.mode=always`，与主配置冲突）；`schema.sql` 为过期错误结构 |
| 后台页面 | `static/index.html` 单文件 65KB（内联 CSS 约 100 行、内联 JS 约 820 行） |

### 2.1 已完成的备份（本规格的前置条件，已执行）
- 源码包：`backup/code_20261111_200928.zip`（hospital-api / crawler / miniprogram / delivery 脚本 + python-service + 根目录脚本与文档，已校验可打开）
- 数据库：`backup/hospital_db_20261111_200636.sql`（10.37MB，`--single-transaction --routines --triggers --events`）

---

## 3. 统一规范（重构后）

### 3.1 数据库规范
| 规范项 | 统一值 |
|---|---|
| 实例 / 库 | `localhost:3306` / `hospital_db` |
| 引擎 | InnoDB |
| 字符集 / 排序规则 | `utf8mb4` / `utf8mb4_unicode_ci`（库级、表级、列级三层一致） |
| 主键 | 业务字符串主键（`*_id`、`openid`），保持现状，不改类型 |
| 命名 | 索引 `idx_<表>_<列缩写>`；唯一索引 `uk_<表>_<列>`；外键 `fk_<表>_<父表>` |
| 时间列 | `datetime(6)`；日期列 `date`；禁止用字符串存日期 |
| 连接池 | HikariCP，`maximum-pool-size=20`、`minimum-idle=5`、`connection-timeout=30000`、`max-lifetime=1800000`、`connection-test-query` 不显式设置（用 JDBC4 校验） |
| 连接参数 | `useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true` |
| 归档库 | `hospital_db_archive`（遗留表迁移目标，保留 90 天后可清理） |

### 3.2 端口规范
| 端口 | 归属 | 监听地址 | 暴露范围 | 说明 |
|---|---|---|---|---|
| 8080 | hospital-api（Spring Boot） | `0.0.0.0` | 对外唯一入口 | 静态后台页 `/`、全部 `/api/**` |
| 5000 | hospital-crawler（Flask） | `127.0.0.1` | 仅本机 | 只由 8080 的 `/api/nearby/**` 代理访问 |
| 3306 | MySQL | `127.0.0.1` | 仅本机 | 仅 hospital-api 访问 |

**废止**：8081（历史误配）、3308（历史误配）。小程序与浏览器一律只使用 `http://<host>:8080`。

### 3.3 前端规范
- 小程序：`config/index.js` 为唯一接口地址来源；`utils/request.js` 统一超时 / 重试 / 错误提示；`app.json` 开启 `lazyCodeLoading`，管理端 9 页拆入分包 `subpackages/admin`。
- 后台控制台：`static/index.html` 仅保留结构，样式拆到 `static/css/app.css`，脚本拆到 `static/js/app.js`，均通过 `/css/app.css`、`/js/app.js` 引用；视觉遵循 hallmark 规范（`design tokens` + 无硬编码色值）。

---

## 4. 详细变更设计

### 4.1 数据库迁移（新增文件）
- `db/migration/V1__unify_collation_and_indexes.sql`
  1. `ALTER DATABASE hospital_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`
  2. 归档：`CREATE DATABASE IF NOT EXISTS hospital_db_archive ...;` + `CREATE TABLE hospital_db_archive.<t> LIKE hospital_db.<t>;` + `INSERT INTO ... SELECT * FROM ...;`，对象为 `user`、`registration_attachment`、`emergency_login_log`。
  3. 删除遗留表：`DROP TABLE IF EXISTS hospital_db.user, hospital_db.registration_attachment, hospital_db.emergency_login_log;`
  4. 索引（全部 `CREATE INDEX`，幂等性由脚本内 `information_schema` 判断 + `PREPARE` 动态执行保证）：
     - `schedule`: `idx_schedule_doctor_date(doctor_id, work_date)`、`idx_schedule_hospital_date(hospital_id, work_date)`
     - `appointment`: `idx_appointment_patient(patient_id, create_time)`、`idx_appointment_openid(openid, create_time)`、`idx_appointment_doctor_date(doctor_id, appointment_date)`、`idx_appointment_schedule(schedule_id)`
     - `doctor`: `idx_doctor_hospital(hospital_id)`、`idx_doctor_dept(dept_id)`、`idx_doctor_username(username)`
     - `department`: `idx_department_hospital(hospital_id)`
     - `hospital`: `idx_hospital_region(province, city, district)`、`idx_hospital_status(status)`
     - `patient`: `idx_patient_phone(phone)`
     - `doctor_schedule_setting`: `idx_dss_doctor(doctor_id)`
     - `emergency_log`: `idx_emergency_log_hospital_time(hospital_id, login_time)`
  5. 不新增唯一约束（避免与历史重复数据冲突改变写入行为）。
- `db/rollback/R1__unify_collation_and_indexes.sql`：删除上述索引；从 `hospital_db_archive` 还原 3 张遗留表。
- `db/schema/hospital_db_schema.sql`：迁移后 `mysqldump --no-data` 权威结构快照，替换过期的 `schema.sql` / `schema_new.sql` 角色（旧文件保留但在文档标注 deprecated）。

### 4.2 后端配置与端口
- 新增 `hospital-api/src/main/resources/application.yml`，内容为统一配置：`server.port=${SERVER_PORT:8080}`、数据源（`${DB_HOST:localhost}`、`${DB_PORT:3306}`、`${DB_NAME:hospital_db}`、`${DB_USER:root}`、`${DB_PASSWORD}`）、HikariCP 参数、JPA `ddl-auto=none`、`spring.sql.init.mode=never`、`crawler.base-url=${CRAWLER_BASE_URL:http://127.0.0.1:5000}`、`amap.key`、`wechat.miniapp.*`。
- 删除 `application.properties`、`application-enhanced.properties`（已备份），删除 `spring.profiles.active=dev` 空配置。
- `pom.xml`：移除未使用的 `spring-boot-starter-data-redis`；删除 `config/RedisConfig.java`。
- 新增 `controller/NearbyController.java`：
  - `GET /api/nearby/hospitals?latitude&longitude&radius&keyword` → `POST {crawler.base-url}/api/hospital/nearby`，透传响应体（`success/data/source/count/totalCount` 结构不变）。
  - `GET /api/nearby/health` → `GET {crawler.base-url}/api/health`，爬虫不可用时返回 `{"status":"down"}` 且 HTTP 200，避免前端阻塞。
  - 超时 8s，异常统一走 `GlobalExceptionHandler` 现有约定。
- 不动 `NearbyHospitalService`（保留高德增强能力，供后续启用）。

### 4.3 端口中心化（运维脚本）
- 新增 `delivery/config/env.bat`：`set API_PORT=8080`、`set CRAWLER_PORT=5000`、`set MYSQL_PORT=3306`、`set DB_NAME=hospital_db`、`set JAVA_HOME`/`PYTHON` 路径。
- 新增 `delivery/start-all.bat`：按 env.bat 启动 MySQL → 爬虫（`--host 127.0.0.1`）→ API，失败即停并打印错误。
- 新增 `delivery/stop-all.bat`：按端口精确停止进程。
- `delivery/start_8081.bat` 改为弃用垫片：打印提示并转调 `start-all.bat`（不再监听 8081）。
- `hospital-crawler/app.py`：`app.run(host='127.0.0.1', port=int(os.environ.get('CRAWLER_PORT', 5000)), debug=False)`。
- 根目录 `update_chinese.py`、`remove_duplicate_schedules.py`、`UpdateChineseData.java`：`3308` → 从环境变量读取，缺省 `3306`。

### 4.4 小程序工程化
- 新增 `config/index.js`：`module.exports = { apiBase: 'http://localhost:8080', requestTimeout: 10000, ... }`，支持通过 `wx.getStorageSync('API_BASE')` 覆盖（便于真机 / 内网调试）。
- 新增 `utils/request.js`：封装 `request(options)`，统一 `baseUrl`、超时、GET/POST、`code !== 200` 与 `success === false` 的错误提示、可选重试 1 次。
- 替换硬编码：`admin-login`、`admin-register`、`admin-manage`、`admin-dashboard`、`emergency-login`、`emergency-department` 的 `apiBase: 'http://localhost:8081/api'` → `apiBase: config.apiBase + '/api'`。
- `nearby/nearby_optimized.js`：删除 `pythonServiceUrl`，`/api/hospital/nearby` → `config.apiBase + '/api/nearby/hospitals'`；`/api/hospital/detail` 保持不调用（爬虫无该路由，改为提示，避免 404）。
- `app.js`：`baseUrl` 改为引用 `config.apiBase`。
- `app.json`：新增 `"lazyCodeLoading": "requiredComponents"`；新增 `"subpackages"`，将 `admin-login`、`admin-register`、`admin-manage`、`admin-audit`、`admin-dashboard`、`hospital-admin-dashboard`、`doctor-audit`、`emergency-login`、`emergency-department` 共 9 页迁入 `subpackages/admin/pages/...`，同步更新 10 处 `wx.navigateTo/redirectTo` 路径。
- `app.wxss`：**追加**（不修改既有规则）设计 token 变量块，供后续页面复用。

### 4.5 后台控制台（static）
- 阶段 A（不改视觉，本次执行）：拆分为 `static/css/app.css`、`static/js/app.js`，`index.html` 保留 DOM 与引用；核对拆分前后页面功能一致。
- 阶段 B（视觉重构，**需用户先确认参考图**）：按 hallmark 流程输出 `backup/reference/index-redesign.png` 供确认，确认后再实施视觉与首屏优化。

---

## 5. 迁移与回滚

### 5.1 执行顺序
1. 备份（已完成）→ 2. 数据库迁移 → 3. 后端配置/端口 → 4. 运维脚本 → 5. 小程序 → 6. 后台拆分 → 7. 全量验证 → 8. 文档

### 5.2 回滚
| 层 | 回滚动作 |
|---|---|
| 数据库 | 执行 `db/rollback/R1__unify_collation_and_indexes.sql`；极端情况用 `backup/hospital_db_20261111_200636.sql` 整库恢复 |
| 后端 | 用 `backup/code_*.zip` 中 `hospital-api` 覆盖回滚；或还原 `application.properties` 并删除 `application.yml` |
| 运维脚本 | 还原 `delivery/*.bat`、`crawler/app.py` |
| 小程序 | 还原对应 `.js` / `.json`；分包回滚需同时还原 `app.json` 与 10 处页面路径 |
| 后台 | 还原 `static/index.html`，删除 `static/css`、`static/js` |

### 5.3 回滚触发条件
- 核心接口冒烟失败（登录、医院列表、科室、医生、排班、预约创建/取消）；
- 应用启动失败或启动时间 > 60s；
- 数据库迁移后 EXPLAIN 结果劣化或出现锁等待超时。

---

## 6. 验证方案

| 编号 | 验证项 | 命令 / 方法 | 通过标准 |
|---|---|---|---|
| V1 | 排序规则统一 | `SELECT table_name,table_collation FROM information_schema.tables WHERE table_schema='hospital_db'` | 全部 `utf8mb4_unicode_ci` |
| V2 | 遗留表已清理 | `SHOW TABLES FROM hospital_db` | 无 `user`/`registration_attachment`/`emergency_login_log` |
| V3 | 归档可查 | `SELECT COUNT(*) FROM hospital_db_archive.user` | = 1 |
| V4 | 索引生效 | `EXPLAIN` 排班按医生+日期、预约按患者查询 | `type=ref/range`，`key` 命中新建索引 |
| V5 | 后端构建 | `mvn -q -DskipTests package` | BUILD SUCCESS |
| V6 | 启动与端口 | 启动 jar，`netstat -ano \| findstr :8080` | 仅 8080 监听；5000 为 127.0.0.1 |
| V7 | 核心接口 | `curl /api/hospitals`、`/api/departments/hospital/{id}`、`/api/doctors/hospital/{id}`、`/api/schedules/available`、`/api/nearby/health` | HTTP 200，JSON 结构不变 |
| V8 | 端口残留 | 全仓 grep `8081`、`3308`、`localhost:5000` | 仅在文档/历史备份中出现 |
| V9 | 小程序配置 | grep `localhost:8081`、`pythonServiceUrl` | 0 命中 |
| V10 | 分包完整性 | 校验 `app.json` 中每个分包页面在磁盘存在，且无残留旧路径引用 | 全部命中 |

---

## 7. 交付物清单
- 代码：`db/migration/V1__*.sql`、`db/rollback/R1__*.sql`、`db/schema/hospital_db_schema.sql`、`application.yml`、`NearbyController.java`、`config/index.js`、`utils/request.js`、`static/css/app.css`、`static/js/app.js`、`delivery/config/env.bat`、`delivery/start-all.bat`、`delivery/stop-all.bat`
- 文档：`docs/ARCHITECTURE.md`、`docs/DEPLOYMENT.md`、`docs/MIGRATION_ROLLBACK.md`、`docs/PORT_REGISTRY.md`、`docs/TEST_REPORT.md`
- 参考图：`backup/reference/index-redesign.png`（阶段 B 前置确认物）

---

## 8. 待用户确认事项
1. 是否认可「8080 单入口 + 5000 仅本机代理」端口方案（本规格已按此设计）。
2. 后台控制台视觉重构需先看参考图再实施；本次是否先只做 A 阶段（拆分资源、不改视觉），视觉重构等参考图确认后单独执行。
3. 管理端 9 页迁入分包会变更页面路径，需在微信开发者工具中回归 10 条跳转链路；是否接受该改动（不接受则仅开启 `lazyCodeLoading`）。
