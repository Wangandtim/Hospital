import os

# 高德地图 Web 服务 API Key：
# 优先读取环境变量 AMAP_KEY；本地开发可放在 config_local.py（该文件已被 .gitignore 排除）。
# 获取 Key: https://lbs.amap.com/
try:
    from config_local import AMAP_KEY as _LOCAL_AMAP_KEY
except ImportError:
    _LOCAL_AMAP_KEY = ""

AMAP_KEY = os.environ.get("AMAP_KEY") or _LOCAL_AMAP_KEY

# 未配置 Key 时关闭高德调用，接口自动降级
AMAP_ENABLED = bool(AMAP_KEY)

# 高德地图API地址
AMAP_AROUND_API = 'https://restapi.amap.com/v3/place/around'
AMAP_DETAIL_API = 'https://restapi.amap.com/v3/place/detail'
AMAP_GEOCODE_API = 'https://restapi.amap.com/v3/geocode/regeo'

# 缓存配置
CACHE_TTL = 600  # 10分钟缓存
