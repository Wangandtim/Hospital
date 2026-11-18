# -*- coding: utf-8 -*-
"""
健康联防 · 设计 Token 与 SVG 基础工具
统一视觉语言：48 网格 / 等宽描边 / 斜切角 / 圆角线帽；不使用圆形或圆角方块作背景装饰。
色板沿用小程序现有青绿体系（app.wxss 设计 Token）。
"""

# ---------- 品牌色板 ----------
COLORS = {
    "primary":   "#14B8A6",  # 主青绿  填充/高亮
    "primary_deep": "#0D9488",  # 深青  导航/强调
    "ink":       "#0F766E",  # 青墨  文字强调/线性主色
    "soft":      "#CCFBF1",  # 浅青  软底
    "mist":      "#F0FDFA",  # 雾青  页面底
    "accent":    "#5EEAD4",  # 亮青  点缀
    "success":   "#10B981",
    "warning":   "#F59E0B",
    "danger":    "#EF4444",
    "info_blue": "#06B6D4",
    "nav_blue":  "#0EA5E9",
    "ink_dark":  "#0F2E2E",
    "gray":      "#5B7C7A",
    "gray_light":"#94AEAB",
    "white":     "#FFFFFF",
}

# 线宽（48 视口）
STROKE = 3.0
STROKE_THIN = 2.4
CAP = "round"
JOIN = "round"

NS = 'xmlns="http://www.w3.org/2000/svg"'


def svg_open(vb=48, extra=""):
    return (
        f'<svg {NS} viewBox="0 0 {vb} {vb}" '
        f'width="96" height="96" fill="none" '
        f'stroke-linecap="{CAP}" stroke-linejoin="{JOIN}"{extra}>'
    )


def wrap(body, vb=48):
    return f'{svg_open(vb)}' + body + "</svg>"


def write(path, content):
    import io, os
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with io.open(path, "w", encoding="utf-8") as f:
        f.write(content)
    return path
