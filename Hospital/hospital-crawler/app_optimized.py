from flask import Flask, request, jsonify
from flask_cors import CORS
import requests
import math
import time
import json
import os
from functools import lru_cache

app = Flask(__name__)
CORS(app)

# 尝试导入配置
try:
    from config import AMAP_KEY, AMAP_ENABLED, CACHE_TTL
    from config import AMAP_AROUND_API, AMAP_DETAIL_API, AMAP_GEOCODE_API
except ImportError:
    # 默认配置
    AMAP_KEY = 'YOUR_AMAP_KEY'
    AMAP_ENABLED = False
    CACHE_TTL = 600
    AMAP_AROUND_API = 'https://restapi.amap.com/v3/place/around'
    AMAP_DETAIL_API = 'https://restapi.amap.com/v3/place/detail'
    AMAP_GEOCODE_API = 'https://restapi.amap.com/v3/geocode/regeo'

# 医院数据缓存
hospital_cache = {
    'data': None,
    'lat': None,
    'lon': None,
    'time': None,
    'radius': None
}

# 医院详情缓存
hospital_detail_cache = {}

# 备用医院数据（作为兜底）
DEFAULT_HOSPITALS = [
    {
        'id': 'XA003',
        'name': '西安欧亚学院门诊部',
        'address': '西安市雁塔区东仪路8号',
        'phone': '029-88286888',
        'emergencyPhone': '120',
        'appointmentUrl': None,
        'latitude': 34.1968,
        'longitude': 108.8952,
        'distance': 100,
        'level': None,
        'type': '社区医院',
        'is24Hours': False,
        'city': '西安',
        'tags': ['社区', '学校'],
        'source': 'default'
    },
    {
        'id': 'XA001',
        'name': '西安交通大学第一附属医院',
        'address': '西安市雁塔区雁塔西路277号',
        'phone': '029-85323333',
        'emergencyPhone': '029-85323333',
        'appointmentUrl': 'https://www.dyyfyy.com',
        'latitude': 34.2287,
        'longitude': 108.9299,
        'distance': 1200,
        'level': '三级甲等',
        'type': '综合医院',
        'is24Hours': True,
        'city': '西安',
        'tags': ['三甲', '综合', '急诊'],
        'source': 'default'
    },
    {
        'id': 'XA002',
        'name': '陕西省人民医院',
        'address': '西安市碑林区友谊西路256号',
        'phone': '029-85251331',
        'emergencyPhone': '029-85251331',
        'appointmentUrl': 'https://www.spph-sx.com',
        'latitude': 34.2387,
        'longitude': 108.9099,
        'distance': 2500,
        'level': '三级甲等',
        'type': '综合医院',
        'is24Hours': True,
        'city': '西安',
        'tags': ['三甲', '综合', '急诊'],
        'source': 'default'
    },
    {
        'id': 'XA004',
        'name': '西安市中心医院',
        'address': '西安市新城区西五路161号',
        'phone': '029-87279234',
        'emergencyPhone': '029-87279234',
        'appointmentUrl': 'https://www.xaszxyy.com',
        'latitude': 34.2587,
        'longitude': 108.9599,
        'distance': 1800,
        'level': '三级甲等',
        'type': '综合医院',
        'is24Hours': True,
        'city': '西安',
        'tags': ['三甲', '综合', '急诊'],
        'source': 'default'
    },
    {
        'id': 'XA005',
        'name': '第四军医大学西京医院',
        'address': '西安市新城区长乐西路169号',
        'phone': '029-84775517',
        'emergencyPhone': '029-84775517',
        'appointmentUrl': 'https://www.xjyy.com',
        'latitude': 34.2687,
        'longitude': 108.9699,
        'distance': 2200,
        'level': '三级甲等',
        'type': '综合医院',
        'is24Hours': True,
        'city': '西安',
        'tags': ['三甲', '综合', '知名'],
        'source': 'default'
    }
]

def calculate_distance(lat1, lon1, lat2, lon2):
    """计算两个坐标点之间的距离（米）"""
    R = 6371000  # 地球半径
    phi1 = math.radians(lat1)
    phi2 = math.radians(lat2)
    delta_phi = math.radians(lat2 - lat1)
    delta_lambda = math.radians(lon2 - lon1)
    a = math.sin(delta_phi / 2) ** 2 + math.cos(phi1) * math.cos(phi2) * math.sin(delta_lambda / 2) ** 2
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    return R * c

def reverse_geocode(lat, lon):
    """逆地理编码：坐标转地址"""
    if AMAP_KEY == 'YOUR_AMAP_KEY' or not AMAP_ENABLED:
        return None
    
    try:
        params = {
            'key': AMAP_KEY,
            'location': f'{lon},{lat}',
            'poitype': '190201',  # 医院POI
            'radius': 50000,
            'extensions': 'all'
        }
        response = requests.get(AMAP_GEOCODE_API, params=params, timeout=10)
        if response.status_code == 200:
            data = response.json()
            if data.get('status') == '1':
                return data.get('regeocode', {})
    except Exception as e:
        print(f'逆地理编码失败: {e}')
    
    return None

def search_hospitals_from_amap(lat, lon, radius=50000, keyword='医院', count=50):
    """从高德地图API搜索附近医院"""
    if AMAP_KEY == 'YOUR_AMAP_KEY' or not AMAP_ENABLED:
        return None
    
    try:
        params = {
            'key': AMAP_KEY,
            'keywords': keyword,
            'location': f'{lon},{lat}',
            'radius': radius,
            'types': '090100',  # 医院类型
            'offset': count,
            'extensions': 'all'
        }
        
        print(f'请求高德API: 位置={lat},{lon}, 半径={radius}米')
        response = requests.get(AMAP_AROUND_API, params=params, timeout=10)
        
        if response.status_code == 200:
            data = response.json()
            print(f'高德API响应: 状态={data.get("status")}, 数量={len(data.get("pois", []))}')
            if data.get('status') == '1':
                return data.get('pois', [])
    except Exception as e:
        print(f'高德API请求失败: {e}')
    
    return None

def get_hospital_detail_from_amap(poi_id):
    """从高德地图API获取医院详情"""
    if AMAP_KEY == 'YOUR_AMAP_KEY' or not AMAP_ENABLED:
        return None
    
    try:
        params = {
            'key': AMAP_KEY,
            'id': poi_id
        }
        response = requests.get(AMAP_DETAIL_API, params=params, timeout=10)
        if response.status_code == 200:
            data = response.json()
            if data.get('status') == '1':
                pois = data.get('pois', [])
                if pois:
                    return pois[0]
    except Exception as e:
        print(f'获取医院详情失败: {e}')
    
    return None

def extract_hospital_level(poi):
    """从POI数据中提取医院等级"""
    name = poi.get('name', '')
    address = poi.get('address', '')
    tags = poi.get('tag', '')
    
    level_keywords = {
        '三级甲等': ['三甲', '三级甲等'],
        '三级乙等': ['三级乙等'],
        '二级甲等': ['二甲', '二级甲等'],
        '二级乙等': ['二级乙等'],
        '一级甲等': ['一甲', '一级甲等']
    }
    
    for level, keywords in level_keywords.items():
        for keyword in keywords:
            if keyword in name or keyword in address or keyword in tags:
                return level
    
    return None

def extract_hospital_type(poi):
    """从POI数据中提取医院类型"""
    name = poi.get('name', '')
    biz_type = poi.get('biz_type', '')
    tag = poi.get('tag', '')
    
    type_keywords = {
        '综合医院': ['综合'],
        '专科医院': ['专科'],
        '中医医院': ['中医'],
        '妇幼保健院': ['妇幼', '妇儿'],
        '儿童医院': ['儿童'],
        '口腔医院': ['口腔'],
        '眼科医院': ['眼科'],
        '骨科医院': ['骨科']
    }
    
    for h_type, keywords in type_keywords.items():
        for keyword in keywords:
            if keyword in name or keyword in biz_type or keyword in tag:
                return h_type
    
    return '综合医院'

def convert_to_hospital(poi, user_lat, user_lon):
    """将高德POI数据转换为医院对象"""
    location = poi.get('location', '0,0').split(',')
    lon = float(location[0]) if len(location) > 0 else user_lon
    lat = float(location[1]) if len(location) > 1 else user_lat
    
    distance = calculate_distance(user_lat, user_lon, lat, lon)
    
    address = poi.get('address', '')
    city = poi.get('cityname', '')
    name = poi.get('name', '医院')
    
    # 提取医院等级和类型
    level = extract_hospital_level(poi)
    h_type = extract_hospital_type(poi)
    
    # 生成标签
    tags = ['医院']
    if level:
        tags.append(level)
    if h_type:
        tags.append(h_type)
    
    # 尝试匹配预约链接
    appointment_url = None
    for dh in DEFAULT_HOSPITALS:
        if dh['name'] in name or name in dh['name']:
            appointment_url = dh.get('appointmentUrl')
            if not level:
                level = dh.get('level')
            tags = dh.get('tags', tags)
            break
    
    return {
        'id': poi.get('id', ''),
        'name': name,
        'address': address,
        'phone': poi.get('tel', ''),
        'emergencyPhone': poi.get('tel', '120'),
        'appointmentUrl': appointment_url,
        'latitude': lat,
        'longitude': lon,
        'distance': int(distance),
        'level': level,
        'type': h_type,
        'is24Hours': True,  # 假设医院24小时
        'city': city,
        'tags': tags,
        'source': 'amap'
    }

def merge_and_sort_hospitals(amap_hospitals, user_lat, user_lon, radius):
    """合并高德数据和默认数据，排序并去重"""
    all_hospitals = {}
    
    # 添加高德数据
    for h in amap_hospitals:
        all_hospitals[h['id']] = h
    
    # 添加默认数据（只添加不在高德数据中的）
    for dh in DEFAULT_HOSPITALS:
        dh = dh.copy()
        dh['distance'] = int(calculate_distance(
            user_lat, user_lon, dh['latitude'], dh['longitude']
        ))
        if dh['distance'] <= radius and dh['id'] not in all_hospitals:
            all_hospitals[dh['id']] = dh
    
    # 转换为列表并按距离排序
    hospital_list = list(all_hospitals.values())
    hospital_list.sort(key=lambda x: x['distance'])
    
    return hospital_list

@app.route('/api/health', methods=['GET'])
def health_check():
    """健康检查接口"""
    return jsonify({
        'status': 'ok',
        'message': '医院信息搜索服务 v4.0 (优化版)',
        'version': '4.0',
        'hospital_count': len(DEFAULT_HOSPITALS),
        'amap_enabled': AMAP_KEY != 'YOUR_AMAP_KEY' and AMAP_ENABLED,
        'amap_key_configured': AMAP_KEY != 'YOUR_AMAP_KEY'
    })

@app.route('/api/config', methods=['GET', 'POST'])
def config():
    """配置管理接口"""
    global AMAP_KEY, AMAP_ENABLED, CACHE_TTL
    
    if request.method == 'GET':
        return jsonify({
            'amap_enabled': AMAP_ENABLED,
            'amap_key_configured': AMAP_KEY != 'YOUR_AMAP_KEY',
            'cache_ttl': CACHE_TTL
        })
    
    # 更新配置
    data = request.json or {}
    if 'amap_key' in data:
        AMAP_KEY = data['amap_key']
    if 'amap_enabled' in data:
        AMAP_ENABLED = data['amap_enabled']
    if 'cache_ttl' in data:
        CACHE_TTL = data['cache_ttl']
    
    # 清空缓存
    hospital_cache['data'] = None
    hospital_cache['time'] = None
    
    return jsonify({
        'success': True,
        'message': '配置已更新'
    })

@app.route('/api/hospital/nearby', methods=['POST'])
def nearby_hospitals():
    """获取附近医院 - 优化版"""
    data = request.json or {}
    latitude = data.get('latitude')
    longitude = data.get('longitude')
    radius = data.get('radius', 50000)
    count = data.get('count', 30)
    keyword = data.get('keyword', '医院')
    force_refresh = data.get('force_refresh', False)
    
    print(f'请求附近医院 - 位置: {latitude},{longitude}, 范围: {radius}米, 关键词: {keyword}')
    
    # 验证坐标
    if not latitude or not longitude:
        return jsonify({
            'success': False,
            'message': '缺少坐标参数',
            'data': DEFAULT_HOSPITALS
        })
    
    # 检查缓存
    now = time.time()
    if not force_refresh and hospital_cache['data'] and \
       abs(hospital_cache['lat'] - latitude) < 0.01 and \
       abs(hospital_cache['lon'] - longitude) < 0.01 and \
       abs(hospital_cache['radius'] - radius) < 5000 and \
       (now - hospital_cache['time']) < CACHE_TTL:
        print('返回缓存数据')
        return jsonify({
            'success': True,
            'data': hospital_cache['data'][:count],
            'source': 'cache',
            'count': len(hospital_cache['data'][:count])
        })
    
    # 尝试从高德API获取
    amap_pois = search_hospitals_from_amap(latitude, longitude, radius, keyword, count)
    hospitals = []
    
    if amap_pois:
        # 转换POI数据
        hospitals = [convert_to_hospital(poi, latitude, longitude) for poi in amap_pois]
        print(f'从高德API获取了 {len(hospitals)} 家医院')
        
        # 合并并排序
        hospitals = merge_and_sort_hospitals(hospitals, latitude, longitude, radius)
    else:
        # 高德API失败，使用备用数据
        print('高德API不可用，使用备用数据')
        hospitals = []
        for h in DEFAULT_HOSPITALS:
            hospital = h.copy()
            hospital['distance'] = int(calculate_distance(
                latitude, longitude, hospital['latitude'], hospital['longitude']
            ))
            if hospital['distance'] <= radius:
                hospitals.append(hospital)
        
        hospitals.sort(key=lambda x: x['distance'])
    
    # 更新缓存
    hospital_cache['data'] = hospitals
    hospital_cache['lat'] = latitude
    hospital_cache['lon'] = longitude
    hospital_cache['radius'] = radius
    hospital_cache['time'] = now
    
    return jsonify({
        'success': True,
        'data': hospitals[:count],
        'source': 'amap' if amap_pois else 'default',
        'count': len(hospitals[:count])
    })

@app.route('/api/hospital/detail', methods=['POST'])
def hospital_detail():
    """获取医院详情"""
    data = request.json or {}
    hospital_id = data.get('id', '')
    hospital_name = data.get('name', '')
    
    # 尝试从缓存获取
    cache_key = hospital_id or hospital_name
    if cache_key in hospital_detail_cache:
        cached_time, cached_data = hospital_detail_cache[cache_key]
        if time.time() - cached_time < CACHE_TTL * 2:
            return jsonify({
                'success': True,
                'data': cached_data,
                'source': 'cache'
            })
    
    # 尝试从高德获取详情
    if hospital_id:
        amap_detail = get_hospital_detail_from_amap(hospital_id)
        if amap_detail:
            # 转换格式
            result = {
                'id': amap_detail.get('id'),
                'name': amap_detail.get('name'),
                'address': amap_detail.get('address'),
                'phone': amap_detail.get('tel'),
                'latitude': float(amap_detail.get('location', '0,0').split(',')[1]),
                'longitude': float(amap_detail.get('location', '0,0').split(',')[0]),
                'level': extract_hospital_level(amap_detail),
                'type': extract_hospital_type(amap_detail),
                'tag': amap_detail.get('tag'),
                'business_area': amap_detail.get('business_area'),
                'city': amap_detail.get('cityname'),
                'source': 'amap'
            }
            # 更新缓存
            hospital_detail_cache[cache_key] = (time.time(), result)
            return jsonify({
                'success': True,
                'data': result,
                'source': 'amap'
            })
    
    # 从默认数据查找
    for h in DEFAULT_HOSPITALS:
        if h['id'] == hospital_id or (hospital_name and hospital_name in h['name']):
            hospital_detail_cache[cache_key] = (time.time(), h)
            return jsonify({
                'success': True,
                'data': h,
                'source': 'default'
            })
    
    return jsonify({
        'success': False,
        'message': '未找到医院信息'
    })

@app.route('/api/hospital/search', methods=['GET', 'POST'])
def search_hospital():
    """搜索医院"""
    if request.method == 'GET':
        keyword = request.args.get('name', '') or request.args.get('keyword', '')
        latitude = request.args.get('latitude', type=float)
        longitude = request.args.get('longitude', type=float)
    else:
        data = request.json or {}
        keyword = data.get('name', '') or data.get('keyword', '')
        latitude = data.get('latitude')
        longitude = data.get('longitude')
    
    # 如果有坐标，先尝试从高德搜索
    if latitude and longitude and keyword:
        pois = search_hospitals_from_amap(latitude, longitude, 100000, keyword, 20)
        if pois:
            results = [convert_to_hospital(poi, latitude, longitude) for poi in pois]
            return jsonify({
                'success': True,
                'data': results,
                'source': 'amap'
            })
    
    # 否则从默认数据搜索
    results = []
    for h in DEFAULT_HOSPITALS:
        if not keyword or keyword in h['name'] or keyword in h['city'] or keyword in h['address']:
            results.append(h)
    
    return jsonify({
        'success': True,
        'data': results,
        'source': 'default'
    })

@app.route('/api/hospital/reverse-geocode', methods=['POST'])
def reverse_geocode_endpoint():
    """逆地理编码接口"""
    data = request.json or {}
    latitude = data.get('latitude')
    longitude = data.get('longitude')
    
    if not latitude or not longitude:
        return jsonify({
            'success': False,
            'message': '缺少坐标参数'
        })
    
    result = reverse_geocode(latitude, longitude)
    if result:
        return jsonify({
            'success': True,
            'data': result
        })
    else:
        return jsonify({
            'success': False,
            'message': '逆地理编码失败'
        })

@app.route('/api/cache/clear', methods=['POST'])
def clear_cache():
    """清空缓存"""
    hospital_cache['data'] = None
    hospital_cache['time'] = None
    hospital_detail_cache.clear()
    
    return jsonify({
        'success': True,
        'message': '缓存已清空'
    })

if __name__ == '__main__':
    print('='*70)
    print('医院信息搜索服务 v4.0 (优化版)')
    print('='*70)
    print(f'高德地图API: {"已启用" if AMAP_ENABLED else "未启用"}')
    print(f'高德Key配置: {"已配置" if AMAP_KEY != "YOUR_AMAP_KEY" else "未配置"}')
    print(f'备用医院: {len(DEFAULT_HOSPITALS)}家')
    print(f'缓存时间: {CACHE_TTL}秒')
    print('='*70)
    print('API接口:')
    print('  GET /api/health - 健康检查')
    print('  GET/POST /api/config - 配置管理')
    print('  POST /api/hospital/nearby - 附近医院')
    print('  POST /api/hospital/detail - 医院详情')
    print('  GET/POST /api/hospital/search - 搜索医院')
    print('  POST /api/hospital/reverse-geocode - 逆地理编码')
    print('  POST /api/cache/clear - 清空缓存')
    print('='*70)
    print('服务地址: http://127.0.0.1:%s' % os.environ.get('CRAWLER_PORT', '5000'))
    print('='*70)
    print('使用说明:')
    print('  1. 修改 config.py 配置高德地图API Key')
    print('  2. 获取Key: https://lbs.amap.com/')
    print('  3. 设置 AMAP_ENABLED = True 启用高德API')
    print('='*70)
    app.run(host='0.0.0.0', port=5000, debug=False, use_reloader=False)
