const app = getApp();
const elderly = require('../../utils/elderly');

const HOSPITAL_DATABASE = [
  {
    id: 'H001',
    name: '邛崃市医疗中心医院',
    address: '四川省成都市邛崃市文君街道文昌街70号',
    phone: '028-88791120',
    latitude: 30.657,
    longitude: 103.445,
    level: '二级甲等',
    websiteUrl: 'https://www.ql120.com',
    type: '综合医院',
    source: '本地数据'
  },
  {
    id: 'H002',
    name: '西安交通大学第一附属医院',
    address: '陕西省西安市雁塔区雁塔西路277号',
    phone: '029-85323240',
    latitude: 34.23,
    longitude: 108.95,
    level: '三级甲等',
    websiteUrl: 'https://www.dyyfy.com',
    type: '综合医院',
    source: '本地数据'
  },
  {
    id: 'H003',
    name: '西京医院',
    address: '陕西省西安市新城区长乐西路127号',
    phone: '029-84774114',
    latitude: 34.27,
    longitude: 108.97,
    level: '三级甲等',
    websiteUrl: 'https://www.xijinghospital.com',
    type: '综合医院',
    source: '本地数据'
  },
  {
    id: 'H004',
    name: '陕西省人民医院',
    address: '陕西省西安市碑林区友谊西路256号',
    phone: '029-85251331',
    latitude: 34.24,
    longitude: 108.94,
    level: '三级甲等',
    websiteUrl: 'https://www.sxhospital.com',
    type: '综合医院',
    source: '本地数据'
  },
  {
    id: 'H005',
    name: '四川大学华西医院',
    address: '四川省成都市武侯区国学巷37号',
    phone: '028-85422114',
    latitude: 30.65,
    longitude: 104.05,
    level: '三级甲等',
    websiteUrl: 'https://www.wchscu.cn',
    type: '综合医院',
    source: '本地数据'
  },
  {
    id: 'QL002',
    name: '邛崃市中医医院',
    address: '四川省成都市邛崃市临邛镇东街59号',
    phone: '028-88792837',
    latitude: 30.661,
    longitude: 103.452,
    level: '二级甲等',
    websiteUrl: null,
    type: '中医医院',
    source: '本地数据'
  },
  {
    id: 'QL003',
    name: '邛崃市第二人民医院',
    address: '四川省成都市邛崃市羊安镇',
    phone: '028-88751043',
    latitude: 30.501,
    longitude: 103.591,
    level: '二级医院',
    websiteUrl: null,
    type: '综合医院',
    source: '本地数据'
  },
  {
    id: 'CD001',
    name: '大邑县人民医院',
    address: '四川省成都市大邑县晋原镇北街323号',
    phone: '028-88222636',
    latitude: 30.591,
    longitude: 103.517,
    level: '二级甲等',
    websiteUrl: null,
    type: '综合医院',
    source: '本地数据'
  }
];

Page({
  data: {
    hospitals: [],
    markers: [],
    location: '正在获取位置...',
    latitude: null,
    longitude: null,
    keyword: '',
    radiusOptions: [3000, 5000, 10000, 20000, 50000],
    radiusLabels: ['3公里', '5公里', '10公里', '20公里', '50公里'],
    radiusIndex: 4,
    isRefreshing: false,
    lastUpdateTime: '',
    dataSource: '',
    useApiData: true,
    locationUpdateCount: 0,
    isUpdating: false,
    lastLocationTime: null,
    // 分页相关
    currentPage: 0,
    pageSize: 10,
    totalPages: 0,
    totalElements: 0,
    hasNext: false,
    hasPrevious: false,
    // 地图
    includePoints: [],
    markerCount: 0,
    totalMarkable: 0,
    excludedCount: 0,
    circles: [],
    mapScale: 9,
    located: false,
    accuracy: 0,
    summaryText: '正在获取附近医院…',
    rangeOpen: false,
    selectedHospitalId: '',
    showHospitalSheet: false,
    activeHospital: null,
    elderly: false
  },

  onLoad() {
    console.log('[页面] 加载');
    this.setData({ elderly: elderly.isElderly() });
    const selectedHospital = wx.getStorageSync('selectedHospital');
    if (selectedHospital && selectedHospital.hospitalId) {
      this.setData({ selectedHospitalId: selectedHospital.hospitalId });
    }
    this.initRealTimeLocation();
  },

  onShow() {
    console.log('[页面] 显示');
    this.setData({ elderly: elderly.isElderly() });
    const selectedHospital = wx.getStorageSync('selectedHospital');
    this.setData({
      selectedHospitalId: selectedHospital && selectedHospital.hospitalId ? selectedHospital.hospitalId : ''
    });
    this.startLocationUpdate();
  },

  onHide() {
    console.log('[页面] 隐藏');
    this.stopLocationUpdate();
  },

  onUnload() {
    console.log('[页面] 卸载');
    this.stopLocationUpdate();
  },

  initRealTimeLocation() {
    const globalLat = app.globalData.latitude;
    const globalLng = app.globalData.longitude;

    if (globalLat && globalLng) {
      console.log('[位置] 使用全局位置数据');
      this.updateLocation(globalLat, globalLng);
    } else {
      // 先用缓存位置立即渲染，再静默定位刷新，避免进入页面时被定位弹窗/遮罩卡住
      const cached = wx.getStorageSync('lastLocation');
      const lat = cached && typeof cached.latitude === 'number' ? cached.latitude : 34.23;
      const lng = cached && typeof cached.longitude === 'number' ? cached.longitude : 108.95;
      this.updateLocation(lat, lng);
      this.refreshLocationQuietly();
    }

    this.setupLocationListener();
  },

  refreshLocationQuietly() {
    wx.getLocation({
      type: 'gcj02',
      isHighAccuracy: false,
      success: (res) => {
        wx.setStorageSync('lastLocation', {
          latitude: res.latitude,
          longitude: res.longitude
        });
        app.globalData.latitude = res.latitude;
        app.globalData.longitude = res.longitude;
        this.updateLocation(res.latitude, res.longitude);
      }
    });
  },

  setupLocationListener() {
    const that = this;
    this.locationChangeCallback = (res) => {
      const now = Date.now();
      const lastTime = that.data.lastLocationTime;

      if (!lastTime || now - lastTime > 5000) {
        console.log('[位置] 实时更新:', res.latitude, res.longitude);
        that.updateLocation(res.latitude, res.longitude);
      }
    };

    if (typeof wx.onLocationChange === 'function') {
      wx.onLocationChange(this.locationChangeCallback);
      if (typeof wx.onLocationChangeFail === 'function') {
        wx.onLocationChangeFail((err) => {
          console.log('[位置] 更新失败:', err);
        });
      }
    }
  },

  startLocationUpdate() {
    if (typeof wx.startLocationUpdate !== 'function') {
      this.forceGetLocation();
      return;
    }
    wx.startLocationUpdate({
      success: () => {
        console.log('[位置] 开始实时更新');
      },
      fail: (err) => {
        console.log('[位置] 开启实时更新失败:', err);
        this.forceGetLocation();
      }
    });
  },

  stopLocationUpdate() {
    wx.stopLocationUpdate({
      success: () => {
        console.log('[位置] 停止实时更新');
      },
      fail: (err) => {
        console.log('[位置] 停止实时更新失败:', err);
      }
    });
  },

  updateLocation(latitude, longitude) {
    const currentLat = this.data.latitude;
    const currentLng = this.data.longitude;

    if (currentLat !== null && currentLng !== null) {
      const distance = this.calculateDistance(currentLat, currentLng, latitude, longitude);
      if (distance < 50) {
        console.log('[位置] 移动距离小于50米，忽略更新');
        return;
      }
    }

    this.setData({
      latitude: latitude,
      longitude: longitude,
      located: true,
      mapScale: this.getMapScale(),
      location: '当前位置',
      locationUpdateCount: this.data.locationUpdateCount + 1,
      lastLocationTime: Date.now(),
      lastUpdateTime: this.formatTime(new Date())
    });

    app.globalData.latitude = latitude;
    app.globalData.longitude = longitude;

    this.loadNearbyHospitals();
  },

  // 按搜索半径计算缩放级别：小半径让范围环填满地图，大半径保底 11 级，避免医院挤成一团
  getMapScale(radiusIndex) {
    const index = radiusIndex === undefined ? Number(this.data.radiusIndex) : Number(radiusIndex);
    const radius = this.data.radiusOptions[index] || 50000;
    const lat = this.data.latitude || 34.23;
    const cosLat = Math.max(Math.cos(lat * Math.PI / 180), 0.2);
    // 地图可视区约 375×168 px，直径最多占高度的 2/3、宽度的 0.86
    const fitPx = Math.min(375 * 0.86, 168 * 0.66);
    const metersPerPixel = (2 * radius) / fitPx;
    const ideal = Math.log2((156543.03 * cosLat) / metersPerPixel);
    return Math.max(12, Math.min(16, Math.round(ideal)));
  },

  formatTime(date) {
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    return `${hours}:${minutes}`;
  },

  calculateDistance(lat1, lon1, lat2, lon2) {
    const R = 6371000;
    const phi1 = lat1 * Math.PI / 180;
    const phi2 = lat2 * Math.PI / 180;
    const deltaPhi = (lat2 - lat1) * Math.PI / 180;
    const deltaLambda = (lon2 - lon1) * Math.PI / 180;

    const a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
              Math.cos(phi1) * Math.cos(phi2) *
              Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);

    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    const distance = R * c;

    return Math.round(distance);
  },

  formatDistance(distance) {
    if (!distance) return '';
    if (distance > 10000) {
      return Math.round(distance / 1000) + '公里';
    } else if (distance > 1000) {
      return (distance / 1000).toFixed(1) + '公里';
    }
    return distance + '米';
  },

  forceGetLocation() {
    console.log('[位置] 强制获取位置');

    wx.showLoading({ title: '正在定位...', mask: true });

    wx.getLocation({
      type: 'gcj02',
      isHighAccuracy: true,
      highAccuracyExpireTime: 10000,

      success: (res) => {
        console.log('[位置] 定位成功:', res);
        const latitude = res.latitude;
        const longitude = res.longitude;
        const accuracy = res.accuracy || '未知';

        this.setData({
          latitude: latitude,
          longitude: longitude,
          located: true,
          accuracy: res.accuracy || 0,
          mapScale: this.getMapScale(),
          location: '当前位置'
        });

        wx.hideLoading();
        wx.showToast({ title: '定位成功', icon: 'success', duration: 1500 });
        this.loadNearbyHospitals();
      },

      fail: (err) => {
        console.warn('[位置] 定位失败:', err);
        wx.hideLoading();

        wx.showModal({
          title: '定位失败',
          content: '无法获取您的位置，是否显示默认医院数据？',
          confirmText: '查看数据',
          cancelText: '重试',
          success: (res) => {
            if (res.confirm) {
              this.showDefaultData();
            } else {
              this.forceGetLocation();
            }
          }
        });
      }
    });
  },

  showDefaultData() {
    console.log('[数据] 显示默认数据');
    const qionglaiLat = 30.657;
    const qionglaiLng = 103.445;

    this.setData({
      latitude: qionglaiLat,
      longitude: qionglaiLng,
      location: '30.657, 103.445 (邛崃市中心)'
    });

    this.loadNearbyHospitals();
  },

  loadNearbyHospitals() {
    const { latitude, longitude, useApiData } = this.data;

    if (!latitude || !longitude) {
      console.warn('[加载] 没有位置数据');
      wx.showToast({ title: '请先定位', icon: 'none' });
      return;
    }

    this.setData({ isRefreshing: true });

    if (useApiData) {
      this.loadFromApi();
    } else {
      this.loadFromDatabase();
    }
  },

  loadFromDatabase() {
    console.log('[数据库] 加载医院数据');

    const { latitude, longitude, keyword, radiusOptions, radiusIndex } = this.data;
    const radius = radiusOptions[radiusIndex];

    let allHospitals = HOSPITAL_DATABASE.map(hospital => {
      const distance = this.calculateDistance(latitude, longitude, hospital.latitude, hospital.longitude);
      return {
        ...hospital,
        distance: distance,
        distanceText: this.formatDistance(distance)
      };
    });

    let filteredHospitals = allHospitals.filter(h => h.distance <= radius);

    if (keyword) {
      const kw = keyword.toLowerCase();
      filteredHospitals = filteredHospitals.filter(h =>
        h.name.toLowerCase().includes(kw) ||
        h.address.toLowerCase().includes(kw) ||
        h.type.toLowerCase().includes(kw)
      );
    }

    filteredHospitals.sort((a, b) => a.distance - b.distance);

    console.log('[数据库] 找到', filteredHospitals.length, '家医院');

    const now = new Date();
    const lastUpdateTime = `${now.getHours()}:${now.getMinutes().toString().padStart(2, '0')}`;

    this.setData({
      hospitals: filteredHospitals,
      isRefreshing: false,
      lastUpdateTime: lastUpdateTime,
      dataSource: '本地数据库'
    });

    this.updateMarkers(filteredHospitals);
  },

  loadFromApi(page = 0) {
    const { latitude, longitude, keyword, radiusOptions, radiusIndex } = this.data;
    const radius = radiusOptions[radiusIndex];
    const cacheKey = `${latitude.toFixed(4)}_${longitude.toFixed(4)}_${keyword}_${radius}`;

    if (this._allHospitals && this._cacheKey === cacheKey) {
      this.renderPage(page, this._allHospitals);
      return;
    }

    this.setData({ isRefreshing: true });

    // 地图需要完整点位，按服务端上限 size=100 正常翻页，避免一次请求 200 条被截断
    const failHandler = (err) => {
      console.error('[API] 医院数据加载失败', err);
      this.setData({ isRefreshing: false, useApiData: false });
      wx.showToast({
        title: 'API加载失败，使用本地数据',
        icon: 'none'
      });
      this.loadFromDatabase();
    };

    const finish = (content) => {
      const hospitals = content
        .filter(h => h.latitude && h.longitude)
        .map(hospital => {
          const distance = this.calculateDistance(
            latitude,
            longitude,
            hospital.latitude || 0,
            hospital.longitude || 0
          );
          return {
            id: hospital.hospitalId,
            name: hospital.hospitalName,
            address: hospital.address,
            phone: hospital.phone,
            level: hospital.level,
            websiteUrl: hospital.websiteUrl,
            latitude: hospital.latitude,
            longitude: hospital.longitude,
            distance: distance,
            distanceText: this.formatDistance(distance),
            score: this.getLevelWeight(hospital.level) - 1.2 * (distance / radius)
          };
        })
        .filter(h => h.distance <= radius);

      // 综合推荐：等级优先，其次按当前半径内的相对距离
      hospitals.sort((a, b) => (b.score - a.score) || (a.distance - b.distance));

      // 去重：同名或"西京医院 / 空军军医大学西京医院"这类包含关系只保留推荐度最高的一条
      const uniqueHospitals = [];
      hospitals.forEach((hospital) => {
        const duplicated = uniqueHospitals.some((item) => {
          if (item.name === hospital.name) return true;
          if (hospital.name.length >= 4 && item.name.indexOf(hospital.name) > -1) return true;
          if (item.name.length >= 4 && hospital.name.indexOf(item.name) > -1) return true;
          return false;
        });
        if (!duplicated) uniqueHospitals.push(hospital);
      });

      this._outOfRangeCount = Math.max(0, content.length - hospitals.length);
      this._allHospitals = uniqueHospitals;
      this._cacheKey = cacheKey;

      this.renderPage(page, uniqueHospitals);
    };

    const fetchPage = (pageNo, collected) => {
      let url = `${app.globalData.baseUrl}/api/hospitals/page?page=${pageNo}&size=100`;
      if (keyword) {
        url += `&keyword=${encodeURIComponent(keyword)}`;
      }
      wx.request({
        url: url,
        method: 'GET',
        timeout: 8000,
        success: (res) => {
          const body = res.data || {};
          const content = body.content || body.data || [];
          const merged = collected.concat(content);
          const totalPages = Number(body.totalPages || 1);
          const hasMore = body.hasMore !== undefined ? Boolean(body.hasMore) : pageNo + 1 < totalPages;
          if (hasMore && pageNo + 1 < 50) {
            fetchPage(pageNo + 1, merged);
          } else {
            finish(merged);
          }
        },
        fail: failHandler
      });
    };

    fetchPage(0, []);
  },

  // 医院等级权重，用于综合推荐排序
  getLevelWeight(level) {
    const text = level || '';
    if (text.indexOf('三级甲') > -1) return 3.2;
    if (text.indexOf('三级') > -1) return 2.9;
    if (text.indexOf('二级甲') > -1) return 2.4;
    if (text.indexOf('二级') > -1) return 2.0;
    if (text.indexOf('一级') > -1) return 1.6;
    if (text.indexOf('社区') > -1) return 1.2;
    return 1.5;
  },

  // 本地分页渲染（数据已按推荐度筛选排序）
  renderPage(page, allHospitals) {
    const { pageSize } = this.data;
    const totalElements = allHospitals.length;
    const totalPages = Math.ceil(totalElements / pageSize);
    const currentPage = totalPages > 0 ? Math.min(page, totalPages - 1) : 0;
    const start = currentPage * pageSize;
    const hospitals = allHospitals.slice(start, start + pageSize);

    const now = new Date();
    const lastUpdateTime = `${now.getHours()}:${now.getMinutes().toString().padStart(2, '0')}`;

    const radiusLabel = this.data.radiusLabels[this.data.radiusIndex];
    let summaryText = `${radiusLabel}内暂未找到医院，试试扩大范围`;
    if (totalElements > 0) {
      const nearest = allHospitals[0];
      summaryText = `${radiusLabel}内 ${totalElements} 家医院 · 最近 ${nearest.distanceText}`;
    }

    this.setData({
      hospitals: hospitals,
      isRefreshing: false,
      lastUpdateTime: lastUpdateTime,
      dataSource: '后端API',
      currentPage: currentPage,
      totalPages: totalPages,
      totalElements: totalElements,
      excludedCount: this._outOfRangeCount || 0,
      hasNext: currentPage < totalPages - 1,
      hasPrevious: currentPage > 0,
      summaryText: summaryText
    });

    this.updateMarkers(allHospitals);

    if (this._notifyRadius) {
      this._notifyRadius = false;
      wx.showToast({
        title: `已切换：${radiusLabel}内共 ${totalElements} 家`,
        icon: 'none',
        duration: 1600
      });
    }
  },

  // 上一页
  prevPage() {
    if (this.data.hasPrevious && this.data.currentPage > 0) {
      this.renderPage(this.data.currentPage - 1, this._allHospitals || this.data.hospitals);
    }
  },

  // 下一页
  nextPage() {
    if (this.data.hasNext && this.data.currentPage < this.data.totalPages - 1) {
      this.renderPage(this.data.currentPage + 1, this._allHospitals || this.data.hospitals);
    }
  },

  // 跳转到指定页
  goToPage(page) {
    if (page >= 0 && page < this.data.totalPages) {
      this.renderPage(page, this._allHospitals || this.data.hospitals);
    }
  },

  // 地图标注：按半径做网格去重，避免市中心标记堆成一团
  updateMarkers(hospitals, currentScale) {
    const { latitude, longitude, radiusOptions, radiusIndex } = this.data;
    const radius = radiusOptions[radiusIndex];
    const scale = currentScale === undefined ? (Number(this.data.mapScale) || 9) : Number(currentScale);
    const labelLimit = this.getLabelLimit(scale);
    this._mapHospitals = hospitals;
    this._labelLimit = labelLimit;
    this._markerMap = {};

    const cellMeters = Math.max(radius / 10, 300);
    const cellLat = cellMeters / 111000;
    const cosLat = Math.max(Math.cos((latitude || 34.23) * Math.PI / 180), 0.2);
    const cellLng = cellLat / cosLat;
    const seenCells = {};
    const mapHospitals = [];

    hospitals.forEach((hospital) => {
      if (!hospital.latitude || !hospital.longitude) return;
      if (mapHospitals.length >= 30) return;
      const key = `${Math.round(hospital.latitude / cellLat)}_${Math.round(hospital.longitude / cellLng)}`;
      if (seenCells[key]) return;
      seenCells[key] = true;
      mapHospitals.push(hospital);
    });

    // 视野越大文字越容易堆叠：按缩放级别控制常驻院名的数量，放大后自动展开
    const nearestIds = {};
    mapHospitals
      .map((hospital, index) => ({ id: index + 1, distance: Number(hospital.distance) || 1e12 }))
      .sort((a, b) => a.distance - b.distance)
      .slice(0, labelLimit)
      .forEach((item) => { nearestIds[item.id] = true; });

    const markers = mapHospitals.map((hospital, index) => {
      const id = index + 1;
      const showName = nearestIds[id];
      this._markerMap[id] = hospital;
      const marker = {
        id: id,
        latitude: hospital.latitude,
        longitude: hospital.longitude,
        iconPath: showName
          ? '/images/icons/map-hospital-v5.png'
          : '/images/icons/map-hospital-dot2.png',
        width: showName ? 34 : 12,
        height: showName ? 42 : 12,
        anchor: showName ? { x: 0.5, y: 0.96 } : { x: 0.5, y: 0.5 },
        // 点击查看「院名 + 距离」，最近几家额外常驻院名
        callout: {
          content: `${this.shortHospitalName(hospital.name)} · ${hospital.distanceText}`,
          display: 'BYCLICK',
          color: '#0B5E58',
          fontSize: 12,
          borderRadius: 8,
          bgColor: '#FFFFFF',
          padding: 6,
          borderWidth: 1,
          borderColor: '#D9F2EF',
          textAlign: 'center'
        }
      };

      // 常驻院名：放在针尖下方，避免顶部被地图边缘裁切
      if (showName) {
        marker.label = {
          content: this.shortHospitalName(hospital.name),
          color: '#0B5E58',
          fontSize: 11,
          anchorX: 0,
          anchorY: 2,
          borderWidth: 1,
          borderColor: '#14B8A6',
          borderRadius: 6,
          bgColor: '#F3FFFC',
          padding: 4,
          textAlign: 'center'
        };
      }

      return marker;
    });

    // 半径范围环：直观展示当前搜索范围
    const circles = [];
    if (latitude && longitude) {
      circles.push({
        latitude: latitude,
        longitude: longitude,
        radius: radius,
        color: '#14B8A6',
        fillColor: 'rgba(20,184,166,0.03)',
        strokeWidth: 2
      });
    }

    this.setData({
      markers: markers,
      circles: circles,
      markerCount: markers.length,
      totalMarkable: hospitals.length
    });
  },

  // 按当前缩放级别决定地图上常驻院名的数量
  getLabelLimit(scale) {
    if (scale >= 14) return 10;
    if (scale >= 13) return 8;
    if (scale >= 12) return 4;
    if (scale >= 11) return 4;
    return 3;
  },

  // 用户缩放/拖动地图后，按新级别补充或收起院名
  onRegionChange(e) {
    const scaleValue = e && e.detail && Number(e.detail.scale);
    if (!scaleValue || !this._mapHospitals) return;
    const limit = this.getLabelLimit(Math.round(scaleValue));
    if (limit === this._labelLimit) return;
    this.updateMarkers(this._mapHospitals, scaleValue);
  },

  onMarkerTap(e) {
    const hospital = this._markerMap && this._markerMap[e.markerId];
    if (hospital) {
      this.showHospitalModal(hospital);
    }
  },

  // 地图气泡里用短名称，避免长院名把地图盖住
  shortHospitalName(name) {
    if (!name) return '';
    let text = String(name)
      .replace(/^空军军医大学/, '')
      .replace(/^西安交通大学/, '交大')
      .replace(/^陕西中医药大学/, '陕中医大')
      .replace(/^陕西省/, '省');
    return text.length > 9 ? text.slice(0, 9) + '…' : text;
  },

  onKeywordInput(e) {
    this.setData({ keyword: e.detail.value });
  },

  onRadiusChange(e) {
    this.setData({ radiusIndex: e.detail.value });
    this.loadNearbyHospitals();
  },

  // 胶囊按钮切换搜索范围
  onRadiusTap(e) {
    const index = Number(e.currentTarget.dataset.index);
    if (Number(this.data.radiusIndex) === index) {
      this.setData({ rangeOpen: false });
      return;
    }

    this.setData({
      radiusIndex: index,
      rangeOpen: false,
      mapScale: this.getMapScale(index),
      summaryText: '正在筛选医院…'
    });
    this._notifyRadius = true;
    this.loadNearbyHospitals();
  },

  toggleRangePanel() {
    this.setData({ rangeOpen: !this.data.rangeOpen });
  },

  closeRangePanel() {
    this.setData({ rangeOpen: false });
  },

  searchHospitals() {
    this.loadNearbyHospitals();
  },

  // 预约：切换首页展示医院
  reserveHospital(e) {
    const hospital = e.currentTarget.dataset.hospital;
    if (!hospital || !hospital.id) {
      wx.showToast({ title: '医院信息不完整', icon: 'none' });
      return;
    }

    const selected = {
      hospitalId: hospital.id,
      hospitalName: hospital.name,
      address: hospital.address,
      phone: hospital.phone,
      level: hospital.level,
      websiteUrl: hospital.websiteUrl,
      latitude: hospital.latitude,
      longitude: hospital.longitude
    };

    if (typeof wx.vibrateShort === 'function') {
      wx.vibrateShort({ type: 'light', fail: () => {} });
    }

    wx.setStorageSync('selectedHospital', selected);
    app.globalData.selectedHospital = selected;
    this.setData({
      selectedHospitalId: hospital.id,
      showHospitalSheet: false
    });

    wx.showToast({ title: '已切换就诊医院', icon: 'success', duration: 1200 });
    setTimeout(() => {
      wx.switchTab({ url: '/pages/index/index' });
    }, 700);
  },

  viewHospital(e) {
    const hospital = e.currentTarget.dataset.hospital;
    this.showHospitalModal(hospital);
  },

  showHospitalModal(hospital) {
    if (!hospital) return;
    this.setData({
      activeHospital: hospital,
      showHospitalSheet: true
    });
  },

  closeHospitalSheet() {
    this.setData({ showHospitalSheet: false });
  },

  makePhoneCall(e) {
    const phone = e.currentTarget.dataset.phone;

    if (!phone) {
      wx.showToast({ title: '暂无电话', icon: 'none' });
      return;
    }

    wx.setClipboardData({
      data: phone,
      success: () => {
        wx.showToast({ title: '电话已复制', icon: 'success' });
      }
    });
  },

  openHospitalWebsite(e) {
    const websiteUrl = e.currentTarget.dataset.websiteUrl;

    if (!websiteUrl) {
      wx.showToast({ title: '暂无官网链接', icon: 'none' });
      return;
    }

    wx.setClipboardData({
      data: websiteUrl,
      success: () => {
        wx.showModal({
          title: '链接已复制',
          content: `已复制医院官网链接到剪贴板，请在浏览器中粘贴访问：\n${websiteUrl}`,
          confirmText: '我知道了',
          showCancel: false
        });
      },
      fail: () => {
        wx.showToast({ title: '复制链接失败', icon: 'none' });
      }
    });
  },

  navigateToHospitalOnMap(e) {
    const hospital = e.currentTarget.dataset.hospital;

    wx.openLocation({
      latitude: hospital.latitude,
      longitude: hospital.longitude,
      name: hospital.name,
      address: hospital.address,
      scale: 18
    });
  }
});
