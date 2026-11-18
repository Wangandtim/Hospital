# 迁移与回滚手册（MIGRATION_ROLLBACK）

版本：v1.0　执行窗口：业务低峰　预计停机：≤ 5 分钟（仅重启 API）

---

## 1. 备份资产（迁移前已生成）

| 资产 | 路径 | 大小 | 说明 |
|---|---|---|---|
| 源码包 | `backup/code_20261111_200928.zip` | 391 MB | hospital-api / crawler / miniprogram / delivery 脚本 + 根目录脚本与文档（已校验可解压） |
| 数据库全量 | `backup/hospital_db_20261111_200636.sql` | 10.37 MB | `mysqldump --single-transaction --routines --triggers --events` |

校验方式：

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
([System.IO.Compression.ZipFile]::OpenRead("d:\Desktop\Hospital\backup\code_20261111_200928.zip")).Entries.Count
```

---

## 2. 迁移步骤（按序执行）

### 步骤 1：数据库

```powershell
# 备份（若尚未备份）
mysqldump -uroot -p --single-transaction --routines --triggers --events hospital_db > backup\hospital_db_manual.sql

# 执行迁移（幂等，可重复执行）
mysql -uroot -p hospital_db -e "source d:/Desktop/Hospital/db/migration/V1__unify_collation_and_indexes.sql"
```

迁移内容：
1. 库级字符集统一 `utf8mb4 / utf8mb4_unicode_ci`。
2. 归档 `user`、`registration_attachment`、`emergency_login_log` 到 `hospital_db_archive` 并下线（实测耗时 3.1s）。
3. 新增 15 个业务索引（`schedule` 11.6 万行，Online DDL，秒级）。

验证：

```sql
SELECT table_name, table_collation FROM information_schema.tables WHERE table_schema='hospital_db';
SHOW TABLES FROM hospital_db;                       -- 应为 12 张
SELECT COUNT(*) FROM hospital_db_archive.user;      -- 应为 1
EXPLAIN SELECT schedule_id FROM schedule WHERE doctor_id='D001' AND work_date='2026-11-12';
-- 期望 key = idx_schedule_doctor_date
```

### 步骤 2：后端

1. 确认 `hospital-api/src/main/resources/application.yml` 存在。
2. 确认 `application.properties`、`application-enhanced.properties`、`config/RedisConfig.java` 已删除。
3. `mvn -DskipTests package` 并重启 jar。

### 步骤 3：爬虫

1. `hospital-crawler/app.py` 监听 `127.0.0.1:5000`（环境变量 `CRAWLER_HOST`/`CRAWLER_PORT`）。
2. 重启后访问 `GET /api/health`。

### 步骤 4：小程序

1. 确认 `config/index.js`、`utils/request.js` 存在。
2. 确认 9 个页面位于 `subpackages/admin/pages/`，`app.json` 含 `subpackages` 与 `lazyCodeLoading`。
3. 全量扫描：任何 `*.js` 不应再出现 `8081`、`localhost:5000`、`/pages/admin-`。

### 步骤 5：后台控制台

1. 确认 `static/css/app.css`、`static/js/app.js` 存在且 `index.html` 引用 `/css/app.css`、`/js/app.js`。
2. 打开 `http://localhost:8080/`，登录与列表功能正常。

---

## 3. 回滚剧本

### 3.1 触发条件（满足任一即回滚）

- 核心接口冒烟失败：`/api/hospitals`、`/api/departments/hospital/{id}`、`/api/doctors/hospital/{id}`、`/api/schedules/hospital/{id}/date/{date}`、预约创建/取消。
- 应用启动失败或启动耗时 > 60s。
- 迁移后 `EXPLAIN` 由 `ref/range` 退化为 `ALL`，或出现锁等待超时。

### 3.2 分层回滚

| 层 | 操作 | 预计耗时 |
|---|---|---|
| 数据库（结构） | `mysql -uroot -p hospital_db -e "source d:/Desktop/Hospital/db/rollback/R1__unify_collation_and_indexes.sql"`（删 15 个索引 + 还原 3 张表） | < 1 min |
| 数据库（数据） | `mysql -uroot -p -e "DROP DATABASE hospital_db; CREATE DATABASE hospital_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"` 后导入 `backup/hospital_db_20261111_200636.sql` | 2–5 min |
| 后端 | 从 `backup/code_*.zip` 解压 `hospital-api` 覆盖；或临时恢复 `application.properties` 并删除 `application.yml`，改回 `${python.service.url}` 占位 | 2 min |
| 爬虫 | 恢复 `app.py`（`0.0.0.0:5000`）；或在 `start-all.bat` 前设置 `CRAWLER_HOST=0.0.0.0` | 1 min |
| 小程序 | 恢复 `app.json`（37+9 页合并为 46 页主包）、`pages/<9页>` 目录、6 处 `apiBase`、`nearby_optimized.js` | 10 min（需重新上传体验版） |
| 后台 | 恢复 `static/index.html` 单文件版，删除 `static/css`、`static/js` | 1 min |

### 3.3 快速整库恢复

```powershell
mysql -uroot -p -e "DROP DATABASE IF EXISTS hospital_db; CREATE DATABASE hospital_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
Get-Content backup\hospital_db_20261111_200636.sql | mysql -uroot -p --default-character-set=utf8mb4 hospital_db
```

---

## 4. 回滚后的自检清单

- [ ] `SHOW TABLES` 恢复 15 张（含 `user`/`registration_attachment`/`emergency_login_log`）
- [ ] 行数与基线一致：hospital 54、doctor 1685、department 547、schedule 116190、appointment 89、patient 7、admin 5、wechat_user 3
- [ ] API 启动成功，`/api/hospitals` 200
- [ ] 小程序 6 个原 8081 页面可正常请求
- [ ] 后台首页样式与功能正常

---

## 5. 迁移演练记录

| 项目 | 结果 |
|---|---|
| 迁移脚本执行 | 成功，耗时 3.1s（首次因 `registration_attachment` 外键依赖 `user` 失败一次，已调整为先归档子表） |
| 重复执行 | 通过（幂等，无报错） |
| 索引命中 | `schedule` 按 `(doctor_id, work_date)` → `type=ref`、`key=idx_schedule_doctor_date`；`appointment` 按 `patient_id` 倒序 → `type=ref`、`key=idx_appointment_patient` |
| 遗留表归档 | `hospital_db_archive.user` = 1 行，其余 0 行 |
| API 构建 | `mvn -DskipTests package` 成功（jar 49.34 MB） |
| API 启动 | 11.1s，监听 `0.0.0.0:8080` |
