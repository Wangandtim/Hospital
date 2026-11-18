# 架构重构 任务清单（tasks）

对应规格：`docs/spec.md`　执行前提：`backup/code_20261111_200928.zip` 与 `backup/hospital_db_20261111_200636.sql` 已生成（已完成）

图例：`[ ]` 未开始　`[~]` 进行中　`[x]` 已完成

---

## T1 数据库层统一
- [x] T1.1 新建 `db/migration/V1__unify_collation_and_indexes.sql`
  - 库级字符集/排序规则统一
  - 归档 `user`、`registration_attachment`、`emergency_login_log` 到 `hospital_db_archive` 后 `DROP`
  - 按 spec §4.1 建 15 个业务索引（`information_schema` 幂等判断）
- [x] T1.2 新建 `db/rollback/R1__unify_collation_and_indexes.sql`（删索引 + 还原 3 表）
- [x] T1.3 执行迁移，输出 V1/V2/V3/V4 验证结果（3.1s，全部通过）
- [x] T1.4 生成 `db/schema/hospital_db_schema.sql`（`mysqldump --no-data --routines`）

验收：spec §6 V1–V4 全部通过；`schedule` 表按 `(doctor_id, work_date)` 查询 `EXPLAIN` 命中 `idx_schedule_doctor_date`。✅

## T2 后端配置与端口统一
- [x] T2.1 新增 `hospital-api/src/main/resources/application.yml`（含 HikariCP、env 覆盖、`crawler.base-url`）
- [x] T2.2 删除 `application.properties`、`application-enhanced.properties`
- [x] T2.3 `pom.xml` 移除 `spring-boot-starter-data-redis`；删除 `config/RedisConfig.java`
- [x] T2.4 新增 `controller/NearbyController.java`（`/api/nearby/hospitals`、`/api/nearby/health` 代理爬虫）
- [x] T2.5 `mvn -q -DskipTests package` 通过，启动后 V6/V7 通过（jar 49.34 MB，启动 11.1s）

验收：V5–V7 通过；`/api/nearby/hospitals` 在爬虫启动时返回 `success=true`，未启动时返回 `success=false` 而非 500。✅

## T3 端口中心化与运维脚本
- [x] T3.1 新增 `delivery/config/env.bat`（端口/库名/JAVA_HOME 唯一来源）
- [x] T3.2 新增 `delivery/start-all.bat`、`delivery/stop-all.bat`
- [x] T3.3 `delivery/start_8081.bat` 改为弃用垫片并转调 `start-all.bat`（同时处理 `final_*`、`start_api*`、`reinit_*`、`import_*`、`create_db` 等 16 个遗留脚本）
- [x] T3.4 `hospital-crawler/app.py` 改为 `127.0.0.1` + `CRAWLER_PORT` 环境变量
- [x] T3.5 根目录 `update_chinese.py`、`remove_duplicate_schedules.py`、`UpdateChineseData.java`、`import_schedule.py`、`fix_duplicate_usernames.py`：`3308` → `3306`/环境变量

验收：V8 通过（8081/3307/3308 仅存于废弃脚本提示语）。✅

## T4 小程序工程化
- [x] T4.1 新增 `hospital-miniprogram/config/index.js`
- [x] T4.2 新增 `hospital-miniprogram/utils/request.js`
- [x] T4.3 6 个页面 `8081` 硬编码改为 `config.apiBase`
- [x] T4.4 `nearby_optimized.js` 改为 `/api/nearby/hospitals`，移除 `pythonServiceUrl` 与 404 的 detail 调用
- [x] T4.5 `app.js` `baseUrl` 引用 `config.apiBase`
- [x] T4.6 `app.json` 增加 `lazyCodeLoading` + `subpackages.admin`（9 页），同步更新 14 处跳转路径
- [x] T4.7 `app.wxss` 追加设计 token（不覆盖既有规则）

验收：V9、V10 通过；分包页面文件均存在，无 `/pages/admin-login/...` 等旧路径残留。✅

## T5 后台控制台
- [x] T5.1 阶段 A：拆分 `static/index.html` → `static/css/app.css` + `static/js/app.js`，视觉与行为不变（17.2 + 7.9 + 40.1 KB）
- [x] T5.2 阶段 B 前置：输出参考图 `backup/reference/index-redesign.png`（含现状截图 `index-current.png`）
- [ ] T5.3 阶段 B（阻塞）：按 hallmark 规范重构视觉与首屏 —— **等待用户确认参考图**

验收：A 阶段拆分后首页 200 且功能与拆分前一致；B 阶段需用户确认参考图后方可执行。

## T6 文档交付
- [x] T6.1 `docs/ARCHITECTURE.md`
- [x] T6.2 `docs/DEPLOYMENT.md`
- [x] T6.3 `docs/MIGRATION_ROLLBACK.md`
- [x] T6.4 `docs/PORT_REGISTRY.md`
- [x] T6.5 `docs/TEST_REPORT.md`
- [x] T6.6 更新 `PROJECT_README.md` 的端口与数据库章节

验收：文档中的端口、库名、路径与实际一致。✅

## T7 收尾验收
- [x] T7.1 全量验证 V1–V12 记录归档（`docs/checklist.md`）
- [x] T7.2 核心业务链路回归：登录 → 医院列表 → 科室 → 医生 → 排班（读链路全部 200）
- [x] T7.3 输出重构前后对比（表数量、索引数、配置项数、端口数、首屏资源）

---

## 阻塞与依赖
- T5.3 依赖用户确认参考图（spec §8.2）。

## 风险登记
| 风险 | 影响 | 缓解 |
|---|---|---|
| `schedule` 建索引期间锁表 | 短时写入阻塞 | 实测 3.1s 完成（含全部索引），无锁等待 |
| 分包路径变更导致跳转 404 | 管理端不可达 | 已全量 grep 校验（14 处已更新、旧路径 0 残留）；仍需开发者工具人工回归 |
| 删除 `application.properties` 后环境差异 | 启动失败 | 键值全部迁移至 `application.yml` 并支持 env 覆盖；已实测启动成功 |
| 爬虫未启动时周边页空白 | 用户体验 | `/api/nearby/health` 降级 + 前端提示；异常返回结构化错误（已验证 200 + `success=false`） |
