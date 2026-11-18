# 端口登记表（PORT_REGISTRY）

版本：v1.0　生效日期：2026-11-11　维护责任人：后端负责人

---

## 1. 端口分配（唯一事实来源）

| 端口 | 协议 | 归属服务 | 监听地址 | 可见范围 | 用途 | 配置位置 |
|---|---|---|---|---|---|---|
| 8080 | HTTP | hospital-api（Spring Boot） | `0.0.0.0` | 对外唯一入口 | 全部 `/api/**` + 后台控制台 `/` | `application.yml` → `server.port` / `SERVER_PORT` |
| 5000 | HTTP | hospital-crawler（Flask） | `127.0.0.1` | 仅本机 | 周边医院数据源 | `app.py` → `CRAWLER_PORT` / `CRAWLER_HOST` |
| 3306 | MySQL | MySQL 8.0 | `127.0.0.1` | 仅本机 | 业务库 `hospital_db` | `application.yml` → `DB_PORT` / `delivery/config/env.bat` |

### 已废止端口

| 端口 | 废止原因 | 处理方式 |
|---|---|---|
| 8081 | 历史误配置，小程序 6 个页面硬编码 `localhost:8081/api` | 统一为 8080；`delivery/start_8081.bat` 改为弃用垫片，转调 `start-all.bat` |
| 3308 | 历史误配置，根目录维护脚本与 `delivery/mysql-extracted/my.ini` 使用 | 脚本统一 3306；`mysql-extracted` 目录已无脚本引用，建议人工删除（约 1 GB） |

---

## 2. 端口使用方对照

| 使用方 | 访问目标 | 说明 |
|---|---|---|
| 微信小程序（全部页面） | `http://<host>:8080` | 唯一来源 `hospital-miniprogram/config/index.js` |
| 后台控制台 | 同源 `/api` | `static/js/app.js` → `API_BASE = '/api'` |
| `NearbyController` | `http://127.0.0.1:5000` | `crawler.base-url`，超时 8s |
| hospital-api | `127.0.0.1:3306` | HikariCP，库 `hospital_db` |
| `delivery/start-all.bat` | 8080 / 5000 / 3306 | 全部取自 `delivery/config/env.bat` |

---

## 3. 变更流程

1. 在本文件新增/修改行，注明日期与原因。
2. 若涉及后端：修改 `application.yml` 默认值并保留环境变量覆盖。
3. 若涉及交付脚本：仅修改 `delivery/config/env.bat`，不得在其他 `.bat` 中写死端口。
4. 若涉及小程序：修改 `config/index.js`，禁止在页面内硬编码。
5. 变更后执行 `docs/spec.md` §6 的 V6/V8 验证项。

---

## 4. 自检命令

```powershell
# 监听端口总览
netstat -ano | findstr /r /c:":8080 .*LISTENING" /c:":5000 .*LISTENING" /c:":3306 .*LISTENING"

# 全仓端口残留扫描（应仅命中文档与备份）
Get-ChildItem d:\Desktop\Hospital -Recurse -Include *.js,*.json,*.java,*.py,*.bat,*.yml,*.properties |
  Where-Object { $_.FullName -notmatch 'backup|target|node_modules' } |
  Select-String -Pattern '8081|3308' |
  Select-Object Path,LineNumber,Line
```

---

## 5. 防火墙建议

- 仅放行 8080/TCP 入站；5000、3306 不对外开放。
- 若使用交付包内置 `python-service/app.exe`（固定绑定 `0.0.0.0:5000`），请通过 Windows 防火墙入站规则限制 5000 仅本机可访问：

```powershell
New-NetFirewallRule -DisplayName "hospital-crawler-local-only" -Direction Inbound `
  -Protocol TCP -LocalPort 5000 -RemoteAddress 127.0.0.1 -Action Allow
New-NetFirewallRule -DisplayName "hospital-crawler-block" -Direction Inbound `
  -Protocol TCP -LocalPort 5000 -Action Block
```
