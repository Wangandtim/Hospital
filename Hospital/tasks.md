# tasks.md

- [x] T1 问卷统一（F1/F2）
  - 新增 `hospital-miniprogram/utils/questionnaire-config.js`（16 个科室问卷 + DEFAULT + `getConfig(deptId, deptName)` + `parse` 兼容扁平/嵌套）
  - [x] 匹配规则改为「科室名称优先、编号兜底」，覆盖全部医院与新增科室（`D1234` 等编号）
  - [x] 后端 `by-username` 补充 `deptName`，医生端/详情页均按名称解析
  - [x] 改 `pages/appointment/appointment.js` 使用共享配置（科室加载完成后再按名称匹配）
  - [x] 改 `pages/doctor-workspace/doctor-workspace.js` 使用 `parse`
  - [x] 改 `pages/questionnaire-list/questionnaire-list.js` 使用 `parse`
  - [x] 改 `pages/patient-detail/patient-detail.js` 使用 `parse`
  - 验收：医生端问卷弹层出现题目与答案；DEPT009~016 有题干
- [x] T2 医生端预约字段（F3）
  - [x] `AppointmentController#getAppointmentsByDoctorUsername` 增 `deptId/deptName/hospitalId/hospitalName/doctorTitle` 与 `doctor{...}`
  - 验收：工作台头部显示科室；patient-detail `deptId` 非空（接口实测通过）
- [x] T3 越权修复（F7/F8）
  - [x] `getAppointmentsByScheduleId` 校验医生/医院归属
  - [x] `complete` 校验医生归属 + 状态(1/3)
  - [x] `/admin/all`、`/admin/{id}/status` 院管按医院过滤
  - 验收：跨医生/跨医院请求返回 success=false（实测跨医生被拒）
- [x] T4 保底排班（F4/F5/F6/F11/F12/F13）
  - [x] `doctor-minimum-schedule.js` 逐条 POST `/api/schedules`、字段 workDate、解析 `{success,data}`
  - [x] `onLoad` 支持 `options.date`，移除全量禁用
  - [x] 阈值/文案统一（6/18 时段）
  - [x] `doctor-workspace.wxml/js` 排班不足卡片跳保底页并带日期
  - 验收：选日期后已占用时段置灰；保存成功且 my-schedule 可见
- [x] T5 后端排班创建（F9/F10）
  - [x] `ScheduleController#create` 传 status
  - [x] `ScheduleService#createSchedule` 增 status 参数与区间重叠校验
  - 验收：status=0 生效；重叠返回业务错误（接口实测通过）
- [x] T6 工作台最低排班判定与状态文案（F11/F14/F15）
  - [x] 未来 7 天按日聚合，告警含具体日期与时段数
  - [x] `getStatusText/getStatusClass` 补 3
  - 验收：无排班显示“尚未排班”；单日不足显示“M月D日仅 Y/6”
- [x] T8 排班规则（全医院）
  - [x] `my-schedule` 增加“每段时长”（默认 20 分钟），新增排班自动拆段，总时长 <2 小时拒绝
  - [x] 每段固定 2 人；保底排班与后端默认值同步为 2
  - [x] 工作台按分钟数判定“不足 120 分钟”，患者端隐藏已约满段
  - 验收：建 09:00-11:00/20 分钟 → 生成 6 个 20 分钟段，每段 2 人；接口默认 maxPatients=2（实测通过）
- [x] T7 验证
  - [x] `node --check` 所有改动 JS
  - [x] `mvn -q -DskipTests package`
  - [x] 重启后端 + curl 四项接口验收
  - [x] 小程序 Ctrl+B 编译 + 回归路径
- [x] T9 应急管理员全量检查
  - [x] 应急令牌接入统一鉴权（ADMIN/HOSPITAL_ADMIN + 本院范围，2 小时有效）
  - [x] 应急密钥生成/续期/作废仅限卫健委超管；应急日志仅管理员可查
  - [x] 令牌落库并精确校验；退出只失效当前会话；登录不再轮换当日密钥
  - [x] 应急登录页 UI 与管理员登录页统一（同一渐变背景 + 白色卡片 + 同款按钮）
  - [x] 修复科室搜索销毁原列表、200 状态误判成功
  - 验收：匿名生成密钥 401；应急令牌可增删科室；退出后令牌失效 401（实测通过）

# 应急通道改造（申请即开通 + 事后稽核）

- [x] T10 数据库迁移
  - 新增 `hospital-api/src/main/resources/emergency_v2.sql`（19 个字段 + 3 个索引 + `emergency_operation` 表 + 旧数据关闭）
  - 执行并核对 `SHOW CREATE TABLE emergency_request / emergency_operation`
  - 验收：字段、索引、表存在；旧 0/1/2/3 数据全部转为 5

- [x] T11 实体与仓库
  - `entity/EmergencyRequest.java` 新增证件/报告/确认/结束字段
  - 新增 `entity/EmergencyOperation.java`、`repository/EmergencyOperationRepository.java`
  - `EmergencyRequestRepository` 新增状态+到期/报告/确认查询、按 token/医生最近一条查询
  - 验收：`mvn -q -DskipTests package` 编译通过

- [x] T12 后端服务与控制器
  - `EmergencyService`：资格校验（主任医师 + status=1）、提交即签发、证件复用、报告草稿/提交、证件核对、终止、超管待确认/确认/驳回、超时处理、证件路径解析
  - 删除审批的 `approveRequest/rejectRequest`
  - 新增 `EmergencyOperationService`（应急写操作落库）
  - 新增 `config/EmergencyRequestScheduler`（每 60s 处理 0→1、1→6、2→7）
  - `EmergencyController`：证件上传、报告、报告草稿、受控证件查看、核对/终止、超管队列/确认/驳回
  - `AuthInterceptor`：新增 5 条医生规则 + 超管/证件/知会规则，并记录应急写操作
  - 验收：编译通过；接口权限（医生/院管/超管/应急账号）正确

- [x] T13 医生端前端
  - 删除“应急科室权限”菜单，顶部头像/姓名 `bindlongpress` 隐藏入口
  - 申请弹层：原因、密码、身份证/资格证上传（已归档则复用）
  - 提交成功立即进入应急科室页（无审批提示）
  - 结束报告弹层（自动改动范围 + 影响预估 + 无未完成事项勾选）与待交报告提醒
  - 验收：`node --check` 通过；长按可触发；申请即拿令牌；报告可提交

- [x] T14 院管/卫健委前端
  - `admin-dashboard`：院管“应急通道使用提醒”；超管待确认提醒卡与数量
  - `key-management`：待审批卡改“应急通道知情”，支持证件预览、标记核对、终止
  - 超管全局“待卫健委确认”队列：证件预览、确认归档、驳回异常
  - 验收：`node --check` 通过；院管可见即时记录；超管可确认/驳回

- [x] T15 构建与实测
  - 停止旧进程 → `mvn -q -DskipTests package` → 启动 18080
  - 实测：主任医师即时开通、无证件拦截、24h 报告、2h 逾期、超管确认/驳回、副主任医师被拒、院管冻结/终止
  - 前端所有改动 JS `node --check`；微信开发者工具编译
  - 验收：全流程跑通，旧审批接口不再存在
