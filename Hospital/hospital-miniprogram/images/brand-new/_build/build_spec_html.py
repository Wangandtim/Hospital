# -*- coding: utf-8 -*-
"""生成「健康联防」品牌视觉规范展示页 HTML（自包含，SVG 内嵌）。"""
import os, html, io

BASE = os.path.join(os.path.dirname(__file__), "..")
LOGO = os.path.join(BASE, "logo")
ICONS = os.path.join(BASE, "icons")
OUT_HTML = os.path.join(BASE, "健康联防-品牌视觉规范.html")


def read(p):
    with io.open(p, "r", encoding="utf-8") as f:
        return f.read().strip()


def embed(name, folder, cls=""):
    p = os.path.join(folder, name)
    if not os.path.exists(p):
        return f'<span class="miss">{name}</span>'
    svg = read(p)
    # 去掉 svg 外壳上固定的宽高，交给 CSS 控制尺寸；保留 viewBox
    return f'<div class="{cls}">{svg}</div>'


# 图标清单（分组展示）
ICON_GROUPS = [
    ("主导航 Tab", ["tab-home.svg", "tab-home-on.svg", "tab-map.svg", "tab-map-on.svg",
                  "tab-calendar.svg", "tab-calendar-on.svg", "tab-user.svg", "tab-user-on.svg"]),
    ("医疗业务", ["ic-hospital-14b8a6.svg", "ic-layers-14b8a6.svg", "ic-doctor-14b8a6.svg",
              "ic-cap-14b8a6.svg", "ic-calendar-14b8a6.svg", "ic-doc-14b8a6.svg",
              "ic-bookmark-14b8a6.svg", "ic-wallet-14b8a6.svg"]),
    ("通用操作", ["ic-search-14b8a6.svg", "ic-pin-14b8a6.svg", "ic-map-14b8a6.svg",
              "ic-phone-14b8a6.svg", "ic-gear-14b8a6.svg", "ic-chart-14b8a6.svg",
              "ic-grid-14b8a6.svg", "ic-key-14b8a6.svg"]),
    ("状态与反馈", ["ic-check-14b8a6.svg", "ic-close-94aeab.svg", "ic-arrow-14b8a6.svg",
               "ic-info-14b8a6.svg", "ic-help-14b8a6.svg", "ic-clock-14b8a6.svg",
               "ic-heart-14b8a6.svg", "ic-shield-14b8a6.svg"]),
    ("其他工具", ["ic-camera-14b8a6.svg", "ic-scan-14b8a6.svg", "ic-refresh-14b8a6.svg",
              "ic-target-14b8a6.svg", "ic-globe-06b6d4.svg", "ic-nav-0ea5e9.svg",
              "ic-user-14b8a6.svg", "ic-logout-ef4444.svg"]),
]

PALETTE = [
    ("Primary 主色", "#14B8A6"), ("Deep 深青", "#0D9488"), ("Ink 青墨", "#0F766E"),
    ("Accent 亮青", "#5EEAD4"), ("Soft 浅青", "#CCFBF1"), ("Mist 雾青", "#F0FDFA"),
    ("Line 描边", "#D9F2EF"), ("Success 成功", "#10B981"),
    ("Warning 警示", "#F59E0B"), ("Danger 危险", "#EF4444"),
]

# ---- 组装图标分组 HTML ----
groups_html = ""
for title, names in ICON_GROUPS:
    cells = ""
    for n in names:
        cells += f'<div class="ic-cell">{embed(n, ICONS)}<span>{n.replace(".svg","")}</span></div>'
    groups_html += f'<div class="group"><h3>{title}</h3><div class="ic-grid">{cells}</div></div>'

palette_html = ""
for name, hexv in PALETTE:
    palette_html += (f'<div class="swatch"><div class="sw-color" style="background:{hexv}"></div>'
                      f'<div class="sw-name">{name}</div><div class="sw-hex">{hexv}</div></div>')

# 白标反白样张
reverse_mark = embed("mark-reverse.svg", LOGO)
primary_mark = embed("mark-primary.svg", LOGO)
h_combo = embed("logo-horizontal.svg", LOGO)
v_combo = embed("logo-vertical.svg", LOGO)

page = f"""<!DOCTYPE html>
<html lang="zh-CN" data-canvas-width="1600" data-canvas-height="2500"
      style="width:1600px;height:2500px;">
<head>
<meta charset="UTF-8">
<style>
* {{ box-sizing:border-box; margin:0; padding:0; }}
html,body {{ width:1600px; height:2500px; }}
body {{ font-family:-apple-system,'PingFang SC','Microsoft YaHei','Segoe UI',sans-serif;
  background:#F0FDFA; color:#143634; }}
[data-canvas-root] {{ width:1600px; height:2500px; padding:56px 64px; }}

.hero {{ display:flex; justify-content:space-between; align-items:flex-end;
  border-bottom:1px solid #D9F2EF; padding-bottom:36px; }}
.hero h1 {{ font-size:44px; font-weight:800; letter-spacing:2px; color:#0F2E2E; }}
.hero p {{ margin-top:10px; font-size:18px; color:#5B7C7A; letter-spacing:1px; }}
.hero .meta {{ text-align:right; font-size:15px; color:#94AEAB; line-height:1.9; }}

section {{ margin-top:44px; }}
.sec-title {{ display:flex; align-items:center; gap:14px; font-size:26px; font-weight:700;
  color:#143634; margin-bottom:24px; }}
.sec-title::before {{ content:''; width:10px; height:26px;
  background:linear-gradient(180deg,#14B8A6,#5EEAD4);
  clip-path:polygon(0 0,100% 0,100% calc(100% - 12px),0 100%); }}

/* Logo 区 */
.logo-grid {{ display:grid; grid-template-columns:repeat(4,1fr); gap:24px; }}
.logo-card {{ background:#fff; border:1px solid #E6FFFA; border-radius:4px; padding:32px;
  clip-path:polygon(22px 0,100% 0,100% calc(100% - 22px),calc(100% - 22px) 100%,0 100%,0 22px);
  display:flex; flex-direction:column; align-items:center; gap:18px; min-height:220px; justify-content:center; }}
.logo-card.dark {{ background:linear-gradient(145deg,#14B8A6,#0F766E); }}
.logo-card .cap {{ font-size:15px; color:#5B7C7A; }}
.logo-card.dark .cap {{ color:rgba(255,255,255,.8); }}
.logo-card svg {{ width:120px; height:120px; }}
.logo-card.wide svg {{ width:260px; height:78px; }}
.logo-card.tall svg {{ width:110px; height:130px; }}

/* 色板 */
.palette {{ display:grid; grid-template-columns:repeat(5,1fr); gap:20px; }}
.swatch {{ background:#fff; border:1px solid #E6FFFA; padding:18px; }}
.sw-color {{ height:84px; margin-bottom:14px;
  clip-path:polygon(12px 0,100% 0,100% calc(100% - 12px),calc(100% - 12px) 100%,0 100%,0 12px); }}
.sw-name {{ font-size:16px; font-weight:600; }}
.sw-hex {{ font-size:14px; color:#94AEAB; margin-top:4px; font-family:Menlo,Consolas,monospace; }}

/* 图标 */
.group {{ margin-bottom:28px; }}
.group h3 {{ font-size:18px; color:#0F766E; margin-bottom:16px; font-weight:600; }}
.ic-grid {{ display:grid; grid-template-columns:repeat(8,1fr); gap:16px; }}
.ic-cell {{ background:#fff; border:1px solid #E6FFFA; padding:18px 8px; text-align:center;
  clip-path:polygon(10px 0,100% 0,100% calc(100% - 10px),calc(100% - 10px) 100%,0 100%,0 10px); }}
.ic-cell svg {{ width:44px; height:44px; margin:0 auto 10px; }}
.ic-cell span {{ font-size:12px; color:#5B7C7A; font-family:Menlo,Consolas,monospace; }}

/* 模块与按钮样张 */
.mock-row {{ display:grid; grid-template-columns:1.15fr 1fr 1fr; gap:24px; align-items:stretch; }}
.mock-card {{ background:#fff; border:1px solid #E6FFFA; padding:28px;
  clip-path:polygon(22px 0,100% 0,100% calc(100% - 22px),calc(100% - 22px) 100%,0 100%,0 22px); }}
.mock-card h4 {{ font-size:17px; margin-bottom:18px; color:#143634; }}
.btn {{ display:inline-flex; align-items:center; justify-content:center; height:56px; padding:0 28px;
  font-size:16px; font-weight:600; margin:6px 8px 6px 0;
  clip-path:polygon(16px 0,100% 0,100% calc(100% - 16px),calc(100% - 16px) 100%,0 100%,0 16px); }}
.btn-primary {{ background:linear-gradient(135deg,#14B8A6,#0D9488); color:#fff; }}
.btn-secondary {{ background:#CCFBF1; color:#0F766E; }}
.btn-ghost {{ background:transparent; color:#0F766E; border:1px solid #D9F2EF; }}
.btn-danger {{ background:#EF4444; color:#fff; }}
.btn-sm {{ height:38px; padding:0 18px; font-size:14px;
  clip-path:polygon(10px 0,100% 0,100% calc(100% - 10px),calc(100% - 10px) 100%,0 100%,0 10px); }}
.tag {{ display:inline-flex; padding:5px 14px; font-size:13px; font-weight:600; margin:4px 8px 4px 0;
  background:#CCFBF1; color:#0F766E; clip-path:polygon(6px 0,100% 0,100% 100%,0 100%,0 6px); }}
.tag-outline {{ background:#fff; border:1px solid #D9F2EF; }}
.tag-warning {{ background:#FEF3E2; color:#F59E0B; }}
.tag-danger {{ background:#FEECEC; color:#EF4444; }}

.entry-row {{ display:flex; gap:20px; }}
.entry {{ display:flex; flex-direction:column; align-items:center; gap:12px; }}
.icon-box {{ width:72px; height:72px; display:flex; align-items:center; justify-content:center;
  clip-path:polygon(10px 0,100% 0,100% calc(100% - 10px),calc(100% - 10px) 100%,0 100%,0 10px); }}
.icon-box svg {{ width:38px; height:38px; }}
.ib-primary {{ background:linear-gradient(145deg,#14B8A6,#0D9488); }}
.ib-soft {{ background:#CCFBF1; }}
.ib-outline {{ background:#fff; border:1px solid #D9F2EF; }}
.entry label {{ font-size:14px; color:#33504D; }}

.note {{ margin-top:24px; font-size:14px; color:#5B7C7A; line-height:1.8; }}
</style>
</head>
<body>
<main data-canvas-root style="width:1600px;height:2200px;">

  <div class="hero">
    <div>
      <h1>健康联防 · 品牌视觉规范</h1>
      <p>智慧医院预约系统 Logo / 图标 / 模块 / 按钮 设计系统 v2</p>
    </div>
    <div class="meta">青绿医疗体系 · 切角专业语言<br>SVG 矢量 · 48 网格 · 等线描边</div>
  </div>

  <section>
    <div class="sec-title">一、Logo 标志变体</div>
    <div class="logo-grid">
      <div class="logo-card">{primary_mark}<span class="cap">主图形标（透明底）</span></div>
      <div class="logo-card dark">{reverse_mark}<span class="cap">反白标（深底）</span></div>
      <div class="logo-card wide">{h_combo}<span class="cap">横版图文组合</span></div>
      <div class="logo-card tall">{v_combo}<span class="cap">竖版图文组合</span></div>
    </div>
  </section>

  <section>
    <div class="sec-title">二、品牌色板</div>
    <div class="palette">{palette_html}</div>
  </section>

  <section>
    <div class="sec-title">三、图标系统（SVG 矢量）</div>
    {groups_html}
  </section>

  <section>
    <div class="sec-title">四、模块与按钮样张</div>
    <div class="mock-row">
      <div class="mock-card">
        <h4>按钮体系</h4>
        <div><span class="btn btn-primary">主要按钮</span><span class="btn btn-secondary">次要按钮</span></div>
        <div><span class="btn btn-ghost">幽灵按钮</span><span class="btn btn-danger">危险操作</span></div>
        <div style="margin-top:10px;"><span class="btn btn-primary btn-sm">行内主操作</span><span class="btn btn-ghost btn-sm">行内次操作</span></div>
      </div>
      <div class="mock-card">
        <h4>标签 / 徽章</h4>
        <div>
          <span class="tag">王牌科室</span><span class="tag tag-outline">可预约</span>
          <span class="tag tag-warning">待审核</span><span class="tag tag-danger">已取消</span>
        </div>
        <h4 style="margin-top:22px;">快捷入口容器</h4>
        <div class="entry-row">
          <div class="entry"><div class="icon-box ib-primary">{embed("ic-hospital-ffffff.svg",ICONS)}</div><label>选择医院</label></div>
          <div class="entry"><div class="icon-box ib-soft">{embed("ic-map-14b8a6.svg",ICONS)}</div><label>周边医院</label></div>
          <div class="entry"><div class="icon-box ib-outline">{embed("ic-layers-14b8a6.svg",ICONS)}</div><label>全部科室</label></div>
        </div>
      </div>
      <div class="mock-card">
        <h4>卡片语言</h4>
        <p style="font-size:15px;color:#5B7C7A;line-height:1.9;">
          面板采用左上 / 右下斜切角，替代圆角方块与圆形托底；
          细描边加克制冷青阴影；图标以线性等线呈现，面性仅用于强调。
          整体留白充足，层级由字重与色阶区分，不依赖大面积色块堆砌。
        </p>
      </div>
    </div>
    <p class="note">注：本规范页为视觉评审样张；正式落地样式见同目录 <b>hl-design-system.wxss</b>，
    SVG 源文件位于 <b>logo/</b> 与 <b>icons/</b> 目录，可直接替换小程序旧 PNG 图标。</p>
  </section>

</main>
</body>
</html>"""

with io.open(OUT_HTML, "w", encoding="utf-8") as f:
    f.write(page)
print("spec html:", OUT_HTML)

