# 测试验证报告（TEST REPORT）

版本：v1.0　执行日期：2026-11-11　执行环境：Windows 10/11　Java 17.0.17　MySQL 8.0　Node 26.4.0

---

## 1. 结论摘要

| 维度 | 重构前 | 重构后 | 结果 |
|---|---|---|---|
| 数据库实例 / 库 | 1 实例 / 15 表（3 张无实体映射） | 1 实例 / 12 表（全部有实体映射） | 通过 |
| 排序规则 | 3 张表 `utf8mb4_0900_ai_ci` 混杂 | 12 张全部 `utf8mb4_unicode_ci` | 通过 |
| 业务索引 | 近乎为零（`schedule` 11.6 万行 0 索引） | 新增 15 个，共 32 个索引对象 | 通过 |
| 连接池 | 默认无显式配置 | HikariCP `hospital-hikari`（20/5/30s/30min） | 通过 |
| 配置来源 | 2 份 properties（含冲突项） | 1 份 `application.yml` + 环境变量 | 通过 |
| 端口 | 8080 / 5000(0.0.0.0) / 8081 / 3308 / 3307 | 8080 / 5000(127.0.0.1) / 3306 | 通过 |
| 小程序主包 | 46 页 624 KB | 37 页 576.8 KB（分包 9 页 92.3 KB） | 通过 |
| 后台页面 | 单文件 65.2 KB 内联 | HTML 17.2 + CSS 7.9 + JS 40.1 KB | 通过 |
| API 构建/启动 | — | BUILD SUCCESS / 11.1s | 通过 |

---

## 2. 验证项明细（对应 spec §6）

### V1 排序规则统一 — 通过
```
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='hospital_db';                             -- 12
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='hospital_db'
  AND table_collation <> 'utf8mb4_unicode_ci';                                                               -- 0
```

### V2 遗留表已清理 — 通过
`SHOW TABLES FROM hospital_db` 返回 12 张：`admin, appointment, department, doctor, doctor_detail, doctor_schedule_setting, emergency_log, hospital, hospital_key, patient, schedule, wechat_user`。
`user`、`registration_attachment`、`emergency_login_log` 已不在业务库。

### V3 归档可查 — 通过
```
SELECT 'user',COUNT(*) FROM hospital_db_archive.user
UNION ALL SELECT 'registration_attachment',COUNT(*) FROM hospital_db_archive.registration_attachment
UNION ALL SELECT 'emergency_login_log',COUNT(*) FROM hospital_db_archive.emergency_login_log;
-- user 1 / registration_attachment 0 / emergency_login_log 0
```

### V4 索引生效 — 通过

| 查询 | 执行计划 | 命中索引 |
|---|---|---|
| `SELECT schedule_id FROM schedule WHERE doctor_id=? AND work_date=?` | `type=ref` | `idx_schedule_doctor_date` |
| `SELECT * FROM appointment WHERE patient_id=? ORDER BY create_time DESC` | `type=ref`，无 `filesort` | `idx_appointment_patient` |

迁移耗时：**3.1 s**（含 11.6 万行 `schedule` 建索引）。

### V5 后端构建 — 通过
`mvn -q -DskipTests package` → `EXIT=0`，产物 `hospital-api-0.0.1-SNAPSHOT.jar`，**49.34 MB**。

### V6 启动与端口 — 通过
- 启动日志：`Tomcat started on port 8080`，`Started HospitalApiApplication in 11.09 seconds`。
- `netstat`：8080 监听 `0.0.0.0`；5000 监听 `127.0.0.1`（爬虫）；3306 仅本机。
- 连接池日志：`hospital-hikari - Start completed.`

### V7 核心接口 — 通过

| 接口 | 状态 | 响应体 |
|---|---|---|
| `GET /api/hospitals` | 200 | 20,838 B |
| `GET /api/departments/hospital/H001` | 200 | 4,903 B |
| `GET /api/doctors/hospital/H001` | 200 | 13,105 B |
| `GET /api/schedules/hospital/H001/date/2026-11-19` | 200 | 约 300 KB（单日 888 条） |
| `GET /api/nearby/health`（爬虫在线） | 200 | `{"status":"up",...}` |
| `GET /api/nearby/hospitals?latitude=30.657&longitude=103.447&radius=50000` | 200 | `success=true`，9 家医院 |
| `GET /`（后台首页） | 200 | 17,628 B |

降级验证：爬虫未启动时 `/api/nearby/hospitals` 仍返回 HTTP 200 + `success=false` + 空列表，不抛 500。

### V8 端口残留 — 通过
扫描范围：`*.js/*.json/*.java/*.py/*.bat/*.yml/*.properties/*.html/*.css`（排除 `backup/`、`target/`、`node_modules`、MySQL 二进制）。
结果：`8081`、`3307`、`3308`、`localhost:5000` **仅出现在 9 个已废弃脚本的提示文案中**（`start_8081.bat` 等，均转调 `start-all.bat`），业务代码 0 命中。

### V9 小程序配置 — 通过
`hospital-miniprogram` 内 `8081`、`pythonServiceUrl`、`localhost:5000` 命中数 = **0**。
6 个页面 `apiBase` 均改为 `config.apiBase + '/api'`；`nearby_optimized.js` 改为 `config.apiBase + '/api/nearby/hospitals'`。

### V10 分包完整性 — 通过
- `app.json` JSON 解析通过；主包 37 页、分包 `subpackages/admin` 9 页。
- `lazyCodeLoading = requiredComponents` 已开启。
- 扫描 `app.json` 全部页面路径，`.js/.wxml` 文件 **全部存在**。
- 全仓 `/pages/(admin-*|doctor-audit|emergency-*|hospital-admin-dashboard)/` 旧路径引用 = **0**（14 处跳转已改为 `/subpackages/admin/pages/...`）。

---

## 3. 缺陷与修复记录

| # | 缺陷 | 影响 | 处理 |
|---|---|---|---|
| 1 | 归档 `user` 时被 `registration_attachment` 外键阻塞 | 首次迁移失败 | 调整顺序：先归档子表再归档父表；回滚脚本反向还原 |
| 2 | 删除 `application.properties` 后 `NearbyHospitalService` 仍读 `${python.service.url}` | 应用启动失败 | 统一改用 `${crawler.base-url}` |
| 3 | 爬虫 `DEFAULT_HOSPITALS` 首条记录缺 `longitude` | `/api/hospital/nearby` 恒定 500 | 补齐缺省字段（坐标、type、tags、distance） |
| 4 | 爬虫 `AMAP_KEY` 硬编码 | 与后端配置不一致 | 改为 `AMAP_KEY`/`AMAP_ENABLED` 环境变量 |
| 5 | 12 个 delivery 遗留 bat 写死 3307/8081 | 端口分散 | 统一改为弃用垫片，转调 `start-all.bat` |
| 6 | `app_optimized.py` 打印 localhost:5000 | 误导运维 | 改为读取 `CRAWLER_PORT` |

> 说明：缺陷 3、4 为重构前既存问题（非本次改造引入），已一并修复并在 `docs/MIGRATION_ROLLBACK.md` 记录。

---

## 4. 性能与体积对比

| 指标 | 重构前 | 重构后 | 变化 |
|---|---|---|---|
| 小程序主包 | 624 KB（46 页同包） | 576.8 KB（37 页） | ↓ 7.6% |
| 小程序分包 | — | 92.3 KB（9 页，按需下载） | 首屏不加载 |
| 首屏脚本 | 全量注入 | `lazyCodeLoading` 按需注入 | 解析量下降 |
| 后台 HTML | 65.2 KB（内联 CSS+JS） | 17.2 KB | ↓ 73.6% |
| 后台静态资源 | 不可独立缓存 | CSS 7.9 KB + JS 40.1 KB（可长缓存） | 二次访问更快 |
| `schedule` 表按医生+日期查询 | 全表扫描（11.6 万行） | 索引 `ref` | 数量级提升 |
| 启动耗时 | — | 11.1 s | 达标（< 60 s） |

---

## 5. 未覆盖项与后续建议

| 项 | 原因 | 建议 |
|---|---|---|
| 小程序真机/开发者工具回归 | 需人工在微信开发者工具操作 | 按 `docs/tasks.md` T4.6 回归 10 条跳转链路：登录→管理端→审核→急诊 |
| 预约创建/取消链路 | 需有效排班与患者登录态 | 用 `GET /api/schedules/hospital/H001/date/{yyyy-MM-dd}` 取 `schedule_id` 后走 `POST /api/appointments` 冒烟 |
| 并发压测 | 本次为架构重构 | 全院排班接口已强制按日期查询（缺 `date` 返回 400），压测按单日响应评估 |
| 后台视觉重构 | 按约定需先确认参考图 | 见 `backup/reference/index-redesign.png`，确认后再实施 |

---

## 6. 回归检查清单

- [x] 数据库字符集/排序规则统一
- [x] 遗留表归档并下线
- [x] 15 个业务索引创建且命中
- [x] 后端单一配置 + HikariCP
- [x] 后端构建与启动
- [x] 核心读接口 200
- [x] 周边医院代理链路（含降级）
- [x] 端口残留清理
- [x] 小程序配置层统一
- [x] 小程序分包与按需注入
- [x] 后台资源拆分
- [ ] 小程序开发者工具人工回归（待执行）
- [ ] 后台视觉重构（待参考图确认）
