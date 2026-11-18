# -*- coding: utf-8 -*-
"""把 brand-new/icons 的 SVG 批量渲染为 96x96 透明 PNG，输出到临时目录。
逐张生成内嵌 HTML，调用 Chrome headless 截图。
"""
import os, io, subprocess, time, shutil, sys

BASE = os.path.join(os.path.dirname(__file__), "..")
SVG_DIR = os.path.join(BASE, "icons")
TMP = os.path.join(os.environ.get("TEMP", "/tmp"), "hl_png_batch")
OUT = os.path.join(BASE, "_png_export", "icons")
CHROME = r"C:\Program Files\Google\Chrome\Application\chrome.exe"

os.makedirs(TMP, exist_ok=True)
os.makedirs(OUT, exist_ok=True)

svgs = [f for f in os.listdir(SVG_DIR) if f.lower().endswith(".svg")]
print(f"待渲染图标: {len(svgs)}")

for i, name in enumerate(svgs, 1):
    svg = io.open(os.path.join(SVG_DIR, name), encoding="utf-8").read().strip()
    html = (f"<html><head><meta charset='utf-8'></head>"
            f"<body style='margin:0;width:96px;height:96px'>{svg}</body></html>")
    hpath = os.path.join(TMP, "item.html")
    io.open(hpath, "w", encoding="utf-8").write(html)
    png_name = name[:-4] + ".png"
    opng = os.path.join(OUT, png_name)
    url = "file:///" + hpath.replace("\\", "/")
    if os.path.exists(opng):
        os.remove(opng)
    subprocess.run([CHROME, "--headless=new", "--disable-gpu",
                    "--default-background-color=00000000", "--hide-scrollbars",
                    "--window-size=96,96", f"--screenshot={opng}", url],
                   check=False, capture_output=True)
    time.sleep(0.25)
    if not os.path.exists(opng):
        print(f"  [缺失] {png_name}")
    if i % 10 == 0:
        print(f"  进度 {i}/{len(svgs)}")

print("图标 PNG 导出完成:", len(os.listdir(OUT)))
