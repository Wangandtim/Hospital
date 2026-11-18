const app = getApp();
const config = require('../../../../config/index');

Page({
  data: {
    hospitalList: [],
    filteredHospitals: [],
    selectedHospitalId: '',
    selectedHospitalName: '',
    dailyKey: '',
    searchKeyword: '',
    showPicker: false,
    apiBase: config.apiBase + '/api'
  },

  onLoad() {
    this.loadHospitals();
  },

  loadHospitals() {
    wx.showLoading({ title: '加载中...' });
    wx.request({
      url: this.data.apiBase + '/hospitals',
      method: 'GET',
      success: (res) => {
        wx.hideLoading();
        if (Array.isArray(res.data)) {
          const activeHospitals = res.data.filter(h => h.status === 1);
          this.setData({
            hospitalList: activeHospitals,
            filteredHospitals: activeHospitals
          });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({
          title: '加载医院列表失败',
          icon: 'none'
        });
      }
    });
  },

  showHospitalPicker() {
    this.setData({ showPicker: true });
  },

  hidePicker() {
    this.setData({ showPicker: false });
  },

  stopPropagation() {},

  onHospitalSearch(e) {
    const keyword = e.detail.value;
    this.setData({ searchKeyword: keyword });
    
    if (!keyword) {
      this.setData({ filteredHospitals: this.data.hospitalList });
      return;
    }

    const filtered = this.data.hospitalList.filter(h =>
      (h.hospitalName || '').includes(keyword) || (h.address || '').includes(keyword)
    );
    this.setData({ filteredHospitals: filtered });
  },

  selectHospital(e) {
    const hospitalId = e.currentTarget.dataset.hospitalId;
    const hospitalName = e.currentTarget.dataset.hospitalName;
    
    this.setData({
      selectedHospitalId: hospitalId,
      selectedHospitalName: hospitalName,
      showPicker: false
    });
  },

  onKeyInput(e) {
    this.setData({ dailyKey: e.detail.value });
  },

  handleLogin() {
    if (!this.data.selectedHospitalId) {
      wx.showToast({
        title: '请选择医院',
        icon: 'none'
      });
      return;
    }

    if (!this.data.dailyKey) {
      wx.showToast({
        title: '请输入密钥',
        icon: 'none'
      });
      return;
    }

    wx.showLoading({ title: '登录中...' });

    wx.request({
      url: this.data.apiBase + '/emergency/login',
      method: 'POST',
      data: {
        hospitalId: this.data.selectedHospitalId,
        dailyKey: this.data.dailyKey
      },
      success: (res) => {
        wx.hideLoading();
        
        if (res.data && res.data.success) {
          // 清理旧身份，避免普通管理员/医生令牌抢占应急令牌
          wx.removeStorageSync('token');
          wx.removeStorageSync('authToken');
          wx.removeStorageSync('userRole');
          wx.removeStorageSync('adminRole');
          wx.removeStorageSync('refreshToken');
          
          wx.setStorageSync('emergencyToken', res.data.token);
          wx.setStorageSync('token', res.data.token);
          wx.setStorageSync('userRole', 'emergency');
          wx.setStorageSync('hospitalId', res.data.hospitalId);
          wx.setStorageSync('hospitalName', res.data.hospitalName);
          wx.setStorageSync('expireTime', res.data.expireTime);
          
          app.globalData.token = res.data.token;
          app.globalData.userRole = 'emergency';
          app.globalData.hospitalId = res.data.hospitalId;
          app.globalData.hospitalName = res.data.hospitalName;

          wx.showToast({
            title: '登录成功',
            icon: 'success'
          });

          setTimeout(() => {
            wx.redirectTo({
              url: '/subpackages/admin/pages/emergency-department/emergency-department'
            });
          }, 1200);
        } else {
          wx.showToast({
            title: (res.data && res.data.message) || '登录失败',
            icon: 'none',
            duration: 2000
          });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({
          title: '网络请求失败',
          icon: 'none',
          duration: 2000
        });
      }
    });
  },

  goBack() {
    wx.redirectTo({
      url: '/pages/login/login'
    });
  }
})