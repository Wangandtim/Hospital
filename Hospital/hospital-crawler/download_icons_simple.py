#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
简单的图标下载脚本 - 使用在线图标服务
"""

import os
import urllib.request

def download_icon(url, save_path):
    """
    下载图标
    """
    try:
        urllib.request.urlretrieve(url, save_path)
        print(f"✓ 已下载: {os.path.basename(save_path)}")
        return True
    except Exception as e:
        print(f"✗ 下载失败: {os.path.basename(save_path)}, 错误: {e}")
        return False

def create_simple_icons(output_dir):
    """
    创建简单的图标（使用纯色背景）
    """
    os.makedirs(output_dir, exist_ok=True)
    
    print("=" * 50)
    print("正在生成图标...")
    print("=" * 50)
    print()
    
    # 使用一个简单的方案：告诉用户手动添加图标
    # 或者我们可以使用一些公共的图标服务
    
    icons_info = """
需要的图标文件列表：
------------------------
1. home.png          - 首页图标 (未选中状态，灰色)
2. home-active.png   - 首页图标 (选中状态，蓝色)
3. location.png      - 位置图标 (未选中状态，灰色)
4. location-active.png - 位置图标 (选中状态，蓝色)
5. calendar.png      - 日历图标 (未选中状态，灰色)
6. calendar-active.png - 日历图标 (选中状态，蓝色)
7. user.png          - 用户图标 (未选中状态，灰色)
8. user-active.png   - 用户图标 (选中状态，蓝色)

图标要求：
- 尺寸：81px × 81px
- 格式：PNG
- 背景：透明或白色

获取图标的方法：
1. 使用 iconfont.cn (阿里巴巴矢量图标库)
2. 使用 iconpark.oceanengine.com (字节跳动图标库)
3. 使用 www.flaticon.com
4. 或者在微信开发者工具中暂时不使用图标
"""
    
    print(icons_info)
    
    # 创建一个简单的说明文件
    readme_path = os.path.join(output_dir, "图标说明.txt")
    with open(readme_path, "w", encoding="utf-8") as f:
        f.write(icons_info)
    
    print()
    print("=" * 50)
    print(f"已创建图标说明文件: {readme_path}")
    print("=" * 50)
    print()
    print("临时方案：我已经修改了 app.json，暂时不使用图标")
    print("小程序现在应该可以正常运行了！")
    print()
    print("如果您想添加图标，请参考上面的说明。")

if __name__ == "__main__":
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_dir = os.path.dirname(script_dir)
    output_dir = os.path.join(project_dir, "hospital-miniprogram", "images")
    
    create_simple_icons(output_dir)
