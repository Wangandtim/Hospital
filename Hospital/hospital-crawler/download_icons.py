#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
下载微信小程序 TabBar 图标脚本
使用简单的纯色图标，适合开发阶段使用
"""

import os
from PIL import Image, ImageDraw, ImageFont

def create_icon(text, color, size=81, bg_color="#ffffff"):
    """
    创建简单的图标
    :param text: 图标文字
    :param color: 文字颜色
    :param size: 图标尺寸
    :param bg_color: 背景颜色
    :return: PIL Image 对象
    """
    # 创建白色背景
    img = Image.new('RGBA', (size, size), bg_color)
    draw = ImageDraw.Draw(img)
    
    # 尝试使用系统字体，如果失败则使用简单的圆形
    try:
        # 尝试使用不同的字体
        font_sizes = [40, 36, 32, 28]
        font = None
        
        for font_size in font_sizes:
            try:
                # Windows 系统字体
                font = ImageFont.truetype("msyh.ttc", font_size)
                break
            except:
                try:
                    font = ImageFont.truetype("arial.ttf", font_size)
                    break
                except:
                    continue
        
        if font:
            # 计算文字位置使其居中
            bbox = draw.textbbox((0, 0), text, font=font)
            text_width = bbox[2] - bbox[0]
            text_height = bbox[3] - bbox[1]
            x = (size - text_width) / 2
            y = (size - text_height) / 2 - bbox[1]
            draw.text((x, y), text, font=font, fill=color)
        else:
            # 如果没有字体，画一个简单的圆形
            draw.ellipse([10, 10, size-10, size-10], fill=color)
            
    except Exception as e:
        print(f"创建文字图标失败: {e}")
        # 画一个简单的圆形作为备用
        draw.ellipse([15, 15, size-15, size-15], fill=color)
    
    return img

def create_icons(output_dir):
    """
    创建所有需要的图标
    :param output_dir: 输出目录
    """
    # 确保输出目录存在
    os.makedirs(output_dir, exist_ok=True)
    
    # 图标配置
    icons = [
        # (文件名, 文字, 颜色, 背景色)
        ("home.png", "🏠", "#999999", "#ffffff"),
        ("home-active.png", "🏠", "#1890ff", "#ffffff"),
        ("location.png", "📍", "#999999", "#ffffff"),
        ("location-active.png", "📍", "#1890ff", "#ffffff"),
        ("calendar.png", "📅", "#999999", "#ffffff"),
        ("calendar-active.png", "📅", "#1890ff", "#ffffff"),
        ("user.png", "👤", "#999999", "#ffffff"),
        ("user-active.png", "👤", "#1890ff", "#ffffff"),
    ]
    
    print("开始生成图标...")
    print(f"输出目录: {output_dir}")
    print("-" * 50)
    
    for filename, text, color, bg_color in icons:
        filepath = os.path.join(output_dir, filename)
        
        # 创建图标
        img = create_icon(text, color, size=81, bg_color=bg_color)
        
        # 保存图标
        img.save(filepath, 'PNG')
        print(f"✓ 已生成: {filename}")
    
    print("-" * 50)
    print("所有图标生成完成！")

if __name__ == "__main__":
    # 设置输出目录为小程序的 images 文件夹
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_dir = os.path.dirname(script_dir)
    output_dir = os.path.join(project_dir, "hospital-miniprogram", "images")
    
    print("=" * 50)
    print("医院预约小程序 - 图标生成器")
    print("=" * 50)
    print()
    
    create_icons(output_dir)
    
    print()
    print("提示: 请确保已安装 Pillow 库")
    print("如果未安装，请运行: pip install Pillow")
    print()
    print("生成完成后，请在微信开发者工具中重新编译项目")
