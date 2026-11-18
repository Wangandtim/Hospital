# 医院搜索 API 文档

## 概述

医院搜索系统提供智能搜索、分类搜索、搜索建议等功能，支持搜索科室、医生、疾病等。

## 基础信息

- Base URL: `http://localhost:8080/api/search`
- 响应格式: JSON
- 跨域支持: 是 (CORS Enabled)

## API 接口列表

### 1. 综合搜索
- **URL**: `GET /api/search?keyword={keyword}`
- **描述**: 搜索科室和医生，返回综合结果
- **参数**:
  - `keyword` (必填): 搜索关键词
- **响应**:
```json
{
  "success": true,
  "departments": [...],
  "doctors": [...],
  "totalCount": 5
}
```

---

### 2. 智能搜索（推荐）
- **URL**: `GET /api/search/smart?keyword={keyword}`
- **描述**: 智能搜索，带评分排序，最相关的结果排在前面
- **参数**:
  - `keyword` (必填): 搜索关键词
- **评分规则**:
  - 完全匹配: 100分
  - 开头匹配: 80分
  - 包含匹配: 60分
  - 职称/描述匹配: 30-50分
  - 主任医师额外加分: 15分
  - 副主任医师额外加分: 10分
- **响应**:
```json
{
  "success": true,
  "keyword": "内科",
  "results": [
    {
      "type": "department",
      "department": {...},
      "score": 100
    },
    {
      "type": "doctor",
      "doctor": {...},
      "score": 85
    }
  ],
  "totalCount": 4
}
```

---

### 3. 分类搜索
- **URL**: `GET /api/search/type/{type}?keyword={keyword}`
- **描述**: 按类型搜索（科室/医生/专长）
- **参数**:
  - `type` (路径参数, 必填): 搜索类型
    - `department`: 搜索科室
    - `doctor`: 搜索医生
    - `expertise`: 按专长搜索医生
  - `keyword` (必填): 搜索关键词
- **响应**:
```json
{
  "success": true,
  "type": "doctor",
  "data": [...]
}
```

---

### 4. 搜索建议
- **URL**: `GET /api/search/suggestions?keyword={keyword}`
- **描述**: 获取搜索建议，用于输入框实时提示
- **参数**:
  - `keyword` (必填): 搜索关键词
- **响应**:
```json
{
  "success": true,
  "keyword": "内",
  "suggestions": [
    {
      "name": "内科",
      "type": "department",
      "id": "DEPT001"
    },
    {
      "name": "张建国",
      "title": "主任医师",
      "type": "doctor",
      "id": "DOC001"
    }
  ]
}
```

---

### 5. 热门搜索
- **URL**: `GET /api/search/hot`
- **描述**: 获取热门搜索关键词
- **响应**:
```json
{
  "success": true,
  "hotSearches": [
    {
      "keyword": "内科",
      "category": "最热门科室"
    },
    {
      "keyword": "张建国",
      "category": "知名医生"
    }
  ]
}
```

---

### 6. 科室搜索
- **URL**: `GET /api/search/departments?keyword={keyword}`
- **描述**: 仅搜索科室
- **响应**:
```json
{
  "success": true,
  "data": [...],
  "total": 3
}
```

---

### 7. 医生搜索
- **URL**: `GET /api/search/doctors?keyword={keyword}`
- **描述**: 仅搜索医生
- **响应**:
```json
{
  "success": true,
  "data": [...],
  "total": 2
}
```

---

### 8. 按专长搜索医生
- **URL**: `GET /api/search/doctors/expertise?keyword={keyword}`
- **描述**: 按医疗专长搜索医生
- **响应**:
```json
{
  "success": true,
  "data": [...],
  "total": 2
}
```

---

## 数据模型

### Department (科室)
```json
{
  "deptId": "DEPT001",
  "deptName": "内科",
  "description": "主要诊治呼吸系统、消化系统、心血管系统等内科疾病",
  "serviceTime": "周一至周日 08:00-17:00"
}
```

### Doctor (医生)
```json
{
  "doctorId": "DOC001",
  "name": "张建国",
  "title": "主任医师",
  "deptId": "DEPT001",
  "expertise": "心血管疾病、高血压、冠心病",
  "phone": "13800138001",
  "status": 1,
  "consultationFee": 50
}
```

---

## 使用示例

### 示例 1: 智能搜索"张建国"
```bash
curl "http://localhost:8080/api/search/smart?keyword=张建国"
```

**响应**:
```json
{
  "success": true,
  "keyword": "张建国",
  "results": [
    {
      "type": "doctor",
      "doctor": {
        "doctorId": "DOC001",
        "name": "张建国",
        "title": "主任医师",
        "expertise": "心血管疾病、高血压、冠心病",
        "consultationFee": 50
      },
      "score": 115
    }
  ],
  "totalCount": 1
}
```

---

### 示例 2: 获取搜索建议"心"
```bash
curl "http://localhost:8080/api/search/suggestions?keyword=心"
```

**响应**:
```json
{
  "success": true,
  "keyword": "心",
  "suggestions": [
    {
      "name": "内科",
      "type": "department",
      "id": "DEPT001"
    },
    {
      "name": "张建国",
      "title": "主任医师",
      "type": "doctor",
      "id": "DOC001"
    }
  ]
}
```

---

### 示例 3: 按专长搜索"心血管"
```bash
curl "http://localhost:8080/api/search/type/expertise?keyword=心血管"
```

---

## 搜索场景建议

### 场景 1: 首页搜索框
- 使用 `/api/search/smart` - 智能搜索，带评分排序
- 实时提示使用 `/api/search/suggestions`

### 场景 2: 科室页面搜索
- 使用 `/api/search/departments`
- 或 `/api/search/type/department`

### 场景 3: 医生页面搜索
- 使用 `/api/search/doctors`
- 或 `/api/search/type/doctor`

### 场景 4: 按疾病找医生
- 使用 `/api/search/type/expertise`
- 或 `/api/search/doctors/expertise`

---

## 错误响应

所有接口统一错误格式:
```json
{
  "success": false,
  "message": "错误描述"
}
```

---

## 快速测试

打开浏览器访问以下地址测试:

1. 智能搜索内科:
   `http://localhost:8080/api/search/smart?keyword=内科`

2. 搜索建议:
   `http://localhost:8080/api/search/suggestions?keyword=张`

3. 热门搜索:
   `http://localhost:8080/api/search/hot`

4. 按专长搜索:
   `http://localhost:8080/api/search/doctors/expertise?keyword=心血管`

---

## 注意事项

1. 关键词不区分大小写
2. 支持模糊匹配（包含匹配）
3. 搜索结果按相关度排序
4. 建议使用智能搜索接口以获得最佳体验
5. 搜索建议接口适合在输入框实时调用
