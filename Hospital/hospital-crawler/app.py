from flask import Flask, request, jsonify
import requests
import math
import os
import time

app = Flask(__name__)

# 配置（统一走环境变量，默认值与 hospital-api 的 amap.key 保持一致）
AMAP_KEY = os.environ.get('AMAP_KEY', 'f7c1b0a4aea35d38e6a07f611a48ed9c')
AMAP_ENABLED = os.environ.get('AMAP_ENABLED', 'true').lower() != 'false'
CACHE_TTL = 300

# 医院缓存
hospital_cache = {
    'data': None,
    'lat': None,
    'lon': None,
    'time': None,
    'radius': None
}

# 备用医院数据（邛崃附近）
DEFAULT_HOSPITALS = [
    {
        'id': 'QL001',
        'name': '邛崃市医疗中心医院',
        'address': '四川省成都市邛崃市文君街道文昌街70号',
        'phone': '028-88791120',
        'latitude': 30.657,
        'longitude': 103.447,
        'distance': 0,
        'level': None,
        'type': '综合医院',
        'tags': ['综合'],
        'source': '本地数据'
    },
    {
        'id': 'QL002',
        'name': '邛崃市中医医院',
        'address': '四川省成都市邛崃市临邛镇东街59号',
        'phone': '028-88792837',
        'latitude': 30.661,
        'longitude': 103.452,
        'distance': 1200,
        'level': '二级甲等',
        'type': '中医医院',
        'tags': ['二甲', '中医'],
        'source': '本地数据'
    },
    {
        'id': 'QL003',
        'name': '邛崃市第二人民医院',
        'address': '四川省成都市邛崃市羊安镇',
        'phone': '028-88751043',
        'latitude': 30.501,
        'longitude': 103.591,
        'distance': 18000,
        'level': None,
        'type': '综合医院',
        'tags': ['综合'],
        'source': '本地数据'
    },
    {
        'id': 'QL004',
        'name': '邛崃骨科医院',
        'address': '四川省成都市邛崃市临邛镇',
        'phone': '028-88768120',
        'latitude': 30.665,
        'longitude': 103.458,
        'distance': 2000,
        'level': None,
        'type': '专科医院',
        'tags': ['骨科', '专科'],
        'source': '本地数据'
    },
    {
        'id': 'QL005',
        'name': '邛崃福利医院',
        'address': '四川省成都市邛崃市临邛镇',
        'phone': '028-88772120',
        'latitude': 30.654,
        'longitude': 103.448,
        'distance': 800,
        'level': None,
        'type': '综合医院',
        'tags': ['综合'],
        'source': '本地数据'
    },
    {
        'id': 'QL006',
        'name': '邛崃市妇幼保健院',
        'address': '四川省成都市邛崃市临邛镇',
        'phone': '028-88771150',
        'latitude': 30.662,
        'longitude': 103.449,
        'distance': 1500,
        'level': None,
        'type': '妇幼保健院',
        'tags': ['妇幼', '专科'],
        'source': '本地数据'
    },
    {
        'id': 'CD001',
        'name': '邛崃市人民医院',
        'address': '四川省成都市邛崃市临邛镇',
        'phone': '028-88790120',
        'latitude': 30.659,
        'longitude': 103.451,
        'distance': 1300,
        'level': None,
        'type': '综合医院',
        'tags': ['综合'],
        'source': '本地数据'
    },
    {
        'id': 'CD002',
        'name': '大邑县人民医院',
        'address': '四川省成都市大邑县晋原镇北街323号',
        'phone': '028-88222636',
        'latitude': 30.591,
        'longitude': 103.517,
        'distance': 12000,
        'level': '二级甲等',
        'type': '综合医院',
        'tags': ['二甲', '综合'],
        'source': '本地数据'
    },
    {
        'id': 'CD003',
        'name': '蒲江县人民医院',
        'address': '四川省成都市蒲江县鹤山镇河西路59号',
        'phone': '028-88522131',
        'latitude': 30.196,
        'longitude': 103.521,
        'distance': 52000,
        'level': None,
        'type': '综合医院',
        'tags': ['综合'],
        'source': '本地数据'
    },
    {
        'id': 'CD004',
        'name': '新津区人民医院',
        'address': '四川省成都市新津区五津西路149号',
        'phone': '028-82523027',
        'latitude': 30.421,
        'longitude': 103.814,
        'distance': 42000,
        'level': None,
        'type': '综合医院',
        'tags': ['综合'],
        'source': '本地数据'
    }
]

def calculate_distance(lat1, lon1, lat2, lon2):
    R = 6371000
    phi1 = math.radians(lat1)
    phi2 = math.radians(lat2)
    delta_phi = math.radians(lat2 - lat1)
    delta_lambda = math.radians(lon2 - lon1)
    a = math.sin(delta_phi / 2) ** 2 + math.cos(phi1) * math.cos(phi2) * math.sin(delta_lambda / 2) ** 2
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    return R * c

def search_hospitals_from_amap(lat, lon, radius=50000, keyword='医院', count=50):
    if AMAP_KEY == 'YOUR_AMAP_KEY' or not AMAP_ENABLED:
        return None
    
    try:
        params = {
            'key': AMAP_KEY,
            'keywords': keyword,
            'location': f'{lon},{lat}',
            'radius': radius,
            'types': '090100',
            'offset': count,
            'extensions': 'all'
        }
        
        print(f'[高德API] 请求: loc={lat},{lon}, r={radius}, k={keyword}')
        
        response = requests.get('https://restapi.amap.com/v3/place/around', params=params, timeout=15)
        
        if response.status_code == 200:
            data = response.json()
            print(f'[高德API] 响应: status={data.get("status")}, info={data.get("info")}')
            
            if data.get('status') == '1':
                pois = data.get('pois', [])
                print(f'[高德API] 获取到 {len(pois)} 个POI')
                if pois:
                    print(f'[高德API] 第一个POI: {pois[0].get("name")}, {pois[0].get("location")}')
                return pois
    except Exception as e:
        print(f'[高德API] 错误: {e}')
    
    return None

def convert_poi_to_hospital(poi, user_lat, user_lon):
    location = poi.get('location', '0,0').split(',')
    lon = float(location[0]) if location[0] else user_lon
    lat = float(location[1]) if len(location) > 1 and location[1] else user_lat
    
    distance = calculate_distance(user_lat, user_lon, lat, lon)
    
    return {
        'id': poi.get('id', ''),
        'name': poi.get('name', '医院'),
        'address': poi.get('address', ''),
        'phone': poi.get('tel', ''),
        'latitude': lat,
        'longitude': lon,
        'distance': int(distance),
        'level': None,
        'type': '综合医院',
        'tags': ['医院'],
        'source': '高德地图'
    }

@app.route('/api/health', methods=['GET'])
def health_check():
    return jsonify({
        'status': 'ok',
        'message': '医院搜索服务',
        'amapEnabled': AMAP_KEY != 'YOUR_AMAP_KEY' and AMAP_ENABLED
    })

@app.route('/api/hospital/nearby', methods=['POST'])
def nearby_hospitals():
    print('\n' + '=' * 60)
    data = request.json or {}
    print(f'[请求] 收到请求: {data}')
    
    latitude = data.get('latitude', 30.657)
    longitude = data.get('longitude', 103.445)
    radius = data.get('radius', 50000)
    count = data.get('count', 30)
    keyword = data.get('keyword', '医院')
    force_refresh = data.get('force_refresh', False)
    
    print(f'[请求] 参数: lat={latitude}, lng={longitude}, r={radius}')
    
    # 检查缓存
    now = time.time()
    if not force_refresh and hospital_cache['data'] and abs(hospital_cache['lat'] - latitude) < 0.01 and abs(hospital_cache['lon'] - longitude) < 0.01 and (now - hospital_cache['time']) < CACHE_TTL:
        print('[响应] 返回缓存')
        return jsonify({
            'success': True,
            'data': hospital_cache['data'][:count],
            'source': '缓存',
            'count': len(hospital_cache['data'][:count])
        })
    
    # 先尝试高德API
    hospitals = []
    amap_pois = search_hospitals_from_amap(latitude, longitude, radius, keyword, count)
    
    if amap_pois and len(amap_pois) > 0:
        print('[转换] 转换POI数据')
        hospitals = [convert_poi_to_hospital(poi, latitude, longitude) for poi in amap_pois]
        print(f'[转换] 转换了 {len(hospitals)} 家医院')
    else:
        print('[备用] 使用本地数据')
        hospitals = []
        for h in DEFAULT_HOSPITALS:
            h_copy = h.copy()
            h_copy['distance'] = int(calculate_distance(latitude, longitude, h_copy['latitude'], h_copy['longitude']))
            if h_copy['distance'] <= radius:
                hospitals.append(h_copy)
    
    # 确保数据有效
    if not hospitals or len(hospitals) == 0:
        print('[备用] 没有数据，使用默认数据')
        hospitals = DEFAULT_HOSPITALS.copy()
        for h in hospitals:
            h['distance'] = int(calculate_distance(latitude, longitude, h['latitude'], h['longitude']))
    
    # 排序
    hospitals.sort(key=lambda x: x['distance'])
    
    print(f'[响应] 返回 {len(hospitals)} 家医院')
    
    # 更新缓存
    hospital_cache['data'] = hospitals
    hospital_cache['lat'] = latitude
    hospital_cache['lon'] = longitude
    hospital_cache['time'] = now
    hospital_cache['radius'] = radius
    
    print('=' * 60)
    
    return jsonify({
        'success': True,
        'data': hospitals[:count],
        'source': '高德地图' if amap_pois else '本地数据',
        'count': len(hospitals[:count]),
        'totalCount': len(hospitals)
    })

if __name__ == '__main__':
    # 端口规范：爬虫仅内网监听 127.0.0.1:5000，由 hospital-api 的 /api/nearby/** 代理对外
    HOST = os.environ.get('CRAWLER_HOST', '127.0.0.1')
    PORT = int(os.environ.get('CRAWLER_PORT', '5000'))
    print('=' * 60)
    print('医院搜索服务启动')
    print('=' * 60)
    print(f'高德API: {AMAP_KEY != "YOUR_AMAP_KEY" and AMAP_ENABLED}')
    print(f'备用医院: {len(DEFAULT_HOSPITALS)} 家')
    print(f'服务: http://{HOST}:{PORT}')
    print('=' * 60)
    app.run(host=HOST, port=PORT, debug=False)
