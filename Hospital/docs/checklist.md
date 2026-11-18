# 验证清单（checklist）

对应 `docs/spec.md` §6，逐项实测。`[x]` 通过　`[ ]` 待执行/阻塞

## 检查点

- [x] V1 排序规则统一：`hospital_db` 12 张表全部 `utf8mb4_unicode_ci`（不一致数 = 0）
- [x] V2 遗留表已清理：`user` / `registration_attachment` / `emergency_login_log` 已从业务库移除
- [x] V3 归档可查：`hospital_db_archive.user` = 1 行，其余 0 行
- [x] V4 索引生效：`schedule(doctor_id, work_date)` → `type=ref` 命中 `idx_schedule_doctor_date`；`appointment(patient_id)` 无 filesort
- [x] V5 后端构建：`mvn -DskipTests package` 成功，jar 49.34 MB
- [x] V6 启动与端口：11.1s 启动；8080 = `0.0.0.0`；5000 = `127.0.0.1`
- [x] V7 核心接口：`/api/hospitals`、`/api/departments/hospital/{id}`、`/api/doctors/hospital/{id}`、`/api/schedules/hospital/{id}/date/{date}`、`/api/nearby/health`、`/` 全部 200
- [x] V7a 降级行为：爬虫离线时 `/api/nearby/hospitals` 仍返回 200 + `success=false`
- [x] V8 端口残留：业务代码 0 命中（8081/3307/3308/localhost:5000 仅存在于废弃脚本提示语）
- [x] V9 小程序配置：`8081` / `pythonServiceUrl` / `localhost:5000` 命中数 = 0
- [x] V10 分包完整性：`app.json` 可解析，主包 37 页 + 分包 9 页，页面文件全部存在，旧路径引用 = 0
- [x] V11 后台资源拆分：`index.html` 17.2 KB + `css/app.css` 7.9 KB + `js/app.js` 40.1 KB，首页 200
- [x] V12 备份可恢复：源码 zip 可打开、数据库 dump 10.37 MB 已生成

## 待人工确认

- [ ] 小程序开发者工具回归 10 条分包跳转链路（登录 → 管理端 → 审核 → 急诊）
- [ ] 后台视觉重构：确认 `backup/reference/index-redesign.png` 后实施
