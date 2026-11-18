# -*- coding: utf-8 -*-
"""
健康联防 · 全套 SVG 图标集
网格 48 / 线宽 3 / round 线帽与转角；线性为主，面性仅用于强调。
命名沿用小程序现有 ic-* 文件名，便于直接替换；颜色后缀与旧 PNG 对应。
"""
import os
from svg_tokens import COLORS, write, NS, CAP, JOIN, STROKE, STROKE_THIN

OUT = os.path.join(os.path.dirname(__file__), "..", "icons")

# 颜色别名（对齐旧文件后缀）
C = {
    "teal":  COLORS["ink"],       # 0F766E 主青绿线性
    "white": COLORS["white"],
    "orange": COLORS["warning"],
    "red":   COLORS["danger"],
    "blue":  COLORS["nav_blue"],
    "cyan":  COLORS["info_blue"],
    "gray":  COLORS["gray_light"],
}

SW = STROKE


def _wrap(body, color, sw=SW, fill="none"):
    return (
        f'<svg {NS} viewBox="0 0 48 48" width="96" height="96" fill="{fill}" '
        f'stroke="{color}" stroke-width="{sw}" stroke-linecap="{CAP}" stroke-linejoin="{JOIN}">'
        f'{body}</svg>'
    )


# ---------- 各图标路径（48 网格，描边色由变量 {c} 占位） ----------
# 每个函数返回 (body, fill_mode)；fill_mode: none=线性, fill=面性
ICONS = {}


def reg(name, body, fill="none", sw=SW):
    ICONS[name] = (body, fill, sw)


# 首页：屋顶 + 门
reg("home",
    '<path d="M7 23 L24 9 L41 23" />'
    '<path d="M12 21.5 V38 H36 V21.5" />'
    '<path d="M21 38 V29 H27 V38" />')

# 周边：地图折页 + 定位钉
reg("map",
    '<path d="M8 14 L18 10 L30 14 L40 10 V34 L30 38 L18 34 L8 38 Z" />'
    '<path d="M18 10 V34 M30 14 V38" />')

# 定位钉（地图针）
reg("pin",
    '<path d="M24 41 C24 41 12 29.5 12 19.5 C12 13.2 17.4 9 24 9 C30.6 9 36 13.2 36 19.5 C36 29.5 24 41 24 41 Z" />'
    '<circle cx="24" cy="19.5" r="4" />')

# 日历
reg("calendar",
    '<rect x="9" y="11" width="30" height="28" rx="4" />'
    '<path d="M9 19 H39 M16 7 V13 M32 7 V13" />'
    '<path d="M16 26 H20 M24 26 H28 M16 31 H20" />')

# 用户
reg("user",
    '<circle cx="24" cy="17" r="7" />'
    '<path d="M10 39 C10 30.5 16.5 26 24 26 C31.5 26 38 30.5 38 39" />')

# 医院建筑
reg("hospital",
    '<path d="M8 40 V18 L20 11 L32 18 V40" />'
    '<path d="M32 22 H40 V40" />'
    '<path d="M13 40 V31 H19 V40 M23 40 V31 H29 V40" />'
    '<path d="M20 17.5 H24 M22 15.5 V19.5" />')

# 科室：层叠方块
reg("layers",
    '<path d="M24 8 L40 16 L24 24 L8 16 Z" />'
    '<path d="M11 21.5 L24 28.5 L37 21.5" />'
    '<path d="M11 27.5 L24 34.5 L37 27.5" />')

# 医生：头像 + 领结/听诊
reg("doctor",
    '<circle cx="24" cy="16" r="6.5" />'
    '<path d="M11 39 C11 30.5 17 26 24 26 C31 26 37 30.5 37 39" />'
    '<path d="M19 30 L24 34 L29 30" />')

# 医生帽（学位/职称）
reg("cap",
    '<path d="M6 19 L24 11 L42 19 L24 27 Z" />'
    '<path d="M15 23 V30 C15 33 19.5 35 24 35 C28.5 35 33 33 33 30 V23" />'
    '<path d="M36 21 V29" />')

# 文档/病历
reg("doc",
    '<path d="M13 7 H29 L36 14 V41 H13 Z" />'
    '<path d="M29 7 V14 H36" />'
    '<path d="M18 22 H31 M18 27 H31 M18 32 H26" />')

# 档案夹
reg("bookmark",
    '<path d="M14 7 H30 V39 L22 33 L14 39 Z" />')

# 搜索
reg("search",
    '<circle cx="21" cy="21" r="10" />'
    '<path d="M29 29 L38 38" />')

# 齿轮设置
reg("gear",
    '<circle cx="24" cy="24" r="5.5" />'
    '<path d="M24 8.5 L25.8 12.5 L29.5 11.8 L31 15.3 L34.5 16.8 L33.8 20.5 L37 22.5 V25.5 L33.8 27.5 L34.5 31.2 L31 32.7 L29.5 36.2 L25.8 35.5 L24 39.5 L22.2 35.5 L18.5 36.2 L17 32.7 L13.5 31.2 L14.2 27.5 L11 25.5 V22.5 L14.2 20.5 L13.5 16.8 L17 15.3 L18.5 11.8 L22.2 12.5 Z" />')

# 图表统计
reg("chart",
    '<path d="M9 39 H39" />'
    '<path d="M14 39 V27 M24 39 V17 M34 39 V23" />')

# 网格
reg("grid",
    '<rect x="10" y="10" width="12" height="12" rx="2.5" />'
    '<rect x="26" y="10" width="12" height="12" rx="2.5" />'
    '<rect x="10" y="26" width="12" height="12" rx="2.5" />'
    '<rect x="26" y="26" width="12" height="12" rx="2.5" />')

# 电话
reg("phone",
    '<path d="M14 8 H20 L22 16 L18.5 18.5 C20.5 22.5 23.5 25.5 27.5 27.5 L30 24 L38 26 V32 C38 33.7 36.7 35 35 35 C20.5 33.5 12.5 25.5 11 11 C10.7 9.3 12.3 8 14 8 Z" />')

# 钱包
reg("wallet",
    '<rect x="8" y="13" width="32" height="22" rx="4" />'
    '<path d="M8 18 H32" />'
    '<circle cx="31" cy="26" r="2.2" />')

# 钥匙
reg("key",
    '<circle cx="16" cy="18" r="6" />'
    '<path d="M20.5 22.5 L34 36" />'
    '<path d="M30 32 L33 29 M33.5 35.5 L36.5 32.5" />')

# 相机
reg("camera",
    '<path d="M9 16 H15 L18 11 H30 L33 16 H39 V36 H9 Z" />'
    '<circle cx="24" cy="26" r="6" />')

# 扫码
reg("scan",
    '<path d="M10 18 V11 H17 M31 11 H38 V18 M38 30 V37 H31 M17 37 H10 V30" />'
    '<path d="M10 24 H38" />')

# 刷新
reg("refresh",
    '<path d="M38 20 A15 15 0 1 0 36 30" />'
    '<path d="M38 12 V20 H30" />')

# 时钟
reg("clock",
    '<circle cx="24" cy="24" r="15" />'
    '<path d="M24 16 V24 L30 28" />')

# 心形
reg("heart",
    '<path d="M24 39 C14 32 9 26.5 9 19.5 C9 14.8 12.7 11.5 16.8 11.5 C19.6 11.5 22.1 13.2 24 15.6 C25.9 13.2 28.4 11.5 31.2 11.5 C35.3 11.5 39 14.8 39 19.5 C39 26.5 34 32 24 39 Z" />')

# 盾牌安全
reg("shield",
    '<path d="M24 6 L37 11.5 V23 C37 31.5 31 37.5 24 41 C17 37.5 11 31.5 11 23 V11.5 Z" />'
    '<path d="M17.5 23.5 L22 28 L31 18.5" />')

# 目标
reg("target",
    '<circle cx="24" cy="24" r="14" />'
    '<circle cx="24" cy="24" r="7" />'
    '<circle cx="24" cy="24" r="1.6" fill="{c}" stroke="none" />')

# 信息
reg("info",
    '<circle cx="24" cy="24" r="15" />'
    '<path d="M24 22 V31" />'
    '<circle cx="24" cy="16.5" r="1.6" fill="{c}" stroke="none" />')

# 帮助问号
reg("help",
    '<circle cx="24" cy="24" r="15" />'
    '<path d="M19.5 19.5 C19.5 16.8 21.5 15 24 15 C26.5 15 28.5 16.8 28.5 19.2 C28.5 21.8 26 22.6 24 24.5" />'
    '<circle cx="24" cy="31" r="1.6" fill="{c}" stroke="none" />')

# 对勾
reg("check",
    '<path d="M13 24.5 L20.5 32 L35 17" />', sw=3.4)

# 关闭
reg("close",
    '<path d="M16 16 L32 32 M32 16 L16 32" />', sw=3.2)

# 右箭头
reg("arrow",
    '<path d="M16 12 L28 24 L16 36" />', sw=3.2)

# 导航箭头
reg("nav",
    '<path d="M24 8 L32 34 L24 29 L16 34 Z" />')

# 地球
reg("globe",
    '<circle cx="24" cy="24" r="15" />'
    '<path d="M9 24 H39 M24 9 C18 15 18 33 24 39 M24 9 C30 15 30 33 24 39" />')

# 退出
reg("logout",
    '<path d="M19 12 H12 V36 H19" />'
    '<path d="M28 16 L35 24 L28 32" />'
    '<path d="M35 24 H18" />')


def build():
    os.makedirs(OUT, exist_ok=True)
    # 颜色后缀 → 颜色值；与旧 PNG 一一对应
    variants = {
        "14b8a6":  C["teal"],
        "ffffff":  C["white"],
        "f59e0b":  C["orange"],
        "ef4444":  C["red"],
        "0ea5e9":  C["blue"],
        "06b6d4":  C["cyan"],
        "94aeab":  C["gray"],
    }
    # 每个图标需要输出的颜色变体（对齐现有引用）
    variant_map = {
        "arrow":   ["14b8a6", "ffffff"],
        "bookmark":["14b8a6"],
        "calendar":["14b8a6", "ffffff"],
        "camera":  ["14b8a6"],
        "cap":     ["14b8a6"],
        "chart":   ["14b8a6", "ffffff"],
        "check":   ["14b8a6", "ffffff"],
        "clock":   ["14b8a6"],
        "close":   ["94aeab", "ffffff"],
        "doc":     ["14b8a6", "ffffff"],
        "doctor":  ["14b8a6", "ffffff"],
        "gear":    ["14b8a6", "ffffff"],
        "globe":   ["06b6d4", "ffffff"],
        "grid":    ["14b8a6"],
        "heart":   ["14b8a6"],
        "help":    ["14b8a6"],
        "hospital":["14b8a6", "ffffff"],
        "info":    ["14b8a6", "ffffff", "f59e0b"],
        "key":     ["14b8a6"],
        "layers":  ["14b8a6", "ffffff"],
        "logout":  ["ef4444"],
        "map":     ["14b8a6", "ffffff"],
        "nav":     ["0ea5e9", "ffffff"],
        "phone":   ["14b8a6"],
        "pin":     ["14b8a6", "ffffff"],
        "refresh": ["14b8a6"],
        "scan":    ["14b8a6"],
        "search":  ["14b8a6", "ffffff"],
        "shield":  ["14b8a6"],
        "target":  ["14b8a6"],
        "user":    ["14b8a6", "ffffff"],
        "wallet":  ["14b8a6"],
    }

    count = 0
    for icon, suffixes in variant_map.items():
        body, fill, sw = ICONS[icon]
        for suf in suffixes:
            color = variants[suf]
            rendered = body.replace("{c}", color)
            svg = _wrap(rendered, color, sw=sw, fill=fill)
            write(os.path.join(OUT, f"ic-{icon}-{suf}.svg"), svg)
            count += 1

    # Tab 双态（线性：未选中灰青，选中深青）
    tabs = {"home": "home", "map": "map", "calendar": "calendar", "user": "user"}
    for tab, icon in tabs.items():
        body, fill, sw = ICONS[icon]
        # 未选中
        write(os.path.join(OUT, f"tab-{tab}.svg"),
              _wrap(body.replace("{c}", COLORS["gray_light"]), COLORS["gray_light"], sw=sw))
        # 选中
        write(os.path.join(OUT, f"tab-{tab}-on.svg"),
              _wrap(body.replace("{c}", COLORS["ink"]), COLORS["ink"], sw=sw, fill=fill))
        count += 2

    print("icons done:", count, "files")


if __name__ == "__main__":
    build()
