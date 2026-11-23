const app = getApp();
const elderly = require('../../utils/elderly');

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

    circles: [],
    mapScale: 9,
    located: false,
    accuracy: 0,
    summaryText: '正在获取附近医院…',
    rangeOpen: false,
    selectedHospitalId: '',
    showHospitalSheet: false,
    activeHospital: null,
    elderly: false,
    locationError: false,
    loadError: false
  },

  onLoad() {
    this.setData({ elderly: elderly.isElderly() });
    const selectedHospital = wx.getStorageSync('selectedHospital');
    if (selectedHospital && selectedHospital.hospitalId) {
      this.setData({ selectedHospitalId: selectedHospital.hospitalId });
    }
    this.initRealTimeLocation();
  },

  onShow() {
    this.setData({ elderly: elderly.isElderly() });
    const selectedHospital = wx.getStorageSync('selectedHospital');
    this.setData({
      selectedHospitalId: selectedHospital && selectedHospital.hospitalId ? selectedHospital.hospitalId : ''
    });
    this.startLocationUpdate();
  },

  onHide() {
    this.stopLocationUpdate();
  },

  onUnload() {
    this.stopLocationUpdate();
  },

  initRealTimeLocation() {
    const globalLat = app.globalData.latitude;
    const globalLng = app.globalData.longitude;

    if (globalLat && globalLng) {
      this.updateLocation(globalLat, globalLng);
    } else {
      // 先用缓存位置立即渲染，再静默定位刷新；没有缓存时保持空态等待真实定位
      const cached = wx.getStorageSync('lastLocation');
      if (cached && typeof cached.latitude === 'number' && typeof cached.longitude === 'number') {
        this.updateLocation(cached.latitude, cached.longitude);
      } else {
        this.setData({ location: '正在定位…', summaryText: '正在获取附近医院…' });
      }
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
      },
      fail: () => {
        this.setData({
          locationError: true,
          isRefreshing: false,
          summaryText: '无法获取位置，请允许定位后重试'
        });
      }
    });
  },

  setupLocationListener() {
    const that = this;
    this.locationChangeCallback = (res) => {
      const now = Date.now();
      const lastTime = that.data.lastLocationTime;

      if (!lastTime || now - lastTime > 5000) {
        that.updateLocation(res.latitude, res.longitude);
      }
    };

    if (typeof wx.onLocationChange === 'function') {
      wx.onLocationChange(this.locationChangeCallback);
      if (typeof wx.onLocationChangeFail === 'function') {
        wx.onLocationChangeFail((err) => {
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
      },
      fail: (err) => {
        this.forceGetLocation();
      }
    });
  },

  stopLocationUpdate() {
    wx.stopLocationUpdate({
      success: () => {
      },
      fail: (err) => {
      }
    });
  },

  updateLocation(latitude, longitude) {
    const currentLat = this.data.latitude;
    const currentLng = this.data.longitude;

    if (currentLat !== null && currentLng !== null) {
      const distance = this.calculateDistance(currentLat, currentLng, latitude, longitude);
      if (distance < 50) {
        return;
      }
    }

    this.setData({
      latitude: latitude,
      longitude: longitude,
      located: true,
      locationError: false,
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

    wx.showLoading({ title: '正在定位...', mask: true });

    wx.getLocation({
      type: 'gcj02',
      isHighAccuracy: true,
      highAccuracyExpireTime: 10000,

      success: (res) => {
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

        // 定位失败不再回落到默认坐标/全量数据，给出明确空态与重试入口
        this._nearbySeq = (this._nearbySeq || 0) + 1;
        this.setData({
          latitude: null,
          longitude: null,
          located: false,
          locationError: true,
          hospitals: [],
          markers: [],
          circles: [],
          currentPage: 0,
          totalPages: 0,
          totalElements: 0,
          hasNext: false,
          hasPrevious: false,
          isRefreshing: false,
          summaryText: '定位失败，请允许定位后重试',
          loadError: false
        });
        wx.showToast({ title: '定位失败，请检查定位权限', icon: 'none' });
        const msg = String((err && err.errMsg) || '');
        if (msg.indexOf('auth deny') > -1 || msg.indexOf('authorize') > -1) {
          wx.showModal({
            title: '未获得定位权限',
            content: '可在设置中开启定位后重试，也可按医院名称搜索。',
            confirmText: '去设置',
            cancelText: '取消',
            success: (res) => {
              if (res.confirm) wx.openSetting({});
            }
          });
        }
      }
    });
  },

  loadNearbyHospitals(options) {
    const { latitude, longitude } = this.data;
    const reset = !options || options.reset !== false;

    if (!latitude || !longitude) {
      console.warn('[加载] 没有位置数据');
      this.setData({ locationError: true, isRefreshing: false, summaryText: '没有位置信息，请先定位' });
      return;
    }

    if (reset) {
      // 切换范围/关键词：先清掉旧范围的结果，再从第一页重拉
      this._nearbySeq = (this._nearbySeq || 0) + 1;
      this._allHospitals = null;
      this.setData({
        hospitals: [],
        currentPage: 0,
        totalPages: 0,
        totalElements: 0,
        hasNext: false,
        hasPrevious: false,
        loadError: false
      });
    }

    this.setData({ isRefreshing: true });
    this.loadFromApi(0, this._nearbySeq);
  },

  reloadNearby() {
    this.setData({ loadError: false });
    this.loadNearbyHospitals({ reset: true });
  },

  // 定位被拒时的替代路径：回到首页按医院名称搜索
  goSearchHospital() {
    wx.switchTab({ url: '/pages/index/index' });
  },

  loadFromApi(page = 0, seqArg) {
    const { latitude, longitude, keyword, radiusOptions, radiusIndex, pageSize } = this.data;
    const radiusKm = (radiusOptions[radiusIndex] || 50000) / 1000;
    const seq = seqArg === undefined ? (this._nearbySeq || 0) : seqArg;

    this.setData({ isRefreshing: true, loadError: false });

    let url = `${app.globalData.baseUrl}/api/hospitals/page?page=${page}&size=${pageSize}` +
      `&lat=${latitude}&lng=${longitude}`;
    if (radiusKm > 0) {
      url += `&radius=${radiusKm}`;
    }
    if (keyword) {
      url += `&keyword=${encodeURIComponent(keyword)}`;
    }

    wx.request({
      url: url,
      method: 'GET',
      timeout: 8000,
      success: (res) => {
        if (seq !== this._nearbySeq) return;
        const body = res.data || {};
        const content = body.content || body.data || [];
        const totalElements = Number(body.total !== undefined ? body.total : content.length);
        const totalPages = Number(body.totalPages !== undefined ? body.totalPages : 1);
        const hasMore = body.hasMore !== undefined ? Boolean(body.hasMore) : page + 1 < totalPages;

        // 半径模式下后端已按距离升序返回，直接展示，不再本地筛选/重排
        const hospitals = content.map(hospital => {
          let distance;
          if (hospital.distanceKm !== undefined && hospital.distanceKm !== null) {
            distance = Math.round(Number(hospital.distanceKm) * 1000);
          } else {
            distance = this.calculateDistance(
              latitude,
              longitude,
              hospital.latitude || 0,
              hospital.longitude || 0
            );
          }
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
            distanceText: this.formatDistance(distance)
          };
        });

        this._allHospitals = hospitals;
        this.renderPage(page, hospitals, seq, {
          total: totalElements,
          totalPages: totalPages,
          hasMore: hasMore
        });
      },
      fail: (err) => {
        if (seq !== this._nearbySeq) return;
        console.error('[API] 医院数据加载失败', err);
        this.setData({
          isRefreshing: false,
          loadError: true,
          hospitals: [],
          markers: [],
          summaryText: '医院加载失败，请重试'
        });
      }
    });
  },

  // 服务端分页渲染（后端已按距离升序返回当前页）
  renderPage(page, hospitals, seqArg, meta) {
    const seq = seqArg === undefined ? (this._nearbySeq || 0) : seqArg;
    if (seq !== this._nearbySeq) return;
    const info = meta || {};
    const totalElements = Number(info.total || 0);
    const totalPages = Number(info.totalPages || 1);

    const now = new Date();
    const lastUpdateTime = `${now.getHours()}:${now.getMinutes().toString().padStart(2, '0')}`;

    const radiusLabel = this.data.radiusLabels[this.data.radiusIndex];
    let summaryText = `${radiusLabel}内暂未找到医院，试试扩大范围`;
    if (totalElements > 0) {
      const nearest = hospitals[0];
      summaryText = `${radiusLabel}内 ${totalElements} 家医院` +
        (page === 0 && nearest && nearest.distanceText ? ` · 最近 ${nearest.distanceText}` : '');
    }

    this.setData({
      hospitals: hospitals,
      isRefreshing: false,
      lastUpdateTime: lastUpdateTime,
      dataSource: '后端API',
      currentPage: page,
      totalPages: totalPages,
      totalElements: totalElements,
      hasNext: info.hasMore !== undefined ? Boolean(info.hasMore) : page + 1 < totalPages,
      hasPrevious: page > 0,
      summaryText: summaryText
    });

    this.updateMarkers(hospitals);

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
      this.loadFromApi(this.data.currentPage - 1);
    }
  },

  // 下一页
  nextPage() {
    if (this.data.hasNext) {
      this.loadFromApi(this.data.currentPage + 1);
    }
  },

  // 跳转到指定页
  goToPage(page) {
    if (page >= 0 && page < this.data.totalPages) {
      this.loadFromApi(page);
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
    if (this._keywordTimer) clearTimeout(this._keywordTimer);
    this._keywordTimer = setTimeout(() => {
      this.setData({ summaryText: '正在搜索医院…' });
      this.loadNearbyHospitals({ reset: true });
    }, 400);
  },

  clearKeyword() {
    if (this._keywordTimer) clearTimeout(this._keywordTimer);
    this.setData({ keyword: '' });
    this.loadNearbyHospitals({ reset: true });
  },

  onRadiusChange(e) {
    this.setData({ radiusIndex: e.detail.value, rangeOpen: false });
    this.loadNearbyHospitals({ reset: true });
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
    this.loadNearbyHospitals({ reset: true });
  },

  toggleRangePanel() {
    this.setData({ rangeOpen: !this.data.rangeOpen });
  },

  closeRangePanel() {
    this.setData({ rangeOpen: false });
  },

  searchHospitals() {
    if (this._keywordTimer) clearTimeout(this._keywordTimer);
    this.loadNearbyHospitals({ reset: true });
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
