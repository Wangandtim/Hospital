const config = require('./config/index');
const elderly = require('./utils/elderly');
const agent = require('./utils/agent');

App({
  globalData: {
    userInfo: null,
    token: null,
    openid: null,
    userRole: null,
    baseUrl: config.apiBase,
    hasPatientInfo: false,
    latitude: null,
    longitude: null,
    locationUpdateTime: null,
    isOnline: true,
    cacheUpdateTime: null,
    elderlyMode: false,
    agent: null
  },
  onLaunch() {
    this.globalData.elderlyMode = elderly.isElderly();
    this.globalData.agent = agent;
    this.checkApiBase();
    this.installAuthHeader();
    this.checkLoginStatus();
    this.initNetworkStatus();
    this.preloadData();
  },

  /** 正式环境地址未配置时给出明确提示，避免各页面静默失败。 */
  checkApiBase() {
    if (!config.releaseUnconfigured) {
      return;
    }
    wx.showModal({
      title: '配置缺失',
      content: '正式环境接口地址未配置，请联系管理员。',
      showCancel: false
    });
  },

  /**
   * 统一给所有 wx.request 注入登录令牌。
   * 项目里存在大量直接调用 wx.request 的历史页面，集中在这一层处理，
   * 避免逐个页面漏加请求头导致 401。
   */
  installAuthHeader() {
    if (this.authHeaderInstalled) {
      return;
    }
    this.authHeaderInstalled = true;

    const that = this;
    const rawRequest = wx.request;
    wx.request = function (options) {
      try {
        const token = wx.getStorageSync('token') || wx.getStorageSync('authToken');
        if (token) {
          options.header = Object.assign({}, options.header || {}, { 'X-Auth-Token': token });
        }
        // 统一默认超时，避免弱网下请求长时间挂起
        if (!options.timeout) {
          options.timeout = 10000;
        }
      } catch (e) {
        // 本地存储异常时按未登录处理
      }

      // 令牌失效统一兜底：清登录态并回到登录页，避免各页面各自处理导致会话卡死
      const rawSuccess = options.success;
      if (typeof rawSuccess === 'function') {
        options.success = function (res) {
          try {
            if (res && res.statusCode === 401) {
              that.handleAuthExpired();
            }
          } catch (e) {
            // 忽略状态判断异常，不影响业务回调
          }
          return rawSuccess.apply(this, arguments);
        };
      }
      return rawRequest.call(wx, options);
    };
  },

  handleAuthExpired() {
    if (this.authExpiredHandling) {
      return;
    }
    this.authExpiredHandling = true;
    try {
      wx.removeStorageSync('token');
      wx.removeStorageSync('authToken');
      wx.removeStorageSync('userRole');
    } catch (e) {
      // 忽略存储异常
    }
    this.globalData.token = null;
    this.globalData.userRole = null;
    wx.showToast({ title: '登录已过期，请重新登录', icon: 'none', duration: 2000 });
    setTimeout(() => {
      wx.reLaunch({
        url: '/pages/login/login',
        complete: () => {
          this.authExpiredHandling = false;
        }
      });
    }, 900);
  },

  onShow() {
    console.log('[App] onShow');
  },

  onHide() {
    this.stopLocationUpdate();
  },

  checkLoginStatus() {
    const token = wx.getStorageSync('token');
    const openid = wx.getStorageSync('openid');
    const userRole = wx.getStorageSync('userRole');
    if (token) {
      this.globalData.token = token;
      this.globalData.openid = openid || 'mock_openid';
      this.globalData.userRole = userRole;
      this.globalData.hasPatientInfo = wx.getStorageSync('hasPatientInfo') || false;
    }
  },

  initLocation() {
    wx.getLocation({
      type: 'gcj02',
      isHighAccuracy: false,
      timeout: 3000,
      success: (res) => {
        console.log('[位置] 获取成功:', res.latitude, res.longitude);
        this.updateLocation(res.latitude, res.longitude);
        this.setupLocationChangeListener();
      },
      fail: (err) => {
        console.log('[位置] 获取失败:', err);
      }
    });
  },

  setupLocationChangeListener() {
    wx.startLocationUpdate({
      success: () => {
        console.log('[位置] 开启实时更新成功');
        wx.onLocationChange((res) => {
          this.handleLocationChange(res.latitude, res.longitude);
        });
      },
      fail: (err) => {
        console.log('[位置] 开启实时更新失败:', err);
      }
    });
  },

  handleLocationChange(latitude, longitude) {
    const oldLat = this.globalData.latitude;
    const oldLng = this.globalData.longitude;

    if (oldLat && oldLng) {
      const distance = this.calculateDistance(oldLat, oldLng, latitude, longitude);
      console.log('[位置] 移动距离:', distance, '米');

      if (distance < 100) {
        return;
      }
    }

    this.updateLocation(latitude, longitude);
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
    this.globalData.latitude = latitude;
    this.globalData.longitude = longitude;
    this.globalData.locationUpdateTime = Date.now();

    console.log('[位置] 更新:', latitude.toFixed(6), longitude.toFixed(6));

    if (this.globalData.token) {
      this.updateLocationToServer(latitude, longitude);
    }
  },

  requestLocationPermission() {
    wx.getSetting({
      success: (res) => {
        if (!res.authSetting['scope.userLocation']) {
          wx.authorize({
            scope: 'scope.userLocation',
            success: () => {
              this.initLocation();
            },
            fail: () => {
              console.log('[位置] 用户拒绝位置权限');
              wx.showModal({
                title: '提示',
                content: '需要获取您的位置信息来推荐附近医院',
                showCancel: false
              });
            }
          });
        }
      }
    });
  },

  updateLocationToServer(latitude, longitude) {
    if (!this.globalData.token) return;

    wx.request({
      url: this.globalData.baseUrl + '/api/wechat/location',
      method: 'POST',
      header: {
        'X-Openid': this.globalData.openid
      },
      data: {
        latitude: latitude,
        longitude: longitude
      },
      fail: () => {
        console.log('[位置] 位置更新请求失败');
      }
    });
  },

  login() {
    return new Promise((resolve, reject) => {
      wx.login({
        success: (loginRes) => {
          this.requestSimpleLogin(loginRes.code)
            .then(resolve)
            .catch(reject);
        },
        fail: reject
      });
    });
  },

  requestSimpleLogin(code) {
    return new Promise((resolve, reject) => {
      console.log('[登录] 开始登录，code:', code);
      console.log('[登录] 请求地址:', this.globalData.baseUrl + '/api/wechat/login');

      wx.request({
        url: this.globalData.baseUrl + '/api/wechat/login',
        method: 'POST',
        data: {
          code: code,
          nickname: '微信用户',
          avatarUrl: ''
        },
        success: (res) => {
          console.log('[登录] 响应:', res);
          if (res.statusCode === 200) {
            this.globalData.token = res.data.token;
            this.globalData.openid = res.data.openid;
            this.globalData.hasPatientInfo = res.data.hasPatientInfo;

            wx.setStorageSync('token', res.data.token);
            wx.setStorageSync('openid', res.data.openid);
            wx.setStorageSync('hasPatientInfo', res.data.hasPatientInfo);

            console.log('[登录] 成功，openid:', res.data.openid);
            resolve(res.data);
          } else {
            console.error('[登录] 失败，状态码:', res.statusCode);
            reject(res);
          }
        },
        fail: (err) => {
          console.error('[登录] 请求失败:', err);
          reject(err);
        }
      });
    });
  },

  initNetworkStatus() {
    const that = this;
    
    wx.getNetworkType({
      success: (res) => {
        that.setOnlineStatus(res.networkType !== 'none');
      },
      fail: () => {
        that.globalData.isOnline = false;
      }
    });

    wx.onNetworkStatusChange((res) => {
      that.setOnlineStatus(res.isConnected);
      
      if (res.isConnected) {
        that.syncPendingAppointments();
        that.preloadData();
      }
    });
  },

  setOnlineStatus(isOnline) {
    this.globalData.isOnline = isOnline;
    console.log('[网络状态]', isOnline ? '在线' : '离线');
    
    if (!isOnline) {
      wx.showToast({
        title: '当前处于离线模式',
        icon: 'none',
        duration: 2000
      });
    }
  },

  preloadData() {
    const that = this;
    const lastUpdate = wx.getStorageSync('cacheUpdateTime');
    const now = Date.now();
    
    if (lastUpdate && (now - lastUpdate < 24 * 60 * 60 * 1000)) {
      console.log('[缓存] 数据未过期，使用本地缓存');
      return;
    }

    if (!this.globalData.isOnline) {
      console.log('[缓存] 离线模式，跳过数据预加载');
      return;
    }

    console.log('[缓存] 开始预加载数据...');

    wx.request({
      url: this.globalData.baseUrl + '/api/hospitals',
      method: 'GET',
      success: (res) => {
        if (res.data && Array.isArray(res.data)) {
          wx.setStorageSync('hospitals', res.data);
          console.log('[缓存] 医院数据已缓存');
        }
      },
      fail: () => {
        console.log('[缓存] 获取医院数据失败');
      }
    });

    wx.request({
      url: this.globalData.baseUrl + '/api/departments',
      method: 'GET',
      success: (res) => {
        if (res.data && Array.isArray(res.data)) {
          wx.setStorageSync('departments', res.data);
          console.log('[缓存] 科室数据已缓存');
        }
      },
      fail: () => {
        console.log('[缓存] 获取科室数据失败');
      }
    });

    // 医生数据改为按科室/医院按需加载，避免启动时拉取上千条医生数据拖慢首屏
    wx.setStorageSync('cacheUpdateTime', now);
    this.globalData.cacheUpdateTime = now;
    console.log('[缓存] 数据预加载完成');
  },

  getCachedHospitals() {
    return wx.getStorageSync('hospitals') || [];
  },

  getCachedDepartments() {
    return wx.getStorageSync('departments') || [];
  },

  getCachedDoctors() {
    return wx.getStorageSync('doctors') || [];
  },

  savePendingAppointment(appointment) {
    const pendingAppointments = wx.getStorageSync('pendingAppointments') || [];
    pendingAppointments.push({
      ...appointment,
      id: Date.now(),
      createTime: Date.now(),
      status: 'pending'
    });
    wx.setStorageSync('pendingAppointments', pendingAppointments);
    console.log('[离线预约] 已保存待提交预约:', appointment);
  },

  getPendingAppointments() {
    return wx.getStorageSync('pendingAppointments') || [];
  },

  syncPendingAppointments() {
    const pendingAppointments = this.getPendingAppointments();
    if (pendingAppointments.length === 0) {
      return;
    }

    console.log('[同步] 开始同步待提交预约，共', pendingAppointments.length, '条');

    pendingAppointments.forEach((appointment, index) => {
      setTimeout(() => {
        wx.request({
          url: this.globalData.baseUrl + '/api/appointments',
          method: 'POST',
          data: {
            patientId: appointment.patientId,
            scheduleId: appointment.scheduleId,
            appointmentDate: appointment.appointmentDate,
            timeSlot: appointment.timeSlot
          },
          success: () => {
            const remaining = this.getPendingAppointments().filter(a => a.id !== appointment.id);
            wx.setStorageSync('pendingAppointments', remaining);
            console.log('[同步] 预约', appointment.id, '提交成功');
            
            if (remaining.length === 0) {
              wx.showToast({
                title: '所有预约已同步',
                icon: 'success',
                duration: 2000
              });
            }
          },
          fail: () => {
            console.log('[同步] 预约', appointment.id, '提交失败，稍后重试');
          }
        });
      }, index * 1000);
    });
  },

  isOnline() {
    return this.globalData.isOnline;
  }
})
