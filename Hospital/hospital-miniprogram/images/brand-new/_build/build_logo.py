# -*- coding: utf-8 -*-
"""健康联防 Logo 母版与变体。
母题：切角盾形轮廓（非圆、非圆角方块）内嵌医疗十字，十字三向引出联防连接节点，
表达「院—医—患」协同守护。线条统一圆角端点，斜线切角形成辨识度。
"""
import os
from svg_tokens import COLORS, write, NS, CAP, JOIN

BASE = os.path.join(os.path.dirname(__file__), "..", "logo")

# 48 网格切角盾形外轮廓（上左右斜切角，底部尖收）
SHIELD = "M24 4.5 L38 10.5 L38 25 L24 41.5 L10 25 L10 10.5 Z"

# 医疗十字（共形：横臂左端与联防线衔接）
CROSS = "M21 13.5 H27 V19.5 H33 V25.5 H27 V31.5 H21 V25.5 H15 V19.5 H21 Z"

# 联防连接：左—中—右 三个节点与连线（守护网络）
LINKS = (
    # 上连线：十字顶端节点
    '<path d="M24 13.5 V9.2" />'
    # 左侧连线到左节点
    '<path d="M15 22.5 H10.6" />'
    # 右侧连线到右节点
    '<path d="M33 22.5 H37.4" />'
)
NODES = (
    '<circle cx="24" cy="7.4" r="1.9" />'
    '<circle cx="8.6" cy="22.5" r="1.9" />'
    '<circle cx="39.4" cy="22.5" r="1.9" />'
)


def mark(color="#0F766E", fill_shield=None, node_color=None):
    """独立图形标。color=描边与十字色；fill_shield=盾形填充（反白/实底时用）。"""
    node_color = node_color or color
    parts = [f'<svg {NS} viewBox="0 0 48 48" width="96" height="96" fill="none">']
    # 盾形外轮廓
    if fill_shield:
        parts.append(f'<path d="{SHIELD}" fill="{fill_shield}" stroke="{color}" stroke-width="2.6" stroke-linejoin="{JOIN}"/>')
    else:
        parts.append(f'<path d="{SHIELD}" stroke="{color}" stroke-width="2.6" stroke-linejoin="{JOIN}"/>')
    # 联防连线
    parts.append(f'<g stroke="{node_color}" stroke-width="2.4" stroke-linecap="{CAP}">{LINKS}</g>')
    parts.append(f'<g fill="{node_color}">{NODES}</g>')
    # 十字
    parts.append(f'<path d="{CROSS}" fill="{color}"/>')
    parts.append("</svg>")
    return "".join(parts)


def _inner(mark_svg):
    """去掉独立 svg 外壳，取内部节点，便于在组合版 <g> 内复用。"""
    body = mark_svg.split(">", 1)[1]
    return body.rsplit("</svg>", 1)[0]


def horizontal_combo(mark_svg, brand="健康联防", sub="智慧医院预约系统",
                     color="#0F766E", on_dark=False):
    text_fill = "#FFFFFF" if on_dark else color
    sub_fill = "rgba(255,255,255,0.72)" if on_dark else "#5B7C7A"
    return f'''<svg {NS} viewBox="0 0 360 96" width="360" height="96" fill="none">
  <g transform="translate(12,16) scale(1.42)">{_inner(mark_svg)}</g>
  <text x="104" y="46" font-family="-apple-system,'PingFang SC','Microsoft YaHei',sans-serif"
        font-size="30" font-weight="700" letter-spacing="4" fill="{text_fill}">{brand}</text>
  <text x="105" y="72" font-family="-apple-system,'PingFang SC','Microsoft YaHei',sans-serif"
        font-size="15" letter-spacing="1.5" fill="{sub_fill}">{sub}</text>
</svg>'''


def vertical_combo(mark_svg, brand="健康联防", sub="智慧医院预约系统",
                   color="#0F766E", on_dark=False):
    text_fill = "#FFFFFF" if on_dark else color
    sub_fill = "rgba(255,255,255,0.72)" if on_dark else "#5B7C7A"
    return f'''<svg {NS} viewBox="0 0 160 190" width="160" height="190" fill="none">
  <g transform="translate(56,8) scale(1.833)">{_inner(mark_svg)}</g>
  <text x="80" y="128" text-anchor="middle" font-family="-apple-system,'PingFang SC','Microsoft YaHei',sans-serif"
        font-size="26" font-weight="700" letter-spacing="4" fill="{text_fill}">{brand}</text>
  <text x="80" y="152" text-anchor="middle" font-family="-apple-system,'PingFang SC','Microsoft YaHei',sans-serif"
        font-size="13" letter-spacing="0.5" fill="{sub_fill}">{sub}</text>
</svg>'''


def build():
    os.makedirs(BASE, exist_ok=True)
    # 1. 主图形标（青绿描边 + 青墨十字，透明底）
    write(os.path.join(BASE, "mark-primary.svg"), mark(COLORS["ink"]))
    # 2. 实底反白标（用于深色/导航头图）
    write(os.path.join(BASE, "mark-reverse.svg"),
          mark(color="#FFFFFF", fill_shield=COLORS["primary_deep"], node_color="#FFFFFF"))
    # 3. 单色黑标（印刷/灰度）
    write(os.path.join(BASE, "mark-mono.svg"), mark(color="#0F2E2E"))
    # 4. 横版组合（浅底）
    write(os.path.join(BASE, "logo-horizontal.svg"),
          horizontal_combo(mark(COLORS["ink"]), color=COLORS["ink"]))
    # 5. 横版组合（深底反白）
    write(os.path.join(BASE, "logo-horizontal-reverse.svg"),
          horizontal_combo(mark("#FFFFFF", fill_shield="none", node_color="#FFFFFF"),
                            on_dark=True, color="#FFFFFF"))
    # 6. 竖版组合（浅底）
    write(os.path.join(BASE, "logo-vertical.svg"),
          vertical_combo(mark(COLORS["ink"]), color=COLORS["ink"]))
    # 7. 竖版组合（深底反白）
    write(os.path.join(BASE, "logo-vertical-reverse.svg"),
          vertical_combo(mark("#FFFFFF", fill_shield="none", node_color="#FFFFFF"),
                         on_dark=True, color="#FFFFFF"))
    print("logo done:", len(os.listdir(BASE)), "files")


if __name__ == "__main__":
    build()
