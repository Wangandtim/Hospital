# 架构说明（ARCHITECTURE）

版本：v1.0（2026-11-11 重构后）　适用工程：`d:\Desktop\Hospital`

---

## 1. 系统分层

```
┌──────────────────────────────────────────────────────────────┐
│  客户端层                                                     │
│  · 微信小程序（患者端 + 医院/医生工作台）                        │
│  · Web 后台控制台（static/index.html + css/app.css + js/app.js）│
└───────────────────────────┬──────────────────────────────────┘
                            │ HTTP :8080（唯一对外入口）
┌───────────────────────────▼──────────────────────────────────┐
│  接入层  hospital-api（Spring Boot 3.3.5 / Java 17）           │
│  · controller（REST）  · service（业务）                       │
│  · repository（Spring Data JPA）  · config / exception         │
│  · CORS 仅对 /api/** 开放                                      │
└───────┬───────────────────────────────────┬──────────────────┘
        │ JDBC（HikariCP 连接池）             │ HTTP（RestTemplate，8s 超时）
┌───────▼──────────────┐          ┌─────────▼──────────────────┐
│ MySQL 8  :3306       │          │  hospital-crawler :5000     │
│ hospital_db          │          │  Flask，仅监听 127.0.0.1     │
│ utf8mb4_unicode_ci   │          │  · /api/health              │
│ 23 张业务表           │          │  · /api/hospital/nearby     │
└──────────────────────┘          └─────────┬──────────────────┘
                                            │ HTTPS
                                  ┌─────────▼──────────┐
                                  │ 高德地图 Web 服务    │
                                  └────────────────────┘
```

设计原则：

1. **单入口**：所有客户端只认 8080。爬虫不对外暴露，统一由 `/api/nearby/**` 代理，避免端口与鉴权分散。
2. **单数据源**：仅一个 MySQL 实例、一个业务库，连接参数集中在 `application.yml`。
3. **配置外置**：端口、库地址、密码、第三方 Key 均可用环境变量覆盖，配置不散落在代码里。

---

## 2. 目录职责

| 目录 | 职责 | 关键文件 |
|---|---|---|
| `hospital-api/` | 后端服务与静态后台 | `application.yml`、`controller/`、`service/`、`repository/`、`entity/` |
| `hospital-api/src/main/resources/static/` | 后台控制台 | `index.html`（结构）、`css/app.css`（样式）、`js/app.js`（逻辑） |
| `hospital-crawler/` | 周边医院数据采集与降级 | `app.py` |
| `hospital-miniprogram/` | 微信小程序 | `config/index.js`、`utils/request.js`、`subpackages/admin/` |
| `db/` | 数据库规范资产 | `migration/`、`rollback/`、`schema/` |
| `delivery/` | 交付与运维脚本 | `config/env.bat`、`start-all.bat`、`stop-all.bat` |
| `docs/` | 技术文档 | 本文件所在目录 |
| `backup/` | 备份与回滚资产 | 源码包、数据库 dump |

---

## 3. 数据库架构

### 3.1 统一规范

| 项 | 值 |
|---|---|
| 实例 / 库 | `127.0.0.1:3306` / `hospital_db` |
| 字符集 / 排序规则 | `utf8mb4` / `utf8mb4_unicode_ci`（库、表、列三层一致） |
| 引擎 | InnoDB |
| 连接池 | HikariCP（`hospital-hikari`，max 20 / min idle 5 / conn-timeout 30s / max-lifetime 30min） |
| 归档库 | `hospital_db_archive` |
| Schema 变更 | 手工迁移脚本 + `ddl-auto=none`，禁止 Hibernate 自动改表 |

### 3.2 表清单（23 张，均有实体映射）

| 表 | 说明 | 主键 |
|---|---|---|
| `hospital` | 医院 | `hospital_id` |
| `department` | 科室 | `dept_id` |
| `doctor` | 医生 | `doctor_id` |
| `doctor_detail` | 医生详情 | 关联 `doctor` |
| `doctor_schedule_setting` | 医生排班模板 | 自增/业务键 |
| `schedule` | 排班（约 11.6 万行） | `schedule_id` |
| `appointment` | 预约单 | `appointment_id` |
| `patient` | 患者 | `patient_id` |
| `admin` | 管理员 | `admin_id` |
| `wechat_user` | 微信用户 | `openid` |
| `hospital_key` | 医院每日口令 | `daily_key` |
| `emergency_log` | 急诊登录日志 | 自增 |
| `payment_record` | 支付记录 | `payment_id` |
| `audit_log` | 操作审计 | 自增 `id` |
| `emergency_request` | 急诊申请 | `id` |
| `emergency_operation` | 急诊操作记录 | `id` |
| `emergency_snapshot` | 急诊快照 | `id` |
| `hospital_article` | 医院文章 | `article_id` |
| `patient_feedback` | 患者反馈 | `feedback_id` |
| `doctor_duty_rule` | 医生值班规则 | `rule_id` |
| `schedule_weekly_confirm` | 周排班确认 | `confirm_id` |
| `triage_session` | 分诊会话（Agent） | `triage_id` |
| `triage_lock` | 分诊号源锁（Agent） | `lock_id` |

已下线（归档至 `hospital_db_archive`）：`user`、`registration_attachment`、`emergency_login_log`。

### 3.3 索引策略

- **高频等值 + 范围组合前置**：`schedule(doctor_id, work_date)`、`schedule(hospital_id, work_date)`、`appointment(patient_id, create_time)`、`appointment(openid, create_time)`。
- **外键列全部建索引**：`doctor(hospital_id)`、`doctor(dept_id)`、`department(hospital_id)`、`appointment(schedule_id)`、`doctor_schedule_setting(doctor_id)`。
- **筛选维度**：`hospital(province, city, district)`、`hospital(status)`。
- **不建**：低基数单列（如 `status` 单独使用）、`LIKE '%x%'` 模糊列（索引无效）。
- 命名：`idx_<表>_<列缩写>`，唯一键 `uk_<表>_<列>`，外键 `fk_<表>_<父表>`。

索引变更必须走 `db/migration/` 新脚本，并提供对应 `db/rollback/` 回滚脚本。

---

## 4. 后端架构

### 4.1 配置模型

唯一配置文件：`hospital-api/src/main/resources/application.yml`

| 配置项 | 默认值 | 环境变量 |
|---|---|---|
| `server.port` | 8080 | `SERVER_PORT` |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/hospital_db?...` | `DB_HOST` / `DB_PORT` / `DB_NAME` |
| 数据库账号 | `root` / 由 `DB_PASSWORD` 注入 | `DB_USER` / `DB_PASSWORD` |
| `crawler.base-url` | `http://127.0.0.1:5000` | `CRAWLER_BASE_URL` |
| `crawler.timeout-ms` | 8000 | `CRAWLER_TIMEOUT_MS` |
| `wechat.miniapp.*` | 内置 | `WECHAT_APP_ID` / `WECHAT_APP_SECRET` |
| `amap.key` | 内置 | `AMAP_KEY` |

已移除：`application.properties`、`application-enhanced.properties`（`ddl-auto=update` + `sql.init=always`，与主配置冲突）、`spring.profiles.active=dev`（无对应 profile 文件）、未使用的 Redis 依赖与 `RedisConfig`。

### 4.2 周边医院代理链路

```
小程序 ──GET /api/nearby/hospitals──▶ NearbyController
                                        │ POST /api/hospital/nearby（8s 超时）
                                        ▼
                                   hospital-crawler ──▶ 高德 API
                                        │ 失败时降级本地 10 家医院
                                        ▼
                              原样透传 {success,data,source,count,totalCount}
```

- 爬虫不可用时不抛 500，返回 `success=false` + 空列表，前端提示而非白屏。
- `/api/nearby/health` 聚合探测爬虫状态，供部署自检使用。
- `NearbyHospitalService`（高德 + 爬虫信息增强）保留，供后续按需启用。

### 4.3 安全与跨域

- `WebMvcConfig` 对 `/api/**` 开放 CORS（保留既有行为，供小程序与后台控制台调用）。
- 鉴权沿用现有 token（`X-Openid` 等）机制，本次未改动。

---

## 5. 小程序架构

```
app.js ──require──▶ config/index.js（唯一接口地址）
   │                      │
   ├── globalData.baseUrl ┘
   │
pages/*.js ──require──▶ utils/request.js（统一超时/重试/错误提示）
                              │
                              └──▶ config.apiBase + path
```

- **主包**：37 个页面（患者端 + 医生工作台），576.8 KB。
- **分包 `subpackages/admin`**：9 个页面（后台管理 / 急诊），92.3 KB，`preloadRule` 在登录页与「我的」页预下载。
- **按需注入**：`lazyCodeLoading: "requiredComponents"`，减少首屏脚本解析。
- **设计 token**：`app.wxss` 末尾统一定义色板/间距/圆角/字号，新页面必须引用变量。

接口地址覆盖方式（真机调试）：

```js
wx.setStorageSync('API_BASE', 'http://192.168.1.10:8080');  // 生效
wx.removeStorageSync('API_BASE');                            // 恢复默认
```

---

## 6. 后台控制台架构

| 文件 | 大小 | 职责 |
|---|---|---|
| `static/index.html` | 17.6 KB | DOM 结构 + 资源引用 |
| `static/css/app.css` | 8.1 KB | 全部样式 |
| `static/js/app.js` | 41.1 KB | 交互逻辑（`API_BASE = '/api'` 同源） |

拆分收益：HTML 体积下降 73%，CSS/JS 可被浏览器独立缓存；后续视觉改造只需替换 `app.css`，不触碰结构。

---

## 7. 关键约束（后续开发必须遵守）

1. 新增端口必须先在 `docs/PORT_REGISTRY.md` 登记，禁止在代码或脚本里硬编码。
2. 新增表/索引必须同时提交 `migration` 与 `rollback` 脚本，并更新 `db/schema/hospital_db_schema.sql`。
3. 小程序禁止直连 5000/8081，新增接口一律走 `config.apiBase`。
4. 后台控制台禁止内联 `<style>`/`<script>`，样式与逻辑分别落到 `css/`、`js/`。
5. 禁止 `spring.jpa.hibernate.ddl-auto` 取 `update`/`create`。
