# 医生端预约 / 问卷 / 自主排班 / 最低排班 与三端数据连接 全量审计与修复 Spec

## 1. 目标

对「医生端查看患者预约」「医生端问卷查看」「医生自主排班」「未排班/排班时间不达标流程」「三端（患者端/医生端/管理端）数据连接」做一次全量审计，修复已证实的缺陷，使三端字段、状态、解析规则一致。

## 2. 已证实的缺陷（按严重度）

### P0（功能失效/数据错误/越权）

| # | 问题 | 位置 | 证据 |
|---|------|------|------|
| F1 | 医生端问卷恒为空：患者提交扁平 `{q1:...}`，医生端按 `{deptId, answers:{...}}` 解析 | 患者 [appointment.js](file:///d:/Desktop/Hospital/hospital-miniprogram/pages/appointment/appointment.js#L846)；医生 [doctor-workspace.js](file:///d:/Desktop/Hospital/hospital-miniprogram/pages/doctor-workspace/doctor-workspace.js#L257-L284)、[questionnaire-list.js](file:///d:/Desktop/Hospital/hospital-miniprogram/pages/questionnaire-list/questionnaire-list.js#L22-L58) | 两侧 JSON 结构不同，`parsed.deptId`/`parsed.answers` 均为 undefined |
| F2 | 医生端科室问卷映射只有 DEPT001~008，患者端覆盖 16 个科室；且题干文案两套 | doctor-workspace.js L348-423、questionnaire-list.js L68-215、patient-detail.js L3-155 | DEPT009~016 → `getQuestionnaireConfig` 返回 null |
| F3 | `by-username` 未返回 deptId/deptName/hospitalId/hospitalName/doctorTitle，也无 `doctor` 对象 | [AppointmentController.java](file:///d:/Desktop/Hospital/hospital-api/src/main/java/com/hospital/controller/AppointmentController.java#L438-L465) | 工作台头部 `doctorDept` 恒空；`item.doctor.deptId` 为空 → patient-detail 用 DEFAULT 问卷，题干与答案错配、q4 丢失 |
| F4 | 「保底排班」保存必失败：后端无 `POST /api/schedules/batch`；请求字段 `departmentId`/`scheduleDate` 与 Schedule 实体（`workDate`，无 deptId）不符 | [doctor-minimum-schedule.js](file:///d:/Desktop/Hospital/hospital-miniprogram/pages/doctor-minimum-schedule/doctor-minimum-schedule.js#L155-L200)；[Schedule.java](file:///d:/Desktop/Hospital/hospital-api/src/main/java/com/hospital/entity/Schedule.java) | 全后端无 `/batch` 映射（仅 `/api/batch-import`、`/doctor/{id}/batch`） |
| F5 | 已占用时段未禁用：`/api/schedules/doctor/{id}/date/{date}` 返回 `{success,data}`，前端按数组判断 | doctor-minimum-schedule.js L115-140 | `Array.isArray(res.data)` 为 false |
| F6 | `onLoad` 拉全量排班，按 timeSlot 跨日期禁用当天时段 | doctor-minimum-schedule.js L47-78 | 其他日期的 09:00 会禁用当天 09:00 |
| F7 | 越权：`/api/appointments/schedule/{scheduleId}` 只校验角色，不校验排班归属；`/{appointmentId}/complete` 不校验医生归属与状态 | AppointmentController.java L393-405、L796-808 | 任一医生可查他人患者名单、可完成他人/已取消预约 |
| F8 | 院管越界：`/admin/all`、`/admin/{id}/status` 未按 hospitalId 过滤 | AppointmentController.java L813-851 | 院管可读/改其他医院预约 |

### P1（体验/一致性/流程）

| # | 问题 | 位置 |
|---|------|------|
| F9 | 新建排班选择「停诊」被后端忽略（create 固定 status=1） | ScheduleController L120-144、ScheduleService L68-90 |
| F10 | 创建排班只校验完全相同 timeSlot，未校验时间区间重叠 | ScheduleService L73-77 |
| F11 | 最低排班阈值三处不一致：保底页 6 时段/上限 18 时段（文案“最多6小时”），工作台按“未来全部排班累计”统计，my-schedule 最小 20 分钟 | doctor-minimum-schedule.js、doctor-workspace.js L105-174 |
| F12 | 保底排班页无任何入口（无 navigateTo），“排班不足”跳 my-schedule | 全项目 grep |
| F13 | 跳过文案“未设置保底排班将无法接受预约”与实际不符（无强制拦截） | doctor-minimum-schedule.js L207-215 |
| F14 | `getStatusText/getStatusClass` 缺 status=3（进行中 → “未知”/取消样式） | doctor-workspace.js L489-500 |
| F15 | 医生端 onShow 3 请求 + 30s 刷新；`/api/schedules/doctor/{id}` 全量 3655 条 | doctor-workspace.js L176-198 |

### 三端连接核对结论

- 预约状态语义：`0 已取消 / 1 待就诊 / 2 已完成 / 3 进行中`（[Appointment.checkAndUpdateStatus](file:///d:/Desktop/Hospital/hospital-api/src/main/java/com/hospital/entity/Appointment.java#L76-L120)）。患者端、医生端 WXML 已一致；仅 F14 的 JS 兜底函数不一致。管理端 `appointment-manage` 不读 status（按时段展示患者），无冲突。
- `/api/doctor/{doctorId}/patients`（「我的患者」）实现于 [DoctorAuthController](file:///d:/Desktop/Hospital/hospital-api/src/main/java/com/hospital/controller/DoctorAuthController.java#L178)，连接正常。
- 排班唯一键（医院+医生+日期+时段）在创建时校验；缺区间重叠校验（F10）。

## 3. 修复方案（决策已定）

### 3.1 问卷（F1/F2/F3）
1. 新增共享配置 `hospital-miniprogram/utils/questionnaire-config.js`：
   - 导出 `QUESTIONNAIRES`（16 个科室问卷，取 [patient-detail.js](file:///d:/Desktop/Hospital/hospital-miniprogram/pages/patient-detail/patient-detail.js) 的完整定义）、`DEFAULT_QUESTIONNAIRE`。
   - **匹配规则：科室名称优先，编号仅兜底**（跨医院实测：H001_DEPT001=内分泌科、H002_DEPT001=内科，编号与科室不对应；新增科室编号为 `D1234`）。
   - `getConfig(deptId, deptName)`：先按 `deptName`（含别名：耳鼻咽喉科/风湿科/消化科等）匹配，再按编号后缀匹配，最后回退通用问卷。
   - `parse(raw, deptId, deptName)`：`JSON.parse` 后兼容两种结构——`parsed.answers`（嵌套）或 `parsed` 本身（扁平）；展示名使用实际科室名；返回 `{deptName, title, answers}`；未匹配科室用通用问卷而非空白。
2. 改造 4 处调用：
   - `appointment.js`：删除本地 QUESTIONNAIRES/DEFAULT，`loadQuestionnaire` 用 `getConfig`（患者提交格式保持扁平 `{q1:...}` 不变，历史数据无需迁移）。
   - `doctor-workspace.js`：删除本地 getQuestionnaireConfig，改用 `parse(appt.questionnaireData, appt.deptId)`。
   - `questionnaire-list.js`：同上，用 `appt.deptId`。
   - `patient-detail.js`：`parseQuestionnaire` 改用 `parse(raw, this.data.deptId)`。
3. 后端 `by-username` 响应补充：`doctorTitle`、`deptId`、`deptName`、`hospitalId`、`hospitalName`，并补充嵌套 `doctor: {doctorId,name,title,deptId,hospitalId}`（保持现有字段不变）。

### 3.2 越权（F7/F8）
- `getAppointmentsByScheduleId`：取排班 → 医生角色时校验 `schedule.doctorId == ownerId`；院管校验医院；超管放行。
- `complete`：医生角色时校验预约 `doctorId == ownerId`；仅 `status in (1,3)` 可完成，否则返回业务失败。
- `/admin/all`：院管按 `hospitalId` 过滤（医生所属医院）。
- `/admin/{id}/status`：院管校验预约医生所属医院一致。

### 3.3 自主排班 / 保底排班（F4/F5/F6/F9/F10/F11/F12/F13）
- 前端 `doctor-minimum-schedule.js`：
  - `onLoad` 不再拉全量禁用；从 `options.date` 预选日期并调用 `loadDisabledSlotsForDate`。
  - `loadDisabledSlotsForDate` 正确解析 `res.data.data` 数组。
  - 移除 `loadDisabledSlots`（全量跨日期误禁）。
  - `confirmSchedule` 改为逐条 `POST /api/schedules`，字段 `{doctorId, hospitalId, workDate, timeSlot, maxPatients:20, status:1}`，`Promise.all` 汇总失败时段；不再使用 `/batch`。
  - 阈值统一：`requiredSlots = 6`（2 小时），上限 18（6 小时），文案统一“至少 2 小时（6 个时段）”“最多 6 小时（18 个时段）”。
  - `skipSetting` 文案改为“未设置保底排班时，患者可能约不到您的号源，确定稍后设置吗？”
- 前端 `doctor-workspace.js`：
  - 最低排班改为按未来 7 天按日聚合：无任何排班 → 提示“未来 7 天尚未排班”；存在某日 `0 < 时段数 < 6` → 提示“M月D日仅 Y 个时段（需 6 个）”。`scheduleWarning=schedules.length` 时用实际最差日期。
  - “排班不足”卡片 `bindtap` 改为跳 `/pages/doctor-minimum-schedule/doctor-minimum-schedule?date={{...}}`，不再跳 my-schedule。
  - `getStatusText/getStatusClass` 补 status=3。
- 后端 `ScheduleController.create`：传入 `schedule.getStatus()`；`ScheduleService.createSchedule` 增 `status` 参数（默认 1）。
- 后端 `ScheduleService.createSchedule` 增区间重叠校验：同医院/医生/日期下，`timeSlot` 与既有排班区间相交 → “该时段与已有排班重叠”。

### 3.4 排班规则（全医院统一）

- 每位医生每个出诊日排班总时长 **≥ 120 分钟**。
- 号源段时长可自定义：15/20/30/45/60 分钟，**默认 20 分钟**。
- **每段固定接待 2 位患者**。
- 实现：新增排班按所选时长自动拆分为多段（`my-schedule`）；保底排班每段 2 人；医生工作台按“分钟数”判定是否达标；患者端隐藏已约满段；后端 `maxPatients` 默认值改为 2。
- 存量历史排班不做自动换算（1 小时块/10 人段保持原样），避免把整块号源误缩成 2 人。

### 3.5 明确不做
- 不迁移历史问卷数据、不改患者端存储结构。
- 不改周排班确认（schedule-weekly-confirm）逻辑。
- 不做分页/索引类性能重构（F15 只保留现状）。

## 4. 验证

1. JS 语法：对改动文件逐个 `node --check`。
2. 后端：`mvn -q -DskipTests package` 编译通过；重启 18080。
3. 接口：
   - 医生登录 → `GET /api/appointments/doctor/by-username/{u}` 含 `deptId/deptName/doctor`。
   - `POST /api/schedules` `{status:0}` → 返回 data.status=0；重叠时段 → success=false。
   - 非本人医生 `POST /api/appointments/{id}/complete` → success=false。
   - 院管 `GET /api/appointments/admin/all` 仅本院。
4. 小程序：Ctrl+B 编译无报错；回归
   - 医生工作台：科室显示、问卷弹层有题目与答案、“排班不足”进保底页、保底页选日期后已排时段置灰、保存成功。
   - 患者端：预约问卷提交后医生端可读；patient-detail 问卷题干与患者所选科室一致。

## 5. 需用户确认

- 是否同意「最低排班 = 每个出诊日至少 6 个 20 分钟时段（2 小时）」这一规则，以及“未来 7 天按日检查”的判定范围。

---

# 6. 应急通道改造：申请即开通 + 事后稽核（2026-11-15）

## 6.1 背景与已确认决策

旧流程（医生提交 → 院管 5 分钟审批窗口 → 自动放行）仍以院管动作为前提，违背“院管失联时应急”的设计目的，已否决。

用户确认的最终流程：

1. **取消一切前置审批**：医生提交申请后立即签发 2 小时应急令牌，院管不批准、不拒绝。
2. **同步知会**：院管工作台收到“应急通道使用提醒”，只做知情、证件核对、随时终止/冻结。
3. **卫健委事后稽核**：医生提交结束报告后，卫健委 2 小时内确认归档或驳回异常；超时未确认标记逾期。
4. **院长不新开号**：院长/院领导就是现有 `HOSPITAL_ADMIN` 账号，不新增账号、不新增审批角色。
5. **使用资格**：仅“主任医师”职称且 `doctor.status = 1` 的注册医生可申请；后端强制校验，前端隐藏入口只是辅助。
6. **入口隐藏**：医生工作台删除可见的“应急科室权限”菜单，改为长按顶部头像/姓名区域触发申请入口；仅主任医师可用，无资格者调用接口被拒绝。
7. **2 小时硬上限**，不可续期；结束报告 24 小时内提交；证件首次上传后归档复用。
8. 保留软删除/回收站、修改前快照、单会话 20 次/全院每日 40 次写上限、DELETE 硬拦截、一键冻结/解冻、全链路审计。

## 6.2 状态机（`emergency_request.status`）

| 值 | 含义 | 进入条件 | 退出 |
|---|---|---|---|
| 0 | 使用中·待核对 | 提交申请即进入，令牌 2 小时有效 | 到期→1；医生提前交报告→2；院管/卫健委终止→5 |
| 1 | 已结束·待报告 | 令牌到期（status 0 + 到期） | 24h 内交报告→2；超 24h→6 |
| 2 | 待卫健委确认 | 医生提交结束报告 | 卫健委确认→3；驳回→4；超 confirm_deadline→7 |
| 3 | 已确认归档 | 卫健委确认 | 终态 |
| 4 | 已驳回·异常 | 卫健委驳回/证件不一致 | 终态，同时冻结本院应急通道 |
| 5 | 已中止 | 院管/卫健委强制终止、旧流程遗留数据 | 终态 |
| 6 | 逾期未交报告 | status 1 超 report_deadline | 补交报告→2 |
| 7 | 逾期未确认 | status 2 超 confirm_deadline | 卫健委确认→3/驳回→4 |

- `report_deadline = token_expire_time + 24h`（到期转入 1 时写入）。
- `confirm_deadline = report_time + 2h`（提交报告时写入）。
- 存在 `status = 7` 的医院，拒绝新的自助申请，直到卫健委处理；存在 `status = 6` 的医生，拒绝其再次申请，直到补交报告。

## 6.3 数据库迁移（一次性，部署前执行）

文件：`hospital-api/src/main/resources/emergency_v2.sql`（新增，不自动执行，手工在 `hospital_db` 运行）。

```sql
ALTER TABLE emergency_request
  ADD COLUMN doctor_title        VARCHAR(50)  NULL,
  ADD COLUMN cert_id_path        VARCHAR(255) NULL,
  ADD COLUMN cert_license_path   VARCHAR(255) NULL,
  ADD COLUMN cert_verified       INT NOT NULL DEFAULT 0,
  ADD COLUMN cert_verified_by    VARCHAR(40)  NULL,
  ADD COLUMN cert_verified_time  DATETIME     NULL,
  ADD COLUMN cert_verify_remark  VARCHAR(255) NULL,
  ADD COLUMN auto_grant          INT NOT NULL DEFAULT 1,
  ADD COLUMN report_scope        TEXT         NULL,
  ADD COLUMN report_impact       TEXT         NULL,
  ADD COLUMN report_complete     INT NOT NULL DEFAULT 0,
  ADD COLUMN report_time         DATETIME     NULL,
  ADD COLUMN report_deadline     DATETIME     NULL,
  ADD COLUMN confirm_deadline    DATETIME     NULL,
  ADD COLUMN confirm_by          VARCHAR(40)  NULL,
  ADD COLUMN confirm_by_name     VARCHAR(80)  NULL,
  ADD COLUMN confirm_time        DATETIME     NULL,
  ADD COLUMN confirm_remark      VARCHAR(255) NULL,
  ADD COLUMN end_time            DATETIME     NULL,
  ADD COLUMN end_reason          VARCHAR(255) NULL;

ALTER TABLE emergency_request
  ADD KEY idx_er_status_expire (status, token_expire_time),
  ADD KEY idx_er_status_report (status, report_deadline),
  ADD KEY idx_er_status_confirm (status, confirm_deadline);

-- 旧审批流遗留数据统一关闭，避免与新状态语义冲突
UPDATE emergency_request
   SET status = 5,
       end_time = NOW(),
       end_reason = '旧审批流遗留，已停用'
 WHERE status IN (0, 1, 2, 3)
   AND end_time IS NULL;

CREATE TABLE emergency_operation (
  id          VARCHAR(40)  NOT NULL,
  request_id  VARCHAR(40)  NULL,
  hospital_id VARCHAR(20)  NULL,
  doctor_id   VARCHAR(40)  NULL,
  token       VARCHAR(80)  NULL,
  method      VARCHAR(10)  NULL,
  path        VARCHAR(255) NULL,
  target_type VARCHAR(20)  NULL,
  target_id   VARCHAR(40)  NULL,
  summary     VARCHAR(255) NULL,
  create_time DATETIME     NULL,
  PRIMARY KEY (id),
  KEY idx_eo_request (request_id, create_time),
  KEY idx_eo_token (token, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

## 6.4 后端改动（精确到文件与方法）

### 6.4.1 实体与仓库

1. `hospital-api/src/main/java/com/hospital/entity/EmergencyRequest.java`
   - 按 6.3 新增字段及 getter/setter；`status` 注释改为 6.2 状态机。
2. 新增 `hospital-api/src/main/java/com/hospital/entity/EmergencyOperation.java`
   - 与 `emergency_operation` 表一一对应，字段 `id/requestId/hospitalId/doctorId/token/method/path/targetType/targetId/summary/createTime`。
3. 新增 `hospital-api/src/main/java/com/hospital/repository/EmergencyOperationRepository.java`
   - `List<EmergencyOperation> findByRequestIdOrderByCreateTimeAsc(String requestId)`
   - `List<EmergencyOperation> findByTokenOrderByCreateTimeAsc(String token)`
4. `hospital-api/src/main/java/com/hospital/repository/EmergencyRequestRepository.java` 增加：
   - `List<EmergencyRequest> findByStatusAndTokenExpireTimeBefore(Integer status, LocalDateTime time)`
   - `List<EmergencyRequest> findByStatusAndReportDeadlineBefore(Integer status, LocalDateTime time)`
   - `List<EmergencyRequest> findByStatusAndConfirmDeadlineBefore(Integer status, LocalDateTime time)`
   - `Optional<EmergencyRequest> findFirstByDoctorIdAndCertIdPathIsNotNullOrderByCreateTimeDesc(String doctorId)`
   - `Optional<EmergencyRequest> findFirstByDoctorIdOrderByCreateTimeDesc(String doctorId)`
   - `List<EmergencyRequest> findByToken(String token)`
   - `List<EmergencyRequest> findByStatusInAndHospitalIdOrderByCreateTimeDesc(List<Integer> statuses, String hospitalId)`
   - `List<EmergencyRequest> findByStatusInOrderByCreateTimeDesc(List<Integer> statuses)`

### 6.4.2 服务层

`hospital-api/src/main/java/com/hospital/service/EmergencyService.java` 保留密钥/日志/登录方法，重写申请审批相关方法：

- `isEligible(Doctor doctor)`（私有）：`doctor != null && status == 1 && title != null && title.contains("主任医师") && !title.contains("副主任医师")`。
- `submitRequest(doctorId, password, ip, reason, certIdPath, certLicensePath)`：
  1. 医生存在、资格校验（不满足抛“应急通道仅限主任医师及以上职称的注册医生使用”）。
  2. 原因 ≥4 字、密码二次校验、医院存在、`emergencyGuardService.isLocked` 拦截。
  3. 医生存在 `status = 6` 的申请 → “存在未提交的应急结束报告，请先补交”。
  4. 本院存在 `status = 7` 的申请 → “本院有应急事项待卫健委确认，暂停自助申请”。
  5. 医生 24 小时一次、全院每日 3 次（沿用现有规则）。
  6. 证件：优先使用入参路径（校验文件存在于 `emergency-certs` 根目录下且非路径穿越）；否则复用该医生最近一条含证件的申请；都没有 → “首次使用请上传身份证人像面和医师资格证”。
  7. 立即生成令牌（`token_expire_time = now + 2h`），`status = 0`、`auto_grant = 1`、`cert_verified = 0`、`doctor_title`、证件路径落库。
  8. 写 `EmergencyLog`（operatorType=DOCTOR、operatorId=doctorId）、`tokenService.register(... ADMIN/EMERGENCY_ADMIN ...)`、审计 `EMERGENCY_REQUEST_SUBMIT`。
  9. 返回 `success/token/expireTime/requestId/status=0/hospitalId/hospitalName/certReady`。
- `getMyRequest(doctorId)`：
  - 返回资格 `eligible/eligibleMessage`、`certReady`（最近含证件申请存在即可）。
  - 惰性处理过期：0 且到期 → 1，写 `report_deadline = expire + 24h`；1 且超 `report_deadline` → 6；2 且超 `confirm_deadline` → 7。
  - `status = 0` 返回 token/expireTime；`needReport`（1/6/0 可提前结束）为 true；返回 `reportDeadline/statusText/doctorTitle/certVerified/reportTime` 等。
- `listRequests(hospitalId)`：返回本院近 7 天 `status in (0,1,2,3,4,5,6,7)` 的知会列表（最多 50 条）；字段含 `requestId/doctorId/doctorName/doctorTitle/reason/status/statusText/createTime/tokenExpireTime/certVerified/reportTime/reportDeadline/confirmDeadline/needReport/overdue...`，不含证件路径（证件走受控接口）。
- `getReportDraft(doctorId, requestId)`：校验申请归属；按 `emergency_operation` 生成自动改动范围（每行 `method + path`，并按 `department/schedule` 生成中文摘要）；返回 `autoScope/reason/startTime/expireTime/certVerified`。
- `submitReport(doctorId, requestId, scope, impact, complete)`：
  - 仅允许本人、`status in (0,1,6)`；`impact` ≥4 字、`complete = true`。
  - 若 `status = 0` 提前结束：`end_time = now`，吊销令牌。
  - 保存 `report_scope`（空则用自动范围）、`report_impact`、`report_complete = 1`、`report_time = now`、`confirm_deadline = now + 2h`、`status = 2`；审计 `EMERGENCY_REPORT_SUBMIT`。
- `verifyCert(requestId, verified, remark, verifierId, verifierName)`：院管/卫健委记录证件核对结论（1 一致 / 2 不一致），留痕。
- `terminate(requestId, operatorId, operatorName)`：院管/卫健委强制终止，`status = 5`、`end_time/end_reason`、`tokenService.revoke(token)`、审计。
- `supervisorPending()`：返回全部医院 `status in (2,7)` 列表，含医院名、医生姓名/职称、报告内容、证件核对状态、`overdue`。
- `confirm(requestId, certChecked, remark, operatorId, operatorName)`：
  - 仅 `status in (2,7)`；`certChecked` 必须为 true。
  - `status = 3`、`cert_verified = 1`、`confirm_by/by_name/time/remark` 落库、审计 `EMERGENCY_SUPERVISOR_CONFIRM`。
- `reject(requestId, reason, operatorId, operatorName)`：
  - 仅 `status in (2,7)`；`reason` 必填。
  - `status = 4`、`tokenService.revoke(token)`、`emergencyGuardService.freeze(hospitalId, 24, "卫健委驳回应急事项：" + reason)`、`tokenService.revokeEmergencyByHospital(hospitalId)`、审计 `EMERGENCY_SUPERVISOR_REJECT`。
- `processTimeouts()`（供定时任务调用）：批量执行 0→1、1→6、2→7，逐条写审计；`status = 7` 不冻结已有令牌，仅阻止新申请（提交时查询拦截）。
- `getCertPath(requestId, type)`：`type ∈ idCard|license`，校验文件存在、统一返回绝对路径。
- 删除 `approveRequest`、`rejectRequest` 方法。

新增 `hospital-api/src/main/java/com/hospital/service/EmergencyOperationService.java`：
- `record(String token, String hospitalId, String method, String path)`：按 token 查申请，写 `emergency_operation`（`request_id/doctor_id` 可空），`target_type/target_id` 解析 `/api/departments/{id}`、`/api/schedules/{id}`，`summary` 生成“新增/修改/删除 科室|排班”。
- `listByRequestId(requestId)` 供报告草稿使用。

新增 `hospital-api/src/main/java/com/hospital/config/EmergencyRequestScheduler.java`：
- `@Scheduled(fixedDelay = 60000)` 调用 `emergencyService.processTimeouts()`。

### 6.4.3 控制器 `EmergencyController.java`

保留：`/status/{hospitalId}`、`/snapshots/*`、`/revoke-all/*`、`/unfreeze/*`、`/login`、`/logout`、`/validate-token`、`/generate-key/*`、`/renew-key/*`、`/invalidate-key/*`、`/logs`。

删除：`POST /request/{requestId}/approve`、`POST /request/{requestId}/reject`。

新增：

| 方法 | 路径 | 角色 | 说明 |
|---|---|---|---|
| POST | `/api/emergency/request/cert` | 医生 | multipart `file` + `type=idCard\|license`；存 `emergency-certs/{doctorId}/`，返回相对路径 `path`；jpg/jpeg/png/webp，≤8MB |
| POST | `/api/emergency/request/report` | 医生 | body `{requestId, scope, impact, complete}` |
| GET | `/api/emergency/request/report-draft` | 医生 | `requestId` 参数，返回自动改动范围 |
| GET | `/api/emergency/cert/{requestId}/{type}` | 院管/超管 | 返回图片流；`authContext.requireHospitalScope`；写审计 `EMERGENCY_CERT_VIEW`；应急账号拒绝 |
| POST | `/api/emergency/request/{requestId}/verify-cert` | 院管/超管 | body `{verified, remark}`；院管限本院 |
| POST | `/api/emergency/request/{requestId}/terminate` | 院管/超管 | 强制终止 |
| GET | `/api/emergency/supervisor/pending` | 超管 | 卫健委待确认队列 |
| POST | `/api/emergency/supervisor/{requestId}/confirm` | 超管 | body `{certChecked:true, remark}` |
| POST | `/api/emergency/supervisor/{requestId}/reject` | 超管 | body `{reason}` |

- 所有新增接口 `rejectEmergencyAccount()`（应急账号拒绝），按需 `authContext.requireHospitalScope` 或 `requireSuperAdmin`。
- 证件上传/查询路径统一用 `Paths.get("emergency-certs").toAbsolutePath()`，禁止 `..` 路径穿越。

### 6.4.4 鉴权 `AuthInterceptor.java`

规则顺序（新增项必须排在通用 `/request/` 之前）：

```java
new Rule("/api/emergency/request/submit", POST, DOCTOR_ONLY, false),
new Rule("/api/emergency/request/my", GET, DOCTOR_ONLY, false),
new Rule("/api/emergency/request/cert", POST, DOCTOR_ONLY, false),
new Rule("/api/emergency/request/report-draft", GET, DOCTOR_ONLY, false),
new Rule("/api/emergency/request/report", POST, DOCTOR_ONLY, false),
new Rule("/api/emergency/supervisor/", null, ADMIN_ONLY, false, true),
new Rule("/api/emergency/cert/", null, ADMIN_ONLY, false),
new Rule("/api/emergency/requests/", null, ADMIN_ONLY, false),
new Rule("/api/emergency/request/", null, ADMIN_ONLY, false),
```

在应急账号写操作分支中，`emergencyGuardService.assertWriteAllowed` 与快照之后调用 `emergencyOperationService.record(token, hospitalId, method, path)`，用于结束报告自动生成改动范围。

## 6.5 前端改动

### 6.5.1 医生端 `pages/doctor-workspace/`

- `doctor-workspace.wxml`
  - 删除“应急科室权限”菜单项（第 81-86 行）。
  - 在 `.user-info` 增加 `bindlongpress="applyEmergencyAccess"`，作为隐藏入口。
  - 新增待交报告提醒条 `wx:if="{{emergencyNeedReport}}"`，点击打开报告弹层。
  - 申请弹层：原因、密码、两个证件上传（身份证人像面/医师资格证）；已归档时显示“已归档，无需重复上传”。
  - 报告弹层：改动范围（预填自动生成、可编辑）、影响预估（必填）、“确认无未完成事项”勾选。
- `doctor-workspace.js`
  - 删除“等待院领导审批”分支；`applyEmergencyAccess` 按 `eligible / status / needReport / certReady` 分流。
  - 新增 `uploadEmergencyCert(type)`（`wx.chooseImage` + `wx.uploadFile`，手动带 `X-Auth-Token`，`formData.type`）。
  - 新增 `loadReportDraft/openReportForm/submitEmergencyReport/onReport...`。
  - `submitEmergencyRequest` 提交 `{reason, password, certIdPath, certLicensePath}`，成功后直接 `enterEmergencyDepartment(res.data)`（立即拿令牌）。
  - `onShow` 调 `loadEmergencyReminder()`（GET `/request/my`），仅在 `needReport` 时显示提醒。
- `doctor-workspace.wxss` 追加证件上传、报告弹层、提醒条样式。

### 6.5.2 院管工作台 `subpackages/admin/pages/admin-dashboard/`

- `admin-dashboard.js`
  - 轮询保留；文案改“应急通道使用提醒”；弹窗改“本院有 N 条应急通道使用记录，权限已自动开通，请核对证件/报告；异常可立即终止或冻结”。
  - `SUPER_ADMIN` 增加 `pendingConfirmCount`：`onShow` 调 `/emergency/supervisor/pending`，卡片提示“待确认应急报告”。
- `admin-dashboard.wxml`
  - 院管通知卡文案改“应急通道使用提醒 / 已自动开通，请核对与监督”，按钮“去查看”。
  - 超管新增待确认通知卡，点击进入应急密钥管理页（无 hospitalId）。

### 6.5.3 应急管理页 `pages/key-management/`

- `key-management.js`
  - `loadEmergencyRequests` 数据改为知会列表；删除 `approveEmergencyRequest/rejectEmergencyRequest`；新增 `terminateEmergencyRequest`、`verifyEmergencyCert`、`previewEmergencyCert`（`wx.downloadFile` + `wx.previewImage`，手动带 `X-Auth-Token`）。
  - `onLoad` 无 `hospitalId`（超管）时调用 `loadSupervisorPending`；新增 `confirmSupervisorRequest`、`rejectSupervisorRequest`（驳回原因用 `wx.showModal({editable:true})`）。
- `key-management.wxml`
  - “待审批应急申请”改为“应急通道知情”，展示状态、证件核对、报告、终止按钮。
  - 超管顶部新增“待卫健委确认”卡片：医院、医生/职称、报告、证件预览、确认归档/驳回异常。
- `key-management.wxss` 追加状态标签、证件按钮、超管卡片样式。

## 6.6 验证步骤

1. 执行 `emergency_v2.sql`，`SHOW CREATE TABLE` 确认字段与表存在。
2. 停止当前 `java -jar` 进程，`mvn -q -DskipTests package`，重启 `18080`。
3. 接口实测：
   - `wangjianan/123456`（DOC001，主任医师）登录 → `GET /request/my` 显示 `eligible=true`。
   - 未上传证件 `POST /request/submit` → 失败“首次使用请上传…”。
   - 上传两张证件 → 提交 → 立即返回 `token/expireTime`，`status=0`，无审批等待。
   - `GET /request/my` 带 token；院管 `GET /requests/{hospitalId}` 可见该记录且 `status=0`。
   - 用 SQL 将 `token_expire_time` 改为过去 1 分钟，等定时任务（≤60s）→ `status=1`、`needReport=true`。
   - `GET /report-draft` 返回自动改动范围；`POST /report` → `status=2`。
   - 超管 `GET /supervisor/pending` 可见；`POST /supervisor/{id}/confirm {certChecked:true}` → `status=3`。
   - 再造一条走驳回 → `status=4`、医院冻结、令牌失效。
   - DOC002（副主任医师）提交 → 被拒“仅限主任医师”。
   - 24h/2h 逾期用 SQL 调整 deadline 验证 `status=6/7` 与再次申请拦截。
4. 前端：对改动的 `.js` 逐个 `node --check`；微信开发者工具编译无报错；回归长按入口、证件上传、即时令牌、报告提交、院管知情、超管确认。

## 6.7 明确不做

- 不开新的院长账号或审批角色；院管不参与批准/拒绝。
- 不做令牌续期、不做多级审批、不做 5 分钟窗口。
- 不改应急密钥（daily key/常设码）登录链路、软删除、快照回滚逻辑。
- 不新增小程序页面；卫健委队列复用 `key-management`。
- 历史遗留申请统一关闭，不做数据回溯审批。
