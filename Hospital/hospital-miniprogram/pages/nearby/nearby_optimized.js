const app = getApp();

const config = require('../../config/index');

Page({
  data: {
    hospitals: [],
    markers: [],
    location: '',
    latitude: null,
    longitude: null,
    keyword: '',
    radiusOptions: [1000, 3000, 5000, 10000, 20000, 50000],
    radiusLabels: ['1公里', '3公里', '5公里', '10公里', '20公里', '50公里'],
    radiusIndex: 2, // 默认5公里
    loading: false,
    autoRefreshInterval: null,
    autoRefreshEnabled: true,
    lastUpdateTime: '',
    showLocationMarker: true,
    mapScale: 15,
    followUserLocation: true,
    isRefreshing: false
  },

  onLoad() {
    this.getLocation();
    this.startAutoRefresh();
  },

  onShow() {
    if (this.data.latitude && this.data.longitude) {
      this.loadNearbyHospitals();
    }
    if (this.data.autoRefreshEnabled) {
      this.startAutoRefresh();
    }
  },

  onHide() {
    this.stopAutoRefresh();
  },

  onUnload() {
    this.stopAutoRefresh();
  },

  /**
   * 开始自动刷新
   */
  startAutoRefresh() {
    this.stopAutoRefresh();
    // 每15秒刷新一次，提升实时性
    this.data.autoRefreshInterval = setInterval(() => {
      if (this.data.followUserLocation) {
        this.getLocation(true); // 静默获取位置
      } else {
        this.loadNearbyHospitals();
      }
    }, 15000);
  },

  /**
   * 停止自动刷新
   */
  stopAutoRefresh() {
    if (this.data.autoRefreshInterval) {
      clearInterval(this.data.autoRefreshInterval);
      this.data.autoRefreshInterval = null;
    }
  },

  /**
   * 获取用户位置
   */
  getLocation(silent = false) {
    if (!silent) {
      wx.showLoading({ title: '获取位置中...' });
    }
    
    wx.getLocation({
      type: 'gcj02',
      altitude: true, // 获取高度信息
      isHighAccuracy: true, // 开启高精度定位
      highAccuracyExpireTime: 3000, // 高精度定位超时时间
      success: (res) => {
        const newLat = res.latitude;
        const newLon = res.longitude;
        
        // 计算位置变化距离（米）
        let shouldRefresh = true;
        if (this.data.latitude && this.data.longitude) {
          const distance = this.calculateDistance(
            this.data.latitude, this.data.longitude,
            newLat, newLon
          );
          // 只有移动超过50米才刷新
          shouldRefresh = distance > 50;
        }
        
        this.setData({
          latitude: newLat,
          longitude: newLon,
          location: `${newLat.toFixed(6)}, ${newLon.toFixed(6)}`
        });
        
        if (!silent) {
          wx.hideLoading();
        }
        
        if (shouldRefresh || !this.data.hospitals.length) {
          this.loadNearbyHospitals(true); // 强制刷新
        }
      },
      fail: (err) => {
        console.error('获取位置失败:', err);
        if (!silent) {
          wx.hideLoading();
          // 使用欧亚学院作为默认位置
          this.setData({
            latitude: 34.1968,
            longitude: 108.8952,
            location: '34.1968, 108.8952 (欧亚学院)'
          });
          wx.showToast({
            title: '使用默认位置',
            icon: 'none',
            duration: 2000
          });
          this.loadNearbyHospitals();
        }
      }
    });
  },

  /**
   * 手动刷新位置
   */
  refreshLocation() {
    this.getLocation(false);
  },

  /**
   * 搜索框输入
   */
  onKeywordInput(e) {
    this.setData({ keyword: e.detail.value });
  },

  /**
   * 搜索框确认
   */
  onKeywordConfirm() {
    this.searchHospitals();
  },

  /**
   * 范围变化
   */
  onRadiusChange(e) {
    this.setData({ radiusIndex: e.detail.value });
    this.loadNearbyHospitals(true);
  },

  /**
   * 搜索医院
   */
  searchHospitals() {
    if (!this.data.latitude || !this.data.longitude) {
      this.getLocation();
      return;
    }
    
    this.loadNearbyHospitals(true);
  },

  /**
   * 计算两个坐标点之间的距离
   */
  calculateDistance(lat1, lon1, lat2, lon2) {
    const R = 6371000; // 地球半径（米）
    const phi1 = (lat1 * Math.PI) / 180;
    const phi2 = (lat2 * Math.PI) / 180;
    const deltaPhi = ((lat2 - lat1) * Math.PI) / 180;
    const deltaLambda = ((lon2 - lon1) * Math.PI) / 180;
    
    const a = 
      Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
      Math.cos(phi1) * Math.cos(phi2) * Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
    
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    
    return R * c;
  },

  /**
   * 格式化距离显示
   */
  formatDistance(distance) {
    if (distance > 10000) {
      return (distance / 1000).toFixed(0) + 'km';
    } else if (distance > 1000) {
      return (distance / 1000).toFixed(1) + 'km';
    }
    return distance + 'm';
  },

  /**
   * 切换自动刷新
   */
  toggleAutoRefresh() {
    const newEnabled = !this.data.autoRefreshEnabled;
    this.setData({ autoRefreshEnabled: newEnabled });
    
    if (newEnabled) {
      this.startAutoRefresh();
      wx.showToast({
        title: '自动刷新已开启',
        icon: 'success'
      });
    } else {
      this.stopAutoRefresh();
      wx.showToast({
        title: '自动刷新已关闭',
        icon: 'success'
      });
    }
  },

  /**
   * 切换跟随用户位置
   */
  toggleFollowLocation() {
    const newFollow = !this.data.followUserLocation;
    this.setData({ followUserLocation: newFollow });
    
    wx.showToast({
      title: newFollow ? '位置跟随已开启' : '位置跟随已关闭',
      icon: 'success'
    });
  },

  /**
   * 加载附近医院
   */
  loadNearbyHospitals(forceRefresh = false) {
    if (!this.data.latitude || !this.data.longitude) {
      console.warn('缺少坐标信息');
      return;
    }
    
    this.setData({ isRefreshing: true });

    const radius = this.data.radiusOptions[this.data.radiusIndex];
    
    
    wx.request({
      url: config.apiBase + '/api/nearby/hospitals',
      method: 'POST',
      data: {
        latitude: this.data.latitude,
        longitude: this.data.longitude,
        radius: radius,
        count: 30,
        keyword: this.data.keyword || '医院',
        force_refresh: forceRefresh
      },
      timeout: 10000,
      success: (res) => {
        
        const responseData = res.data;
        let hospitals = [];
        
        if (responseData.success && responseData.data) {
          hospitals = responseData.data;
        } else if (Array.isArray(responseData)) {
          hospitals = responseData;
        }
        
        // 格式化距离
        hospitals = hospitals.map((hospital, index) => ({
          ...hospital,
          distanceText: this.formatDistance(hospital.distance),
          key: hospital.id || `hospital_${index}`
        }));
        
        const now = new Date();
        const lastUpdateTime = `${now.getHours()}:${now.getMinutes().toString().padStart(2, '0')}`;
        
        this.setData({
          hospitals: hospitals,
          isRefreshing: false,
          loading: false,
          lastUpdateTime: lastUpdateTime
        });
        
        this.updateMarkers(hospitals);
      },
      fail: (err) => {
        console.error('医院数据加载失败:', err);
        this.setData({ 
          isRefreshing: false, 
          loading: false 
        });
        
        wx.showToast({
          title: '加载失败，请确认后端服务已启动',
          icon: 'none',
          duration: 3000
        });
      }
    });
  },

  /**
   * 更新地图标记
   */
  updateMarkers(hospitals) {
    const markers = hospitals.map((hospital, index) => ({
      id: index + 1,
      latitude: hospital.latitude,
      longitude: hospital.longitude,
      title: hospital.name,
      width: 30,
      height: 30,
      callout: {
        content: hospital.name,
        color: '#333',
        fontSize: 10,
        borderRadius: 4,
        bgColor: '#ffffff',
        padding: 4,
        display: 'ALWAYS',
        textAlign: 'center',
        anchorY: 0 // 气泡位于标记下方
      },
      iconPath: '', // 使用默认标记
      zIndex: 100 - index // 距离近的显示在上面
    }));
    
    this.setData({ markers: markers });
  },

  /**
   * 地图标记点击
   */
  onMarkerTap(e) {
    const markerId = e.markerId;
    const hospital = this.data.hospitals[markerId - 1];
    if (hospital) {
      this.viewHospital({ 
        currentTarget: { 
          dataset: { 
            hospital: hospital 
          } 
        } 
      });
    }
  },

  /**
   * 地图区域改变
   */
  onRegionChange(e) {
    if (e.type === 'end' && e.causedBy === 'drag') {
      // 用户手动拖动地图，暂停位置跟随
      if (this.data.followUserLocation) {
        this.setData({ followUserLocation: false });
        wx.showToast({
          title: '位置跟随已暂停',
          icon: 'none',
          duration: 1500
        });
      }
    }
  },

  /**
   * 查看医院详情
   */
  viewHospital(e) {
    const hospital = e.currentTarget.dataset.hospital;
    
    wx.showModal({
      title: hospital.name,
      content: `地址：${hospital.address}\n电话：${hospital.phone || '暂无'}\n距离：${hospital.distanceText}`,
      confirmText: '查看详情',
      cancelText: '关闭',
      success: (res) => {
        if (res.confirm) {
          this.navigateToHospitalDetail(hospital);
        }
      }
    });
  },

  /**
   * 导航到医院详情
   */
  navigateToHospitalDetail(hospital) {
    // 详情直接由列表数据渲染，不再调用爬虫侧不存在的详情接口（避免 404）
    let content = `名称：${hospital.name}\n`;
    if (hospital.level) {
      content += `等级：${hospital.level}\n`;
    }
    if (hospital.type) {
      content += `类型：${hospital.type}\n`;
    }
    content += `地址：${hospital.address}\n`;
    content += `电话：${hospital.phone || '暂无'}`;

    wx.showModal({
      title: hospital.name,
      content: content,
      showCancel: false,
      confirmText: '好的'
    });
  },

  /**
   * 拨打电话
   */
  makePhoneCall(e) {
    const phone = e.currentTarget.dataset.phone;
    if (!phone) {
      wx.showToast({
        title: '暂无电话',
        icon: 'none'
      });
      return;
    }
    
    wx.showModal({
      title: '提示',
      content: `是否拨打 ${phone}？`,
      confirmText: '拨打',
      cancelText: '复制',
      success: (res) => {
        if (res.confirm) {
          wx.makePhoneCall({
            phoneNumber: phone,
            fail: () => {
              // 如果拨打失败，尝试复制到剪贴板
              wx.setClipboardData({
                data: phone,
                success: () => {
                  wx.showToast({
                    title: '电话已复制',
                    icon: 'success'
                  });
                }
              });
            }
          });
        } else {
          wx.setClipboardData({
            data: phone,
            success: () => {
              wx.showToast({
                title: '电话已复制',
                icon: 'success',
                duration: 2000
              });
            }
          });
        }
      }
    });
  },

  /**
   * 跳转到预约
   */
  goToAppointment(e) {
    const hospital = e.currentTarget.dataset.hospital;
    
    if (hospital && hospital.appointmentUrl) {
      wx.showModal({
        title: '预约就诊',
        content: `点击确认将复制 ${hospital.name} 的预约链接到剪贴板`,
        confirmText: '复制链接',
        cancelText: '取消',
        success: (res) => {
          if (res.confirm) {
            wx.setClipboardData({
              data: hospital.appointmentUrl,
              success: () => {
                wx.showToast({
                  title: '预约链接已复制',
                  icon: 'success',
                  duration: 2500
                });
              }
            });
          }
        }
      });
    } else {
      wx.showToast({
        title: '该医院暂无预约链接',
        icon: 'none'
      });
    }
  },

  /**
   * 在地图中导航到医院
   */
  navigateToHospitalOnMap(e) {
    const hospital = e.currentTarget.dataset.hospital;
    
    if (!hospital.latitude || !hospital.longitude) {
      wx.showToast({
        title: '缺少位置信息',
        icon: 'none'
      });
      return;
    }
    
    wx.openLocation({
      latitude: hospital.latitude,
      longitude: hospital.longitude,
      name: hospital.name,
      address: hospital.address,
      scale: 18
    });
  }
})
