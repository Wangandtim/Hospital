# 医院预约系统 - 完整项目说明

## 📋 项目概述

这是一个完整的医院预约系统，包含：
- **Java 后端** (Spring Boot) - 提供 API 接口
- **微信小程序** - 用户端界面
- **Python 爬虫服务** - 获取周边医院信息

## 🗂️ 项目结构

```
Hospital/
├── hospital-api/              # Java 后端项目
│   ├── src/main/java/com/hospital/
│   │   ├── controller/        # 控制器
│   │   ├── service/          # 服务层
│   │   ├── repository/       # 数据访问层
│   │   ├── entity/           # 实体类
│   │   ├── config/           # 配置类
│   │   └── exception/        # 异常处理
│   └── pom.xml
│
├── hospital-crawler/          # Python 爬虫服务
│   ├── app.py                # 主程序
│   ├── requirements.txt       # 依赖
│   └── README.md
│
└── hospital-miniprogram/      # 微信小程序
    ├── pages/                # 页面
    │   ├── index/            # 首页
    │   ├── appointment/      # 预约挂号
    │   ├── nearby/           # 周边医院
    │   ├── profile/          # 个人中心
    │   ├── my-appointments/  # 我的预约
    │   ├── department/       # 科室介绍
    │   └── doctor/           # 医生列表
    ├── app.js
    ├── app.json
    └── app.wxss
```

## 🚀 快速开始

### 1. 配置 Java 后端

修改 `hospital-api/src/main/resources/application.properties`：

```properties
# 微信小程序配置
wechat.miniapp.app-id=你的微信小程序AppID
wechat.miniapp.app-secret=你的微信小程序AppSecret

# 高德地图API Key
amap.key=你的高德地图API Key

# Python 爬虫服务地址
python.service.url=http://localhost:5000
```

### 2. 启动 Java 后端

```bash
cd hospital-api
mvn spring-boot:run
```

服务将在 `http://localhost:8080` 启动

### 3. 启动 Python 爬虫服务

```bash
cd hospital-crawler
pip install -r requirements.txt
python app.py
```

服务将在 `http://localhost:5000` 启动

### 4. 配置微信小程序

1. 打开微信开发者工具
2. 导入项目：选择 `hospital-miniprogram` 目录
3. 修改 `app.js` 中的 `baseUrl` 为你的后端地址
4. 点击"编译"运行

## 📱 小程序功能

### 首页 (pages/index)
- 搜索功能
- 快速入口（预约挂号、科室介绍、医生列表、周边医院）
- 热门科室展示
- 推荐医生展示

### 预约挂号 (pages/appointment)
- 四步预约流程：选择科室 → 选择医生 → 选择时间 → 确认预约
- 日期和时间选择
- 预约信息确认

### 周边医院 (pages/nearby)
- 获取用户位置
- 搜索周边医院（1-10公里范围）
- 显示医院距离、地址、电话
- 一键拨号和预约链接

### 我的预约 (pages/my-appointments)
- 预约状态筛选（全部、待就诊、已完成、已取消）
- 预约详情查看
- 取消预约功能

### 个人中心 (pages/profile)
- 微信登录
- 绑定患者信息
- 患者档案查看
- 设置、帮助、联系我们

### 科室介绍 (pages/department)
- 科室列表展示
- 科室详情查看

### 医生列表 (pages/doctor)
- 医生列表展示
- 按科室筛选
- 医生详情查看

## 🔧 API 接口说明

### 微信相关接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/wechat/login | 微信登录 |
| POST | /api/wechat/bind-patient | 绑定患者信息 |
| POST | /api/wechat/location | 更新位置 |
| GET | /api/wechat/nearby-hospitals | 获取周边医院 |

### 预约相关接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/appointments | 获取预约列表 |
| POST | /api/appointments | 创建预约 |
| POST | /api/appointments/{id}/cancel | 取消预约 |

### 科室和医生接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/departments | 获取科室列表 |
| GET | /api/departments/{id}/doctors | 获取科室医生 |
| GET | /api/doctors | 获取医生列表 |
| GET | /api/doctors/{id} | 获取医生详情 |

### Python 爬虫接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/hospital/info | 获取医院详情（急诊电话、预约链接） |
| GET | /api/health | 健康检查 |

## 📊 数据库设计

### 主要数据表

- `patient` - 患者表
- `doctor` - 医生表
- `department` - 科室表
- `appointment` - 预约表
- `schedule` - 排班表
- `wechat_user` - 微信用户表

## ⚙️ 技术栈

### Java 后端
- Spring Boot 3.3.5
- Spring Data JPA
- Spring Security
- MySQL 数据库
- Redis 缓存
- Lombok

### 微信小程序
- 微信原生小程序框架
- 微信登录 API
- 位置获取 API

### Python 爬虫
- Flask 3.0.0
- Requests 2.31.0
- BeautifulSoup4 4.12.2

## 🔑 关键配置

### 微信小程序
需要在微信公众平台配置：
- 服务器域名白名单
- 授权回调域名

### 高德地图
需要在高德开放平台申请 API Key：
- 周边搜索 API
- 地理编码 API

## 📝 注意事项

1. **隐私合规**：请确保遵守相关隐私法规，用户位置信息需获得明确授权
2. **爬虫使用**：请遵守目标网站的 robots.txt，合理设置请求间隔
3. **API Key**：请勿将真实的 API Key 提交到代码仓库
4. **HTTPS**：生产环境建议使用 HTTPS 协议

## 🎯 后续优化建议

1. 添加用户评价和反馈功能
2. 实现医生排班管理
3. 添加消息通知功能
4. 实现线上支付
5. 添加医院导航功能
6. 优化爬虫，增加更多数据源
7. 添加缓存机制，提升性能
8. 实现数据统计和分析

## 📞 技术支持

如有问题，请参考各模块的 README 文件或联系开发团队。

---

## 🔧 架构重构说明（2026-11-11）

### 统一端口（唯一入口 8080）

| 端口 | 服务 | 监听 | 说明 |
|---|---|---|---|
| 8080 | hospital-api | `0.0.0.0` | 对外唯一入口：`/api/**` + 后台控制台 `/` |
| 5000 | hospital-crawler | `127.0.0.1` | 仅本机，由 `/api/nearby/**` 代理，不对外暴露 |
| 3306 | MySQL | `127.0.0.1` | 业务库 `hospital_db` |

已废止 8081 / 3307 / 3308。端口集中配置在 `delivery/config/env.bat`，详见 `docs/PORT_REGISTRY.md`。

### 统一数据库

- 单一实例 `localhost:3306`，单一库 `hospital_db`，字符集 `utf8mb4`，排序规则 `utf8mb4_unicode_ci`。
- 连接池 HikariCP，配置集中在 `hospital-api/src/main/resources/application.yml`（支持 `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USER`/`DB_PASSWORD` 覆盖）。
- 12 张业务表，遗留表（`user`、`registration_attachment`、`emergency_login_log`）已归档至 `hospital_db_archive`。
- 新增 15 个业务索引；迁移脚本 `db/migration/`，回滚脚本 `db/rollback/`，权威结构 `db/schema/hospital_db_schema.sql`。

### 启动方式

```powershell
cd delivery
start-all.bat     # MySQL → 爬虫 → API，自动等待就绪
stop-all.bat      # 按端口停止
```

开发模式：`cd hospital-api && mvn spring-boot:run`。

### 前端工程化

- 小程序：接口地址唯一来源 `config/index.js`；统一请求层 `utils/request.js`；9 个管理页拆分至 `subpackages/admin`；开启 `lazyCodeLoading`。
- 后台控制台：`static/index.html` + `static/css/app.css` + `static/js/app.js`（原单文件 65 KB 内联）。

### 文档索引

| 文档 | 内容 |
|---|---|
| `docs/ARCHITECTURE.md` | 分层架构、数据流、规范与约束 |
| `docs/DEPLOYMENT.md` | 部署步骤、健康检查、故障处理 |
| `docs/MIGRATION_ROLLBACK.md` | 迁移步骤、回滚剧本、演练记录 |
| `docs/PORT_REGISTRY.md` | 端口登记表与变更流程 |
| `docs/TEST_REPORT.md` | V1–V10 验证结果与性能对比 |
| `docs/spec.md` / `docs/tasks.md` | 重构规格与任务清单 |
