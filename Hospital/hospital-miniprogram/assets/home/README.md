# 首页实景素材

## hospital-scene.jpg

- 用途：首页「纯色 → 实景」下半部分的整幅医院场景照片
- 画面：真实医院门诊楼外观（红砖楼体、入口雨棚）
- 来源：Unsplash 图库，图片直链 `https://images.unsplash.com/photo-1586773860418-d37222d8fce3`
- 授权：Unsplash License，可免费用于商业与非商业项目，无需署名（本文件保留来源记录，便于追溯）
- 授权说明：https://unsplash.com/license
- 说明：Unsplash 直链不包含摄影师姓名信息，因此未在此处虚构署名；如需正式署名，请在图库页面按图片 ID 查询作者后补充
- 本地处理：从 1200px 宽原图居中裁切为 1080×720，JPEG 质量 70，最终约 83KB

## 替换方式

- 直接替换同路径 `hospital-scene.jpg` 即可，页面通过 `pages/index/index.js` 的 `homeSceneImage` 引用
- 建议尺寸 1080×720（3:2），JPEG 质量 65–75，控制在 100KB 以内
- 未接入视频：主包限制 2MB，当前只使用静态照片；如需视频，请放 CDN 或分包，并在页面中补 poster 与失败降级逻辑
