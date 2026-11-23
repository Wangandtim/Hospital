const app = getApp();
const elderly = require('../../utils/elderly');
const placeholder = require('../../utils/placeholder');
const pager = require('../../utils/pager');
const deptIcon = require('../../utils/dept-icon');

Page({
  data: {
    hospitals: [],
    nearbyHospital: null,
    articles: [],
    departments: [],
    doctors: [],
    doctorsLoaded: false,
    selectedHospital: null,
    elderly: false,
    latitude: 34.23,
    longitude: 108.95,
    location: '点击重新定位',
    isLoading: false,
    searchKeyword: '',
    searchType: 'hospital',
    searchTypeIndex: 0,
    searchTypes: [
      { name: '医院', value: 'hospital' },
      { name: '科室', value: 'department' },
      { name: '医生', value: 'doctor' }
    ],
    showSearchResult: false,
    searchResult: [],
    homeSceneImage: '/assets/home/hospital-scene.jpg'
  },

  onLoad() {
    this.setData({ isLoading: true, elderly: elderly.isElderly() });
    
    const selectedHospital = wx.getStorageSync('selectedHospital');
    if (selectedHospital) {
      this.setData({ selectedHospital: selectedHospital });
      app.globalData.selectedHospital = selectedHospital;
    }
    
    this.loadLocationAndData();
  },

  onShow() {
    this.setData({ elderly: elderly.isElderly() });
    const selectedHospital = wx.getStorageSync('selectedHospital');
    if (selectedHospital && JSON.stringify(selectedHospital) !== JSON.stringify(this.data.selectedHospital)) {
      this.setData({ selectedHospital: selectedHospital });
      app.globalData.selectedHospital = selectedHospital;
      this.loadLocationAndData();
    }
  },

  loadLocationAndData() {
    // 先用缓存位置立即渲染，定位改为后台静默刷新，避免进入首页时被“定位中”阻塞
    const cached = wx.getStorageSync('lastLocation');
    const lat = cached && typeof cached.latitude === 'number' ? cached.latitude : 34.23;
    const lng = cached && typeof cached.longitude === 'number' ? cached.longitude : 108.95;

    this.setData({
      latitude: lat,
      longitude: lng,
      location: cached ? '已定位' : '已定位到西安'
    });

    this.loadHospitalData(lat, lng);
    this.refreshLocationQuietly();
  },

  refreshLocationQuietly() {
    wx.getLocation({
      type: 'gcj02',
      isHighAccuracy: false,
      success: (res) => {
        wx.setStorageSync('lastLocation', { latitude: res.latitude, longitude: res.longitude });
        const moved = Math.abs(res.latitude - this.data.latitude) > 0.0001 ||
          Math.abs(res.longitude - this.data.longitude) > 0.0001;
        if (!moved) return;

        this.setData({
          latitude: res.latitude,
          longitude: res.longitude,
          location: '已定位'
        });
        if (this.data.hospitals && this.data.hospitals.length) {
          this.processHospitalData(this.data.hospitals, res.latitude, res.longitude, true);
        }
      },
      fail: () => {
        if (this._locationDeniedTipShown) return;
        this._locationDeniedTipShown = true;
        wx.showToast({ title: '未获取定位，已展示默认城市医院', icon: 'none', duration: 2500 });
      }
    });
  },

  loadHospitalData(lat, lng) {
    if (this.data.selectedHospital && this.data.selectedHospital.hospitalId) {
      this.loadSelectedHospitalDetail(lat, lng);
      return;
    }
    
    let url = app.globalData.baseUrl + '/api/hospitals';
    let useFilter = false;
    
    if (this.data.selectedHospital && this.data.selectedHospital.province && this.data.selectedHospital.city) {
      url += '/filter?province=' + encodeURIComponent(this.data.selectedHospital.province) +
             '&city=' + encodeURIComponent(this.data.selectedHospital.city) +
             '&district=' + encodeURIComponent(this.data.selectedHospital.district || '');
      useFilter = true;
    }
    
    
    wx.request({
      url: url,
      method: 'GET',
      timeout: 3000,
      success: (res) => {
        let hospitals = res.data;
        if (res.data && res.data.value && Array.isArray(res.data.value)) {
          hospitals = res.data.value;
        }
        if (Array.isArray(hospitals)) {
          
          if (hospitals.length === 0 && useFilter) {
            this.loadAllHospitals(lat, lng);
            return;
          }
          
          wx.setStorageSync('hospitals', hospitals);
          wx.setStorageSync('cacheUpdateTime', Date.now());
          this.processHospitalData(hospitals, lat, lng);
          wx.hideLoading();
          wx.showToast({ title: '在线模式', icon: 'success', duration: 2000 });
        } else {
          wx.hideLoading();
          this.loadFromCache(lat, lng);
        }
      },
      fail: (err) => {
        console.error('[医院] 请求失败:', err);
        wx.hideLoading();
        this.loadFromCache(lat, lng);
      }
    });
  },
  
  loadSelectedHospitalDetail(lat, lng) {
    const hospitalId = this.data.selectedHospital.hospitalId;
    
    wx.request({
      url: app.globalData.baseUrl + '/api/hospitals/' + hospitalId,
      method: 'GET',
      timeout: 5000,
      success: (res) => {
        if (res.data) {
          const selectedHospitalDetail = res.data;
          this.setData({
            isLoading: false,
            nearbyHospital: this.decorateHospital({
              ...selectedHospitalDetail,
              distance: 0,
              distanceText: '已选择'
            })
          });
          
          this.loadHomeAggregate(selectedHospitalDetail);
          wx.hideLoading();
          wx.showToast({ title: '已切换到 ' + selectedHospitalDetail.hospitalName, icon: 'success', duration: 2000 });
        } else {
          wx.hideLoading();
          this.loadAllHospitals(lat, lng);
        }
      },
      fail: (err) => {
        console.error('[医院] 获取选中医院详情失败:', err);
        wx.hideLoading();
        wx.showToast({ title: '网络超时，使用缓存数据', icon: 'none', duration: 2000 });
        this.loadAllHospitals(lat, lng);
      }
    });
  },

  loadAllHospitals(lat, lng) {
    const url = app.globalData.baseUrl + '/api/hospitals';
    
    wx.request({
      url: url,
      method: 'GET',
      timeout: 3000,
      success: (res) => {
        let hospitals = res.data;
        if (res.data && res.data.value && Array.isArray(res.data.value)) {
          hospitals = res.data.value;
        }
        if (Array.isArray(hospitals)) {
          wx.setStorageSync('hospitals', hospitals);
          wx.setStorageSync('cacheUpdateTime', Date.now());
          this.processHospitalData(hospitals, lat, lng);
          wx.hideLoading();
        } else {
          wx.hideLoading();
          this.loadFromCache(lat, lng);
        }
      },
      fail: (err) => {
        console.error('[医院] 获取全部失败:', err);
        wx.hideLoading();
        this.loadFromCache(lat, lng);
      }
    });
  },

  loadFromCache(lat, lng) {
    const cachedHospitals = wx.getStorageSync('hospitals') || [];
    if (cachedHospitals.length === 0) {
      this.setData({ isLoading: false });
      wx.hideLoading();
      wx.showToast({ title: '暂无数据', icon: 'none' });
      return;
    }
    this.processHospitalData(cachedHospitals, lat, lng);
    wx.hideLoading();
    wx.showToast({ title: '使用缓存数据', icon: 'none', duration: 2000 });
  },

  processHospitalData(hospitals, lat, lng, silent) {
    const hospitalsWithDistance = hospitals.map(hospital => {
      const distance = this.calculateDistance(lat, lng, hospital.latitude, hospital.longitude);
      return {
        ...hospital,
        distance: distance,
        distanceText: this.formatDistance(distance)
      };
    });

    hospitalsWithDistance.sort((a, b) => a.distance - b.distance);

    let currentHospital = null;
    
    if (this.data.selectedHospital) {
      const selected = hospitalsWithDistance.find(h => h.hospitalId === this.data.selectedHospital.hospitalId);
      currentHospital = selected || hospitalsWithDistance[0] || null;
    } else {
      currentHospital = hospitalsWithDistance[0] || null;
    }

    this.setData({
      hospitals: hospitalsWithDistance,
      nearbyHospital: currentHospital ? this.decorateHospital(currentHospital) : null,
      isLoading: false
    });

    if (!silent) {
      this.loadHomeAggregate(currentHospital);
    }
  },

  // 首页优先走聚合接口，减少首屏并发请求；接口异常时回退到分接口
  loadHomeAggregate(hospital) {
    const hospitalId = hospital && hospital.hospitalId;
    if (!hospitalId) {
      this.loadDepartments();
      this.loadDoctors();
      this.loadHospitalArticles();
      return;
    }

    wx.request({
      url: app.globalData.baseUrl + '/api/home?hospitalId=' + hospitalId,
      method: 'GET',
      timeout: 5000,
      success: (res) => {
        const data = res.data && res.data.data ? res.data.data : (res.data || {});
        if (!data.hospital) {
          this.loadDepartments();
          this.loadDoctors();
          this.loadHospitalArticles();
          return;
        }

        const current = this.decorateHospital({
          ...data.hospital,
          distance: hospital.distance,
          distanceText: hospital.distanceText,
          notice: data.notice
        });
        const departments = this.decorateDepartments(data.recommendedDepartments || []);
        const doctors = (data.recommendedDoctors || []).slice(0, 4).map(d => this.decorateDoctor(d));

        this.setData({
          nearbyHospital: current,
          departments: departments,
          doctors: doctors,
          doctorsLoaded: true,
          isLoading: false
        });

        this.loadHospitalArticles();
        if (departments.length === 0) this.loadDepartments();
        if (doctors.length === 0) this.loadDoctors();
      },
      fail: () => {
        this.loadDepartments();
        this.loadDoctors();
        this.loadHospitalArticles();
      }
    });
  },

  // 医院动态：读取当前医院管理员发布的文章/推文/通知，未发布则整块隐藏
  loadHospitalArticles() {
    let hospitalId = null;
    if (this.data.selectedHospital && this.data.selectedHospital.hospitalId) {
      hospitalId = this.data.selectedHospital.hospitalId;
    } else if (this.data.nearbyHospital && this.data.nearbyHospital.hospitalId) {
      hospitalId = this.data.nearbyHospital.hospitalId;
    }
    if (!hospitalId) {
      this.setData({ articles: [] });
      return;
    }

    wx.request({
      url: app.globalData.baseUrl + '/api/hospital-articles?hospitalId=' + hospitalId,
      method: 'GET',
      timeout: 4000,
      success: (res) => {
        const list = Array.isArray(res.data) ? res.data : [];
        const articles = list.slice(0, 3).map(item => ({
          ...item,
          coverFull: item.coverUrl ? app.globalData.baseUrl + item.coverUrl : '',
          dateText: (item.createTime || '').slice(0, 10),
          brief: item.summary || (item.content || '').replace(/\s+/g, ' ').slice(0, 40)
        }));
        this.setData({ articles });
      },
      fail: () => {
        this.setData({ articles: [] });
      }
    });
  },

  goToArticleDetail(e) {
    const id = e.currentTarget.dataset.id;
    if (!id) return;
    wx.navigateTo({ url: `/pages/article-detail/article-detail?id=${id}` });
  },

  goNoticeList() {
    const hospital = this.data.selectedHospital || this.data.nearbyHospital;
    let url = '/pages/notice/notice';
    if (hospital && hospital.hospitalId) {
      url += '?hospitalId=' + hospital.hospitalId +
        '&hospitalName=' + encodeURIComponent(hospital.hospitalName || '');
    }
    wx.navigateTo({ url });
  },

  // 重点专科文案：国家 / 省级 / 市级，未分级时退回通用说法
  flagshipText(level, isFlagship) {
    const n = Number(level) || 0;
    if (n === 1) return '国家临床重点专科';
    if (n === 2) return '省级临床重点专科';
    if (n === 3) return '市级临床重点专科';
    return Number(isFlagship) === 1 ? '临床重点专科' : '';
  },

  loadDepartments() {
    let url = app.globalData.baseUrl + '/api/departments';
    let targetHospitalId = null;
    
    if (this.data.selectedHospital && this.data.selectedHospital.hospitalId) {
      targetHospitalId = this.data.selectedHospital.hospitalId;
    } else if (this.data.nearbyHospital && this.data.nearbyHospital.hospitalId) {
      targetHospitalId = this.data.nearbyHospital.hospitalId;
    }
    
    if (targetHospitalId) {
      url += '/hospital/' + targetHospitalId;
    }
    
    
    wx.request({
      url: url,
      method: 'GET',
      timeout: 3000,
      success: (res) => {
        let departments = res.data;
        if (res.data && res.data.value && Array.isArray(res.data.value)) {
          departments = res.data.value;
        }
        if (Array.isArray(departments)) {
          wx.setStorageSync('departments', departments);
          this.setData({ departments: this.decorateDepartments(departments) });
        } else {
          this.loadDepartmentsFromCache(targetHospitalId);
        }
      },
      fail: (err) => {
        console.error('[科室] 请求失败:', err);
        this.loadDepartmentsFromCache(targetHospitalId);
      }
    });
  },

  loadDepartmentsFromCache(targetHospitalId) {
    const cachedDepts = wx.getStorageSync('departments') || [];
    let filteredDepts = cachedDepts;
    if (targetHospitalId) {
      filteredDepts = cachedDepts.filter(d => d.hospitalId === targetHospitalId);
    }
    this.setData({ departments: this.decorateDepartments(filteredDepts) });
  },

  decorateDepartments(list) {
    return (list || []).slice(0, 6).map(d => ({
      ...d,
      ...placeholder.departmentExtra(d),
      initial: String(d.deptName || '科').slice(0, 1),
      deptIcon: deptIcon.iconPath(d.deptName),
      flagshipText: this.flagshipText(d.flagshipLevel, d.isFlagship)
    }));
  },

  decorateHospital(hospital) {
    if (!hospital) return hospital;
    const extra = placeholder.hospitalExtra(hospital);
    return {
      ...hospital,
      ...extra,
      // 公告只认后端返回值，避免被 placeholder 的空值覆盖
      notice: hospital.notice || null,
      addressText: hospital.address || (hospital.city ? hospital.city + ' · 主院区' : '院内导航可查')
    };
  },

  loadDoctors() {
    let targetHospitalId = null;
    
    if (this.data.selectedHospital && this.data.selectedHospital.hospitalId) {
      targetHospitalId = this.data.selectedHospital.hospitalId;
    } else if (this.data.nearbyHospital && this.data.nearbyHospital.hospitalId) {
      targetHospitalId = this.data.nearbyHospital.hospitalId;
    } else if (this.data.hospitals.length > 0) {
      targetHospitalId = this.data.hospitals[0].hospitalId;
    }
    
    if (!targetHospitalId) {
      targetHospitalId = 'H001';
    }
    
    const url = app.globalData.baseUrl + '/api/doctors/hospital/' + targetHospitalId;

    this.setData({ doctorsLoaded: false });
    
    wx.request({
      url: url,
      method: 'GET',
      timeout: 3000,
      success: (res) => {
        let doctors = res.data;
        if (res.data && res.data.value && Array.isArray(res.data.value)) {
          doctors = res.data.value;
        }
        if (Array.isArray(doctors)) {
          wx.setStorageSync('doctors', doctors);
          this.filterAvailableDoctors(doctors, targetHospitalId);
        } else {
          this.loadDoctorsFromCache(targetHospitalId);
        }
      },
      fail: (err) => {
        console.error('[医生] 请求失败:', err);
        this.loadDoctorsFromCache(targetHospitalId);
      }
    });
  },

  // 推荐医生只保留今日有排班（且未停诊）的医生；全部无排班时列表为空，由页面展示占位提示
  filterAvailableDoctors(doctors, hospitalId) {
    if (!hospitalId || !doctors || doctors.length === 0) {
      this.setData({ doctors: [], doctorsLoaded: true });
      return;
    }

    const now = new Date();
    const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;

    wx.request({
      url: app.globalData.baseUrl + '/api/schedules/hospital/' + hospitalId + '/date/' + today +
        '/page?page=0&size=100',
      method: 'GET',
      timeout: 3000,
      success: (res) => {
        const list = res.data && res.data.success
          ? (res.data.data || [])
          : (Array.isArray(res.data) ? res.data : []);

        const availableIds = {};
        list.forEach(item => {
          if (!item || !item.doctorId) return;
          const status = item.status;
          if (status === undefined || status === null || status === 1 || status === 'available') {
            availableIds[item.doctorId] = true;
          }
        });

        const available = doctors.filter(d => availableIds[d.doctorId]);
        this.setData({ doctors: available.slice(0, 4).map(d => this.decorateDoctor(d)), doctorsLoaded: true });
      },
      fail: () => {
        // 排班接口异常时保留原有医生展示，避免整块内容消失
        this.setData({ doctors: doctors.slice(0, 4).map(d => this.decorateDoctor(d)), doctorsLoaded: true });
      }
    });
  },

  decorateDoctor(doctor) {
    if (!doctor) return doctor;
    return {
      ...doctor,
      ...placeholder.doctorExtra(doctor),
      avatarText: String(doctor.name || '医').slice(0, 1),
      photoUrlFull: doctor.photoUrl
        ? (String(doctor.photoUrl).indexOf('http') === 0 ? doctor.photoUrl : app.globalData.baseUrl + doctor.photoUrl)
        : ''
    };
  },

  loadDoctorsFromCache(targetHospitalId) {
    const cachedDoctors = wx.getStorageSync('doctors') || [];
    const filteredDoctors = cachedDoctors.filter(d => d.hospitalId === targetHospitalId);
    this.filterAvailableDoctors(filteredDoctors, targetHospitalId);
  },

  calculateDistance(lat1, lon1, lat2, lon2) {
    const R = 6371000;
    const phi1 = Math.PI * lat1 / 180;
    const phi2 = Math.PI * lat2 / 180;
    const deltaPhi = Math.PI * (lat2 - lat1) / 180;
    const deltaLambda = Math.PI * (lon2 - lon1) / 180;

    const a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
              Math.cos(phi1) * Math.cos(phi2) *
              Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return Math.round(R * c);
  },

  formatDistance(distance) {
    if (distance < 1000) {
      return distance + '米';
    } else {
      return (distance / 1000).toFixed(1) + '公里';
    }
  },

  onSearchTypeChange(e) {
    this.setData({
      searchTypeIndex: e.detail.value,
      searchType: this.data.searchTypes[e.detail.value].value
    });
  },

  onSearchInput(e) {
    this.setData({ searchKeyword: e.detail.value });
  },

  performSearch() {
    const keyword = this.data.searchKeyword.trim();
    if (!keyword) {
      this.setData({ showSearchResult: false });
      return;
    }

    let results = [];
    const hit = (value, keyword) => String(value || '').toLowerCase().includes(keyword.toLowerCase());
    if (this.data.searchType === 'hospital') {
      results = this.data.hospitals.filter(h =>
        hit(h.hospitalName, keyword) || hit(h.address, keyword)
      );
    } else if (this.data.searchType === 'department') {
      results = this.data.departments.filter(d =>
        hit(d.deptName, keyword) || hit(d.diseases, keyword)
      );
    } else if (this.data.searchType === 'doctor') {
      results = this.data.doctors.filter(d =>
        hit(d.name, keyword) || hit(d.expertise, keyword)
      );
    }

    this.setData({ searchResult: results, showSearchResult: true });
  },

  selectResult(e) {
    const item = e.currentTarget.dataset.item;
    if (this.data.searchType === 'hospital') {
      wx.navigateTo({ url: `/pages/department/department?hospitalId=${item.hospitalId}` });
    } else if (this.data.searchType === 'doctor') {
      wx.navigateTo({ url: `/pages/doctor-detail/doctor-detail?id=${item.doctorId}` });
    }
    this.setData({ showSearchResult: false });
  },

  closeSearchResult() {
    this.setData({ showSearchResult: false });
  },

  goToNearby() {
    // 周边是 tabBar 页面，只能用 switchTab 打开
    wx.switchTab({ url: '/pages/nearby/nearby' });
  },

  goToProfile() {
    wx.switchTab({ url: '/pages/profile/profile' });
  },

  focusSearch() {
    wx.navigateTo({ url: '/pages/search-result/search-result' });
  },

  viewDept(e) {
    const deptId = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/department-detail/department-detail?id=${deptId}` });
  },

  viewDoctor(e) {
    const doctorId = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/doctor-detail/doctor-detail?id=${doctorId}` });
  },

  makeAppointment(e) {
    if (typeof wx.vibrateShort === 'function') {
      wx.vibrateShort({ type: 'light', fail: () => {} });
    }
    const doctorId = e.currentTarget.dataset.id;
    let hospitalId = null;
    if (this.data.selectedHospital) {
      hospitalId = this.data.selectedHospital.hospitalId;
    } else if (this.data.nearbyHospital) {
      hospitalId = this.data.nearbyHospital.hospitalId;
    }
    
    let url = `/pages/appointment/appointment?doctorId=${doctorId}`;
    if (hospitalId) {
      url += `&hospitalId=${hospitalId}`;
    }
    wx.navigateTo({ url: url });
},

  goToDepartmentList() {
    let hospitalId = null;
    if (this.data.selectedHospital) {
      hospitalId = this.data.selectedHospital.hospitalId;
    } else if (this.data.nearbyHospital) {
      hospitalId = this.data.nearbyHospital.hospitalId;
    } else if (this.data.hospitals.length > 0) {
      hospitalId = this.data.hospitals[0].hospitalId;
    }
    
    if (hospitalId) {
      wx.navigateTo({ url: `/pages/department/department?hospitalId=${hospitalId}` });
    } else {
      wx.showToast({
        title: '请先选择医院',
        icon: 'none'
      });
    }
  },

  goElderVoice() {
    wx.navigateTo({
      url: '/pages/triage/triage?voice=1&mode=ELDER'
    });
  },

  goElderBooking() {
    wx.navigateTo({
      url: '/pages/triage/triage?voice=1&mode=ELDER'
    });
  },

  goTriage() {
    wx.navigateTo({ url: '/pages/triage/triage' });
  },

  goMyAppointments() {
    wx.switchTab({ url: '/pages/my-appointments/my-appointments' });
  },

  goToDoctorList() {
    let hospitalId = null;
    if (this.data.selectedHospital) {
      hospitalId = this.data.selectedHospital.hospitalId;
    } else if (this.data.nearbyHospital) {
      hospitalId = this.data.nearbyHospital.hospitalId;
    } else if (this.data.hospitals.length > 0) {
      hospitalId = this.data.hospitals[0].hospitalId;
    }
    
    if (hospitalId) {
      wx.navigateTo({ url: `/pages/doctor/doctor?hospitalId=${hospitalId}` });
    } else {
      wx.showToast({
        title: '请先选择医院',
        icon: 'none'
      });
    }
  },

  // 点击当前医院卡片：进入该医院的科室列表（患者端页面）
  goToHospitalDeptList() {
    const hospitalId = this.data.nearbyHospital && this.data.nearbyHospital.hospitalId;
    if (!hospitalId) return;
    wx.navigateTo({
      url: '/pages/department/department?hospitalId=' + hospitalId
    });
  },

  goToHospitalList() {    wx.navigateTo({ url: '/pages/hospital-list/hospital-list' });
  },

  refreshLocation() {
    this.setData({ isLoading: true });
    this.loadLocationAndData();
  },

  onPullDownRefresh() {
    this.loadLocationAndData();
    setTimeout(() => { wx.stopPullDownRefresh(); }, 2000);
  }
});
