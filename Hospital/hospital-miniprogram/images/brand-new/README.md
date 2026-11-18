# 健康联防 · 品牌视觉资产交付说明

> 适用工程：`hospital-miniprogram`（智慧医院预约系统）
> 品牌主名：**健康联防** ｜ 副标题/全称：**智慧医院预约系统** ｜ 导航短名：健康联防·智慧预约
> 版本：v2 切角专业体系 ｜ 主色沿用现有青绿 ｜ 全部为 SVG 矢量

## 一、目录结构

```
images/brand-new/
├── 健康联防-品牌视觉规范.html   # 评审样张：Logo 变体 / 色板 / 图标网格 / 模块与按钮
├── hl-design-system.wxss        # 可直接引入小程序的设计 Token 与组件类
├── logo/                        # Logo 母版与变体（7 个 SVG）
├── icons/                       # 全套功能图标与 Tab 图标（58 个 SVG）
└── _build/                      # 生成脚本（可重建，非运行时资产）
```

## 二、Logo 变体（logo/）

| 文件 | 用途 |
|---|---|
| `mark-primary.svg` | 主图形标，透明底，青绿描边 + 青墨十字 |
| `mark-reverse.svg` | 反白标，深青底白形，用于导航头图 / 深色背景 |
| `mark-mono.svg` | 单色黑标，灰度 / 印刷场景 |
| `logo-horizontal.svg` | 横版图文组合（浅底） |
| `logo-horizontal-reverse.svg` | 横版反白组合（深底） |
| `logo-vertical.svg` | 竖版图文组合（浅底） |
| `logo-vertical-reverse.svg` | 竖版反白组合（深底） |

**母题**：切角盾形轮廓内嵌医疗十字，十字三向引出联防连接节点，表达「院—医—患」协同守护；
不使用圆形或圆角方块作背景装饰，识别来自轮廓切角与十字共形。

## 三、图标命名与替换（icons/）

- 命名沿用旧 PNG 文件名（仅扩展名 `.png → .svg`），页面引用路径可平滑替换。
- 网格 48、线宽 3、round 线帽与转角；线性为主，面性仅用于强调。
- 颜色后缀与旧版一一对应：

| 后缀 | 色值 | 用途 |
|---|---|---|
| `-14b8a6` | `#0F766E` 青墨 | 浅底线性主色 |
| `-ffffff` | `#FFFFFF` | 深底 / 渐变块面上的反白图标 |
| `-f59e0b` | `#F59E0B` | 警示 |
| `-ef4444` | `#EF4444` | 危险 / 退出 |
| `-0ea5e9` | `#0EA5E9` | 辅助蓝（导航） |
| `-06b6d4` | `#06B6D4` | 信息青蓝（地球） |
| `-94aeab` | `#94AEAB` | 中性灰（关闭） |

### Tab 双态
`tab-home / tab-home-on`、`tab-map / tab-map-on`、`tab-calendar / tab-calendar-on`、`tab-user / tab-user-on`。

## 四、落地方式（建议）

1. **图标替换**：小程序 `image` 组件支持 SVG 时，将 `images/icons/` 下旧 PNG 引用改为
   `images/brand-new/icons/xxx.svg`；若基础库对 SVG 支持受限，可按 96×96 导出同名 PNG 放回旧目录。
2. **样式接入**：在 `app.wxss` 顶部 `@import "images/brand-new/hl-design-system.wxss";`，
   按其中 `.hl-card / .hl-btn / .hl-tag / .hl-icon-box / .hl-menu-item` 等类逐步替换旧模块。
3. **Token 对齐**：新 WXSS 内 `--hl-*` 变量与现有 `--color-*` 色值一致，可并行过渡，不强制一次性改完。

## 五、设计语言要点

- **形状**：面板与按钮采用左上 / 右下斜切角（`clip-path`），替代大圆角与圆形托底。
- **层级**：靠字重、色阶与留白区分，不依赖大面积色块堆砌；阴影克制冷青。
- **图标**：统一等线描边，端点 / 转角一致；小尺寸下保持轮廓可辨。

## 六、未包含 / 待确认

- 未改动任何页面业务代码与旧 PNG 文件；本次仅新增品牌资产与规范样张。
- 字体使用系统中文黑体栈（PingFang SC / 微软雅黑），未嵌入商业字体。
- SVG 为自绘矢量，未做商标注册检索；正式商用前建议另行做商标排查。
