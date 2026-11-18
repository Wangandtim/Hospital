# 分诊预约 Agent 实施任务

- [x] T1 数据库与实体底座
  - 新增 `hospital-api/src/main/resources/triage_v1.sql`：`triage_session`、`triage_lock` 两表、索引、utf8mb4
  - 新增 `entity/TriageSession.java`、`entity/TriageLock.java`
  - 新增 `repository/TriageSessionRepository.java`、`repository/TriageLockRepository.java`
  - 验收：DDL 执行成功；`SHOW CREATE TABLE` 字段/索引齐备；`mvn -q -DskipTests package` 通过

- [x] T2 规则引擎（红旗 + 科室 + 置信度 + 专家）
  - 新增 `service/TriageRuleEngine.java`
  - 实现归一化、否定词、10 类红旗、18 科室关键词、置信度公式、专家评分
  - 输出结构化症状 JSON、证据列表、追问问题、专家结论
  - 验收：固定样例（高/中/低/红旗/专家）本地单测或临时 main 输出符合 Spec 5.4

- [x] T3 业务服务（画像、匹配、锁号、出单）
  - 新增 `service/TriageService.java`
  - 实现 assess/profile/confirm/reject/lock/book 全流程、幂等、归属校验
  - 医院四维评分（35/25/15/25）与硬过滤；重锁优先同院同科下一号源，再按评分降级
  - 锁号原子占位、释放、单患者单有效锁
  - 验收：锁号后 `booked_count +1`；book 不重复扣减；重复调用返回同一锁/预约

- [x] T4 控制器与鉴权
  - 新增 `controller/TriageController.java`：6 个接口
  - 修改 `security/AuthInterceptor.java`：`/api/triage/` 仅 ADMIN/PATIENT
  - 新增 `service/TriageLockScheduler.java`：每 60 秒释放过期锁；每天 03:15 归档超期未完成会话
  - 验收：匿名 401；患者越权访问他人会话 403/业务拒绝

- [x] T5 分诊页面
  - 新增 `pages/triage/triage.js/.wml/.wxss/.json`
  - 输入、补资料、三档、红旗跳转、锁号、付款面板、倒计时、长辈模式
  - 刷新恢复：本地保存未完成会话，进入页面提示继续；出单/重填/拒绝后清理
  - 长辈语音态：`voice=1&mode=ELDER` 首屏大话筒 + 同风格提示；按住录音/松开；ASR 未配置时展示大号常见不适按钮，不伪造识别
  - 标准模式保留文本输入；语音态与文本态共用状态机
  - 验收：`node --check`；登录拦截；三档分支与文案正确

- [x] T6 急诊页与入口
  - 新增 `pages/triage-emergency/triage-emergency.*`
  - 修改 `app.json` 注册页面
  - 修改 `pages/index/index.wxml/js/wxss`：标准模式新增「智能分诊」卡片；长辈模式主按钮改为「语音 Agent 预约」并跳分诊语音态
  - 修改 `pages/settings/settings.js` 的 `goAgent` 跳分诊页
  - 验收：小程序编译无缺失页面；两处入口可进入；长辈入口进入即见话筒

- [x] T7 部署与接口实测
  - 执行 `triage_v1.sql`
  - 打包、重启 18080
  - 按 Spec 7 的 8 个场景实测
  - 验收：响应字段/档位/专家结论正确；锁号→出单→支付闭环通过

- [x] T8 数据清理与复盘
  - 清理测试预约、恢复测试号源；保留测试患者（或按需删除）
  - 输出实测结果与剩余风险（专家号源标记、LLM 归一化后续项）
  - 验收：预约数/号源数回到测试前口径，差异有说明
