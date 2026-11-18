# -*- coding: utf-8 -*-
"""健康联防 · Logo 多方向候选（图形标，48 网格，透明底）。
每版一个独立母题，强调「联防 = 连接 / 协同 / 守护网络」，非圆非圆角方块托底。
"""
import os
from svg_tokens import COLORS, write, NS, CAP, JOIN

BASE = os.path.join(os.path.dirname(__file__), "..", "logo-candidates")

INK = COLORS["ink"]
PRIMARY = COLORS["primary"]
DEEP = COLORS["primary_deep"]
ACCENT = COLORS["accent"]


def svg(body, vb=48):
    return (f'<svg {NS} viewBox="0 0 {vb} {vb}" width="96" height="96" fill="none" '
            f'stroke-linecap="{CAP}" stroke-linejoin="{JOIN}">{body}</svg>')


# ---------- 方向 A：双环互联（两弧咬合 + 双侧连接节点） ----------
def candidate_a():
    # 左弧与右弧交错成链环，上下节点由连线贯通，象征互联协同
    body = (
        # 左半环（开口朝右）
        '<path d="M20 8 A16 16 0 0 0 20 40" stroke="#0F766E" stroke-width="3.4"/>'
        # 右半环（开口朝左）
        '<path d="M28 8 A16 16 0 0 1 28 40" stroke="#14B8A6" stroke-width="3.4"/>'
        # 中心医疗十字（负形共形）
        '<path d="M21.5 16.5 H26.5 V21.5 H31.5 V26.5 H26.5 V31.5 H21.5 V26.5 H16.5 V21.5 H21.5 Z" fill="#0F766E"/>'
        # 上下连接节点（联防链路）
        '<path d="M24 8 V4.6 M24 40 V43.4" stroke="#5EEAD4" stroke-width="3"/>'
        '<circle cx="24" cy="4" r="2" fill="#5EEAD4"/>'
        '<circle cx="24" cy="44" r="2" fill="#5EEAD4"/>'
    )
    return svg(body)


# ---------- 方向 B：守护之手 + 心（双手环抱心形，内嵌十字） ----------
def candidate_b():
    body = (
        # 左护手持臂
        '<path d="M9 30 C9 20 15 13 24 13" stroke="#0F766E" stroke-width="3.4"/>'
        # 右护手持臂
        '<path d="M39 30 C39 20 33 13 24 13" stroke="#14B8A6" stroke-width="3.4"/>'
        # 心形（守护）
        '<path d="M24 36 C17 30.5 13.5 26.2 13.5 21.3 C13.5 17.6 16.3 15 19.6 15 C21.7 15 23.4 16.2 24 17.8 C24.6 16.2 26.3 15 28.4 15 C31.7 15 34.5 17.6 34.5 21.3 C34.5 26.2 31 30.5 24 36 Z" '
        'stroke="#0F766E" stroke-width="3"/>'
        # 心内十字
        '<path d="M22 20.5 H26 V22.5 H28 V26.5 H26 V28.5 H22 V26.5 H20 V22.5 H22 Z" fill="#14B8A6"/>'
    )
    return svg(body)


# ---------- 方向 C：三节点联防组网（三角拓扑 + 中心十字） ----------
def candidate_c():
    body = (
        # 三条联防连线（三角守护网）
        '<path d="M24 10 L11 34 M24 10 L37 34 M11 34 H37" stroke="#5EEAD4" stroke-width="2.6"/>'
        # 三节点
        '<circle cx="24" cy="10" r="3.2" fill="#0F766E"/>'
        '<circle cx="11" cy="34" r="3.2" fill="#14B8A6"/>'
        '<circle cx="37" cy="34" r="3.2" fill="#14B8A6"/>'
        # 中心十字
        '<path d="M21.8 19.5 H26.2 V23.8 H30.5 V28.2 H26.2 V32.5 H21.8 V28.2 H17.5 V23.8 H21.8 Z" fill="#0F766E"/>'
    )
    return svg(body)


# ---------- 方向 D：十字桥拱（医疗十字 + 拱桥连接梁） ----------
def candidate_d():
    body = (
        # 拱桥拱券（连接两岸）
        '<path d="M8 33 C14 20 34 20 40 33" stroke="#14B8A6" stroke-width="3.4"/>'
        # 桥基立柱
        '<path d="M10 33 V39 M38 33 V39" stroke="#14B8A6" stroke-width="3.4"/>'
        # 桥上医疗十字（立于拱顶）
        '<path d="M22 10 H26 V15 H31 V19 H26 V24 H22 V19 H17 V15 H22 Z" fill="#0F766E"/>'
        # 桥面上的连接节点
        '<circle cx="16" cy="30" r="1.8" fill="#0F766E"/>'
        '<circle cx="32" cy="30" r="1.8" fill="#0F766E"/>'
    )
    return svg(body)


# ---------- 方向 E：相依人形（两人相护，负形成十字） ----------
def candidate_e():
    body = (
        # 左人形（头 + 躯干臂）
        '<circle cx="17" cy="15" r="3.4" fill="#0F766E"/>'
        '<path d="M17 20 L17 34 M17 25 L12 31" stroke="#0F766E" stroke-width="3.4"/>'
        # 右人形（头 + 躯干臂），两人手臂在中部相握
        '<circle cx="31" cy="15" r="3.4" fill="#14B8A6"/>'
        '<path d="M31 20 L31 34 M31 25 L36 31" stroke="#14B8A6" stroke-width="3.4"/>'
        # 中部相握连接（联防纽带）
        '<path d="M17 25 H31" stroke="#5EEAD4" stroke-width="3"/>'
        # 两人躯干之间负形留出竖十字通道
        '<path d="M22.6 22 H25.4 V27 H22.6 Z" fill="#0F766E" opacity="0"/>'
    )
    return svg(body)


# ---------- 方向 F：脉冲纽带（心电波形交织成盾形守护轮廓） ----------
def candidate_f():
    body = (
        # 外轮廓：折线盾形（切角，非圆）
        '<path d="M24 5 L39 12 V26 C39 34 32 39 24 43 C16 39 9 34 9 26 V12 Z" '
        'stroke="#0F766E" stroke-width="3"/>'
        # 内部心电脉冲波形（贯穿左右，联防联动）
        '<path d="M12 25 H18 L21 18 L25 32 L28 22 H36" stroke="#14B8A6" stroke-width="3.2"/>'
        # 波形两端节点
        '<circle cx="12" cy="25" r="1.8" fill="#14B8A6"/>'
        '<circle cx="36" cy="22" r="1.8" fill="#14B8A6"/>'
    )
    return svg(body)


def build():
    os.makedirs(BASE, exist_ok=True)
    cands = {
        "A-双环互联": candidate_a(),
        "B-守护手心": candidate_b(),
        "C-三节点组网": candidate_c(),
        "D-十字桥拱": candidate_d(),
        "E-相依人形": candidate_e(),
        "F-脉冲纽带": candidate_f(),
    }
    for name, svg_text in cands.items():
        write(os.path.join(BASE, f"logo-{name}.svg"), svg_text)
    print("candidates:", len(cands))


if __name__ == "__main__":
    build()
