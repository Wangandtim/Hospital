# 通用分诊预约 Agent（含专家号判定与信息不全处理）实施 Spec

## 1. 目标与范围

在现有患者端预约链路上落地一个可运行的「分诊预约 Agent」：

1. 患者用自然语言描述不适，系统输出**科室 + 三档置信度 + 是否需要专家号**。
2. 红旗症状先于一切：命中即终止预约，进入独立急诊页。
3. 用户信息不全时按**分级策略**处理：先阻塞式补最少必要信息，再给出结论；非必要信息不阻塞。
4. 高/中置信度可锁定唯一医院与号源，走现有挂号缴费链路；低置信度关闭预约入口。

本稿只覆盖第一版可运行闭环，不做 LLM 接入、不做子女代付/医保清算、不做医院确认失败退款工作流。

## 2. 现状核对结论（已读代码与数据）

- 后端 `Spring Boot 3.3.5 + JPA + MySQL`，`ddl-auto: none`，新表必须手写 DDL 并执行。
- 号源表 `schedule` 已有原子扣减 `ScheduleRepository.incrementBookedCount` / `decrementBookedCount`，并有 `@Version`，可直接作为锁号底座。
- 现有预约创建 `POST /api/appointments` 会原子扣减号源；若 Agent 已锁号，不能再扣一次。
- 支付链路完整：`POST /api/payment/pay` 幂等返回原订单；`/api/payment/prepare/{appointmentId}` 提供费用。
- 患者档案字段：`name / gender / age / phone / idCard / medicalHistory`；`hasCompleteInfo = name + gender + age>0`，登录接口返回。
- 医生职称实测：主任医师 461、副主任医师 455、主治医师 409、住院医师 387；少量历史乱码职称按「非专家」处理。
- 科室实测 28 个（含放射/检验/药剂/麻醉/超声/重症等非门诊科室，路由时必须排除）。
- 51 家医院；三甲 19、三级乙等 1、二级甲等 1、未填 30；省市区与经纬度齐全。
- 未来可用号源 1260 个（截至 2026-11-22）。
- 小程序已有 `utils/elderly.js` 长辈模式与全局 `.elderly` 样式，可直接复用；`settings.js` 的 `goAgent()` 目前是「开发中」占位。

## 3. 架构决策（多方案评估后定稿）

### 3.1 规则引擎 vs LLM

**定为后端确定性规则引擎，LLM 不参与任何决策。**

| 方案 | 结论 | 原因 |
|---|---|---|
| LLM 直接输出科室/置信度 | 否决 | 不可审计、不可回归、置信度不可解释；红旗漏判不可接受 |
| 纯规则引擎 | 采用 | 科室集合封闭（28 个）、可版本化、可单元测试、可解释 |
| LLM 解析 + 规则决策 | 后续可选 | 仅用于口语归一化；本期先做关键词/否定词/正则，不引入外部依赖 |

规则版本号写入会话：`triage-rules-2026.11-v1`。

### 3.2 专家号判定

数据层没有「专家门诊」标记，唯一可用信号是**医生职称**：

- `主任医师` 专家（rank 3），`副主任医师` 专家（rank 2），`主治医师` rank 1，`住院医师` rank 0，未知/乱码 rank 0。
- 另结合：年龄（≥75 强、65–74 弱、≤3 强）、既往史（高血压/糖尿病/心脏病/肿瘤/手术）、慢病与复查关键词、症状持续时间、多科室竞争。

输出三档：

- `REQUIRED`：复杂/慢病复查/肿瘤等，优先专家；无专家号时降级并明确告知。
- `RECOMMENDED`：高龄、幼儿、反复不愈，建议专家。
- `NOT_NEEDED`：急性、单一、明确症状，优先普通号，避免过度医疗。

**专家不是加价理由**：只有判定为需要时才锁定专家号；否则优先主治/住院医师。

### 3.3 信息不全分级处理

| 级别 | 字段 | 策略 |
|---|---|---|
| 阻塞（分诊前必须补） | 年龄、性别 | 返回 `NEED_PROFILE`，只问这两个；年龄用 4 个大按钮年龄段（儿童/15-44/45-64/65+），性别 2 个按钮；女方 15-49 且症状涉及下腹/月经/怀孕时追加「是否怀孕」 |
| 降级（可继续，但降置信度） | 既往史 | 缺失仅 -0.05 置信度并提示可补充，不阻塞 |
| 预约前阻塞（分诊可继续） | 姓名、手机号 | 返回 `profileCompleteness.bookingMissing`；点「去付款」时引导到现有 `pages/patient-info/patient-info` |
| 提醒 | 身份证 | 不阻塞现有预约接口，提示实名就诊需要 |

**替长辈问诊**：`subject=ELDER` 时年龄/性别以请求值为准（不能拿子女账号档案当老人），会话记录 `subject=ELDER`。若账号本人年龄与所填老人年龄明显不符，`bookingAllowed=false`，提示「请用长辈本人账号或由家人协助」，避免把老人挂号到子女档案。只有 `subject=SELF` 且档案缺失时，才把补充的精确年龄/性别回写患者档案（只补空值，不覆盖）。

年龄段是估算值：使用年龄段时置信度 -0.05，并提示「档案年龄为估算，建议完善」。

### 3.4 锁号与幂等

- 锁号 TTL 10 分钟，服务端时间为准；同一患者同时只保留一个有效锁，创建新锁前释放旧锁。
- 锁号通过 `schedule.booked_count + 1` 原子占位；释放通过 `booked_count - 1`，仅对 `status=1` 的锁执行，避免重复释放。
- 幂等键：`triageId`。同一会话重复锁号返回同一有效锁。
- 预约单由 `POST /api/triage/{id}/book` 从锁生成，**不再扣减号源**；生成后锁置为 `CONSUMED`。
- 定时任务每 60 秒释放过期锁（`status=1 && expire_at < now`）。
- 本期不扩展「已出单未支付自动退款」；预约单生成后沿用现有支付与「我的预约」逻辑（与现状一致）。

### 3.5 正确性门槛（按更高要求定稿）

为提高分诊正确性，第一版采用**更严格的档位与取证条件**：

- 三档阈值：`HIGH ≥ 0.90`；`MEDIUM 0.70–0.90`；`LOW < 0.70`。
- 进入 HIGH 必须同时满足：
  1. 首位科室至少命中 **2 个不同具体关键词**（强关键词 1 个 + 具体关键词 1 个也可）；
  2. 有持续时间或伴随症状描述；
  3. 首位得分 ≥ 次位 **2 倍**；
  4. 无歧义项：无「两个科室接近」、无年龄段估算、无孕产状态未知；
  5. 年龄与性别为精确值（非年龄段）。
- 任一条件不满足，最高只能判 MEDIUM，必须让用户确认或补充。
- `ageBand` 估算 → 置信度 -0.05 且封顶 MEDIUM。
- 女性 + 下腹/月经/孕产相关症状 + 怀孕状态未知 → 封顶 MEDIUM。
- 专家门槛同步收紧：`REQUIRED ≥ 5`；`RECOMMENDED 3–4`；其余 `NOT_NEEDED`。
- 红旗规则宁可多拦：命中后一律终止，不因置信度高而放行。

### 3.6 长辈模式语音入口

长辈模式首页把「预约挂号」主按钮改为「**语音 Agent 预约**」，进入分诊页的语音态：

- 首屏只有一个大话筒（≥200rpx）与长辈风格提示：「按住话筒，说出哪里不舒服」。
- 按住录音、松开结束；录音仅用于可能的语音转写，最长 30 秒。
- 语音转写为**可插拔能力**：未配置微信同传/云端 ASR 时，不伪造识别结果，改为展示同风格的大号「常见不适」按钮（如咳嗽发热、头晕、胃不舒服、腰腿疼），点选后组成症状文本再走同一规则引擎。
- 语音态与文本态共用同一状态机、同一红旗规则、同一三档判定；长辈模式不显示置信度数字，只显示「已经判断好了 / 还需要再问几句」。
- 标准模式仍以文本输入为主，不强制语音。

### 3.7 安全与隐私

- 红旗规则先于科室映射；否定词（没有/无/不/未）前置 6 字内命中不触发。
- 不返回病名，只返回科室与依据；急诊页不展示预约入口。
- 会话存储症状原文与结构化结果，仅本人与管理员可访问（控制器校验令牌归属）。
- 所有结论保留命中规则、关键词、排除项，供复核。

## 4. 新增数据表（`hospital-api/src/main/resources/triage_v1.sql`）

### 4.1 `triage_session`

| 字段 | 类型 | 说明 |
|---|---|---|
| triage_id | varchar(36) PK | 会话 ID |
| patient_id | varchar(36) not null | 令牌归属患者 |
| subject | varchar(12) | SELF / ELDER |
| mode | varchar(12) | STANDARD / ELDER |
| raw_input | text | 原始症状 |
| normalized_input | varchar(512) | 归一化文本 |
| structured_symptoms | text | 结构化 JSON（关键词、时长、红旗排除） |
| red_flag | tinyint | 0/1 |
| red_flag_pattern | varchar(64) | 命中规则 |
| red_flag_reason | varchar(255) | 面向用户的原因 |
| age / age_band / gender / pregnant | int / varchar / varchar / varchar | 本次分诊使用的画像 |
| medical_history | varchar(512) | 既往史快照 |
| dept_name | varchar(64) | 推荐科室 |
| confidence | decimal(4,3) | 0–1 |
| confidence_tier | varchar(12) | HIGH/MEDIUM/LOW/RED_FLAG |
| need_expert | varchar(16) | REQUIRED/RECOMMENDED/NOT_NEEDED |
| expert_reason | varchar(255) | 专家判定依据 |
| hospital_id / doctor_id / schedule_id | varchar(36) | 唯一推荐 |
| match_score | int | 0–100 |
| match_reason | varchar(512) | 可解释依据 |
| alternatives | text | 降级备选 JSON |
| evidence | text | 规则命中依据 JSON |
| status | varchar(16) | NEED_PROFILE/ASSESSED/CONFIRMED/REJECTED/LOCKED/BOOKED/FAILED/EXPIRED |
| rule_version | varchar(32) | 规则版本 |
| create_time / update_time / confirm_time / book_time | datetime | 时间 |

索引：`idx_triage_session_patient (patient_id, create_time)`。

保留策略：每天 03:15 将超过 30 天仍未完成（`NEED_PROFILE/ASSESSED/FAILED/REJECTED`）的会话置为 `EXPIRED`；`LOCKED/BOOKED` 不受影响。`EXPIRED` 会话不再返回推荐，锁号/确认均被拒绝。

### 4.2 `triage_lock`

| 字段 | 类型 | 说明 |
|---|---|---|
| lock_id | varchar(36) PK | 锁 ID |
| triage_id | varchar(36) not null | 会话 |
| patient_id | varchar(36) | 归属 |
| schedule_id / doctor_id / hospital_id / dept_id | varchar(36) | 锁定对象 |
| status | tinyint | 1=LOCKED 2=CONSUMED 0=RELEASED/EXPIRED |
| expire_at | datetime | 到期 |
| release_reason | varchar(64) | EXPIRED/USER_CANCEL/REBOOK/BOOKED |
| appointment_id | varchar(36) | 生成预约 |
| create_time / release_time | datetime | 时间 |

索引：`idx_triage_lock_triage (triage_id, status)`、`idx_triage_lock_expire (status, expire_at)`。

表统一 `utf8mb4_unicode_ci`。

## 5. 后端改动清单

### 5.1 新增文件

| 文件 | 内容 |
|---|---|
| `entity/TriageSession.java` | 上述字段 + getter/setter |
| `entity/TriageLock.java` | 上述字段 + `isActive()` |
| `repository/TriageSessionRepository.java` | `findByTriageId`、`findTopByPatientIdAndStatusInOrderByCreateTimeDesc`、`findByStatusAndCreateTimeBefore` |
| `repository/TriageLockRepository.java` | `findFirstByTriageIdAndStatusOrderByCreateTimeDesc`、`findByPatientIdAndStatus`、`findByStatusAndExpireAtBefore` |
| `service/TriageRuleEngine.java` | 归一化、红旗、科室映射、置信度、专家判定、结构化症状；纯函数，可单测 |
| `service/TriageService.java` | 会话生命周期、画像合并、医院/医生/号源匹配评分、锁号/释放/出单、幂等 |
| `service/TriageLockScheduler.java` | `@Scheduled(fixedDelay = 60000)` 释放过期锁 |
| `controller/TriageController.java` | 6 个接口（见 5.3） |
| `resources/triage_v1.sql` | DDL |

### 5.2 修改文件

| 文件 | 改动 |
|---|---|
| `security/AuthInterceptor.java` | 增加规则 `new Rule("/api/triage/", null, ADMIN_OR_PATIENT, false)`，五个接口均需登录 |
| `resources/triage_v1.sql` | 新增并纳入部署脚本 |

不修改 `AppointmentController`（避免影响既有预约链路）；Agent 出单走独立接口。

### 5.3 接口契约

所有接口前缀 `/api/triage`，患者令牌归属即 `patientId`。

#### `POST /api/triage/assess`

请求：
```
{ symptoms, mode: "STANDARD|ELDER", subject: "SELF|ELDER",
  age?, gender?, pregnant?, medicalHistory?,
  latitude?, longitude? }
```
响应：
```
{ success, triageId, status: "NEED_PROFILE|RED_FLAG|OK",
  sessionStatus, confidence, confidenceTier, tierLabel,
  needProfile, missingFields:[{key,label,options:[{label,value}]}],
  redFlag:{pattern,reason,advice}, emergency:{hospitalName,phone},
  department:{name}, needExpert, expertNeeded, expertReason, expertLabel,
  evidence:[], followUpQuestions:[],
  recommendation:{hospitalId,hospitalName,level,distanceKm,distanceText,
    departmentName,doctorId,doctorName,doctorTitle,expert,fee,
    scheduleId,date,dateText,timeSlot,score,matchReason},
  alternatives:[], notices:[], profileCompleteness:{percent,bookingMissing:[],note},
  bookingAllowed, bookingBlockedReason,
  copy:{title,subtitle,primary,secondary,lowHint} }
```

规则：
- 缺失年龄或性别 → `NEED_PROFILE`，不匹配医院，不锁号。
- 红旗 → `RED_FLAG`，`recommendation=null`，返回最近医院总机。
- 低置信度 → `recommendation=null`，`followUpQuestions` 给出 2–3 个补充问题。
- 中置信度 → 返回推荐，`copy.secondary` 含「不是这个科室」。
- 专家判定与推荐医生同时输出，专家号不可用时在 `notices` 说明降级原因。

#### `POST /api/triage/{id}/profile`

请求：`{ age?, ageBand?, gender?, pregnant? }`；只允许本人会话；用于 `NEED_PROFILE` 后补信息并重新评估；`subject=SELF` 时回填空缺档案。

#### `POST /api/triage/{id}/confirm`

请求：`{}`；中置信度确认继续；记录 `status=CONFIRMED`，幂等。`BOOKED/LOCKED/REJECTED/EXPIRED` 直接拒绝，避免重复出单或复活已结束会话。

#### `POST /api/triage/{id}/reject`

请求：`{}`；用户「不是这个科室」；`status=REJECTED`，只释放本会话的有效锁；`BOOKED` 会话拒绝改状态（退号走「我的预约」）。

#### `POST /api/triage/{id}/lock`

请求：`{}`；创建/复用 10 分钟锁：
- 会话必须 `ASSESSED/CONFIRMED`，红旗/低置信度/需要补资料直接拒绝。
- 先释放该患者其他有效锁；推荐号源仍可约才锁；被抢则按降级链换号（同院同科下一号源 → 同科其他医院 → 次日），更新会话推荐。
- 返回 `{lockId, expireAt, scheduleId, doctorId, hospitalId, hospitalName, departmentName, doctorName, doctorTitle, date, timeSlot, fee, degraded, degradeReason}`。

#### `POST /api/triage/{id}/book`

请求：`{}`；校验有效锁未过期、令牌归属、重复预约；创建 `Appointment`（不再扣号源），锁置 `CONSUMED`，会话 `BOOKED`；幂等：已出单返回同一 `appointmentId`。

#### 附：`GET /api/triage/{id}`

返回会话详情（本人或管理员），含 `sessionStatus`，用于刷新恢复；前端仅恢复 `ASSESSED/NEED_PROFILE/FAILED/CONFIRMED`，其余状态清理本地记录。

### 5.4 规则细节

**科室路由关键词（命中权重，2 为强）**

- 呼吸内科：咳嗽、咳痰、发热、低烧、咽痛、鼻塞、流涕、气喘、支气管、肺炎
- 消化内科：胃痛、反酸、烧心、恶心、呕吐、腹泻、便秘、腹胀、消化不良、上腹痛
- 心血管内科：心慌、心悸、胸闷、高血压、心律不齐、水肿、气短
- 神经内科：头痛、头晕、眩晕、失眠、手麻、面瘫、偏头痛、记忆减退
- 内分泌科：血糖、糖尿病、甲状腺、口渴、多尿、体重下降
- 肾内科：尿频、尿急、尿痛、血尿、泡沫尿、肾
- 骨科：腰痛、颈肩痛、关节痛、骨折、扭伤、膝盖、背痛、腿痛
- 普外科/外科：右下腹痛、阑尾、疝气、肿块、伤口、肛周、便血
- 妇产科：月经、白带、怀孕、孕、产检、下腹痛（女性）
- 儿科：儿童、宝宝、孩子、婴儿（且年龄 ≤14 时优先）
- 眼科：眼痛、眼红、视力、流泪、视物模糊
- 耳鼻喉科：耳鸣、耳痛、听力、鼻窦、扁桃体、声音嘶哑
- 口腔科：牙痛、牙龈、口腔溃疡、智齿
- 皮肤科：皮疹、瘙痒、红斑、湿疹、水疱、脱发
- 肿瘤科：肿瘤、癌、化疗、放疗
- 老年病科：65+ 且无明确系统归属时的兜底
- 内科：乏力、低热等无明确归属时的兜底
- 排除：急诊科、放射科、检验科、药剂科、超声科、重症医学科、麻醉科（不参与门诊路由）

**置信度公式（clamp 0.05–0.97，按更高正确性要求定稿）**

- 基线 0.45
- 每命中一个不同关键词 +0.10（上限 +0.30）
- 强关键词 +0.12
- 首位科室得分 ≥ 次位 2 倍 +0.10
- 提到持续时间 +0.06
- 提到伴随症状 +0.05
- 年龄+性别为精确值 +0.05；年龄段按 0 计并另扣 0.05
- 首位/次位比值 <1.3 → -0.10
- 无明确科室 → -0.15
- 文本长度 <6 或仅一个泛词 → -0.10
- **HIGH 额外硬条件**：≥2 个不同具体关键词 + 有持续时间/伴随症状 + 首位≥次位 2 倍 + 无年龄段/孕产未知/科室竞争歧义；缺一即封顶 MEDIUM
- 三档：HIGH ≥0.90；MEDIUM 0.70–0.90；LOW <0.70

**红旗规则（任一命中，终止预约）**

1. 胸痛 +（大汗/压榨/呼吸困难/放射/濒死感）或「剧烈胸痛」
2. 呼吸困难/喘不上气/窒息/口唇发紫/不能平卧
3. 嘴歪/口角歪斜/说话不清/一侧肢体无力或麻木/突然昏迷或抽搐
4. 呕血/咯血/大出血/黑便/大量血便
5. 过敏 +（喉头肿/呼吸困难/休克/全身）
6. 怀孕 +（阴道流血/剧烈腹痛/破水/胎动减少）
7. 昏迷/意识不清/叫不醒/抽搐/惊厥
8. 车祸/高处坠落/头部外伤 +（昏迷/呕吐/出血）
9. 儿童高热 +（抽搐/惊厥/精神差）
10. 自杀/轻生/不想活

否定词「没有/无/不/未」在命中词前 6 字内则跳过该次命中。

**专家评分**

- 慢性/反复/长期/多年/术后/复查/控制不好 +3
- 肿瘤/癌/化疗/放疗/结节/肿块 +2
- 年龄 ≥75 +2；65–74 +1；≤3 +2
- 既往史含高血压/糖尿病/心脏病/肿瘤/手术 +2
- 多科室竞争或描述复杂 +1
- 总分 ≥5 `REQUIRED`；3–4 `RECOMMENDED`；否则 `NOT_NEEDED`
- 需要专家时医生优先 rank≥2；7 天内无专家号则降级并写入 `notices`（不降低科室置信度，但提示专家号不可得）

**医院评分（0–100，硬过滤先行）**

| 维度 | 权重 | 规则 |
|---|---|---|
| 强项匹配 | 35 | 科室国家级重点 35 / 省级 28 / 市级 22 / 通用重点 18 / 普通 10 |
| 距离 | 25 | ≤3km 25；≤5 21；≤10 15；≤20 8；≤30 4；>30 0；未知 12 |
| 费用 | 15 | ≤20 元 15；≤30 12；≤50 8；≤80 4；>80 0 |
| 号源 | 25 | 今天 25；明天 24；3 天内 20；5 天内 14；7 天内 8；无号排除 |

硬过滤：科室存在、医生 `status=1`、排班 `status=1` 且 `booked_count < max_patients`、非过去时段。

**降级链**：同院同科下一可用号源 → 同城同科其他医院 → 次日最早号源 → 无号返回失败并保留会话供重试。

## 6. 前端改动清单

### 6.1 新增页面

| 文件 | 内容 |
|---|---|
| `pages/triage/triage.js/.wxml/.wxss/.json` | 症状输入 → 补资料 → 三档结果 → 锁号/支付 → 成功 |
| `pages/triage-emergency/triage-emergency.js/.wxml/.wxss/.json` | 红旗独立页：拨打 120、医院总机、大字提示 |

### 6.2 修改页面

| 文件 | 改动 |
|---|---|
| `app.json` | 注册两个新页面 |
| `pages/index/index.wxml` | 搜索框下新增「智能分诊」卡片（标准/长辈两套字号） |
| `pages/index/index.js` | 新增 `goTriage()` |
| `pages/index/index.wxss` | 分诊卡片样式 |
| `pages/settings/settings.js` | `goAgent()` 从「开发中」改为进入分诊页 |

### 6.3 交互状态机

- 输入：症状 textarea + 快捷症状 chips + 标准/长辈切换 + 给自己/替长辈。
- 长辈语音入口：首页「语音 Agent 预约」进入 `?voice=1&mode=ELDER`；首屏为大话筒 + 同风格提示；按住录音松开结束，最长 30 秒。
- 语音转写可插拔：未配置 ASR 时展示大号「常见不适」按钮，不伪造识别；点选后组成症状文本走同一规则引擎。
- 未登录：提示去登录（分诊接口需要患者令牌）。
- `NEED_PROFILE`：大按钮年龄段、性别；女方 15–49 且相关症状时追问怀孕；提交后自动重判。
- `RED_FLAG`：跳独立急诊页，不显示预约入口。
- `HIGH`：展示唯一推荐（医院/科室/医生/时间/费用/专家依据），主按钮「去付款」；可展开依据。
- `MEDIUM`：先展示一句确认，按钮「继续挂号」/「不是这个科室」/「补充症状」。
- `LOW`：显示补充问题与「重新判断」「转人工」，预约入口关闭。
- 付款：锁号后弹出付款面板，显示 10 分钟倒计时；长辈模式只显示「号源为你保留中」；剩余 60 秒温和提示一次。
- 付款成功：进入成功页，可跳「我的预约」。
- 失败：锁号被抢/过期 → 提示并提供「重新锁定」；支付失败 → 可重试。
- 长辈模式：根节点 `elderly` class、大字号、大按钮；逻辑与标准模式完全一致；不显示置信度数字。

## 7. 验证计划

1. `node --check` 检查所有新增/修改 JS；校验 `app.json` JSON 合法。
2. `mvn -q -DskipTests package`（cwd `hospital-api`）。
3. 执行 `triage_v1.sql`，核对两表字段与索引。
4. 重启 `java -jar target/hospital-api-0.0.1-SNAPSHOT.jar --server.port=18080`。
5. 接口实测：
   - 高置信度：`咳嗽三天了，还有点低烧` → HIGH、呼吸内科、推荐非空。
   - 中置信度：`肚子不舒服，有点反酸` → MEDIUM、消化内科。
   - 低置信度：`不舒服` → LOW、无推荐、有追问。
   - 红旗：`胸痛伴大汗，喘不上气` → RED_FLAG、无推荐。
   - 专家：`血糖高，糖尿病很多年了，想复查` → 内分泌科、`needExpert=REQUIRED`。
   - 信息不全：注册 age 缺失账号 → `NEED_PROFILE`；补年龄段+性别后出结果。
   - 替长辈：填写 age=78 且账号年龄不符 → `bookingAllowed=false` 且有原因。
   - 锁号→出单→支付：成功；预约记录可见；测试后清理预约并恢复号源。
6. 前端静态检查：页面路径、事件绑定、登录拦截；小程序开发者工具编译由用户侧确认。

## 8. 风险与边界

- 关键词规则对口语覆盖有限，低置信度会偏保守（宁可多问一句，不放红旗）。
- 专家判定基于职称而非真实「专家门诊」排班，上线后应接入医院专家号源标记。
- 年龄段为估算，不能用于儿科/孕产等强年龄相关判断时会在提示中要求补精确年龄。
- 替长辈挂号受现有「一个账号一个患者档案」限制，本期选择如实阻断并提示，不伪造就诊人。
- 锁号只覆盖分诊阶段；出单后未支付不自动退款，沿用现状。

## 9. 验收与批准

本 Spec 已明确文件、接口、规则、数据表与验证步骤。请确认后进入实施；实施完成后按第 7 节逐项验证并回报结果。
