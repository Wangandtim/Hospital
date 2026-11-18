# 医院信息爬虫服务

这是一个用于获取医院急诊电话和预约链接的 Python 爬虫服务，配合医院预约系统微信小程序使用。

## 功能特性

- 百度搜索医院相关信息
- 智能提取急诊电话号码
- 自动查找预约挂号链接
- 判断医院是否24小时营业
- 识别医院类型（综合/专科/中医）

## 安装依赖

```bash
pip install -r requirements.txt
```

## 启动服务

```bash
python app.py
```

服务将在 `http://localhost:5000` 启动

## API 接口

### 1. 获取医院信息

**接口地址**: `GET /api/hospital/info`

**请求参数**:
- `name`: 医院名称（必填）
- `address`: 医院地址（可选）

**请求示例**:
```
GET /api/hospital/info?name=北京协和医院&address=北京市东城区
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "emergency_phone": "010-69156114",
    "appointment_link": "https://www.pumch.cn/appointment",
    "is_24h": true,
    "type": "综合医院"
  }
}
```

### 2. 健康检查

**接口地址**: `GET /api/health`

**响应示例**:
```json
{
  "status": "ok",
  "message": "Python爬虫服务运行正常"
}
```

## 技术说明

- 使用 Flask 框架提供 REST API
- 使用 Requests 库发送 HTTP 请求
- 使用 BeautifulSoup 解析 HTML 页面
- 使用正则表达式提取电话号码
- 包含请求延迟避免被反爬虫

## 注意事项

1. 请遵守目标网站的 robots.txt 规则
2. 建议合理设置请求间隔，避免对目标网站造成压力
3. 本服务仅供学习和研究使用
4. 实际项目中建议添加缓存机制
