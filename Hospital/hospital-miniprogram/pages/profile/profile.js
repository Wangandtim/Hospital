const app = getApp();
const elderly = require('../../utils/elderly');

Page({
  data: {
    userInfo: null,
    hasLogin: false,
    hasPatientInfo: false,
    userRole: null,
    displayName: '',
    userId: '',
    elderly: false
  },

  onLoad() {
    this.checkLoginStatus();
    this.syncElderlyMode();
  },

  onShow() {
    this.checkLoginStatus();
    this.syncElderlyMode();
  },

  syncElderlyMode() {
    this.setData({ elderly: elderly.isElderly() });
  },

  checkLoginStatus() {
    const hasLogin = !!app.globalData.token;
    const userRole = app.globalData.userRole;
    const userInfo = wx.getStorageSync('userInfo');
    const openid = app.globalData.openid;
    const patientName = wx.getStorageSync('patientName');
    const hasCompleteInfo = wx.getStorageSync('hasCompleteInfo');
    
    let displayName = '';
    let userId = '';
    
    if (hasLogin) {
      if (userRole === 'patient') {
        if (patientName && patientName.trim() !== '' && patientName !== '患者用户') {
          displayName = patientName;
        } else {
          displayName = '患者用户';
        }
      } else if (userRole === 'doctor') {
        displayName = '王医生';
      } else if (userRole === 'admin') {
        displayName = '管理员';
      }
      
      if (openid) {
        userId = openid.substring(0, 8) + '...';
      }
    }

    this.setData({
      hasLogin: hasLogin,
      hasPatientInfo: hasCompleteInfo,
      userRole: userRole,
      userInfo: userInfo,
      displayName: displayName,
      userId: userId
    });
  },

  handleLogout() {
    wx.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          wx.removeStorageSync('token');
          wx.removeStorageSync('authToken');
          wx.removeStorageSync('openid');
          wx.removeStorageSync('hasPatientInfo');
          wx.removeStorageSync('userInfo');
          wx.removeStorageSync('userRole');
          app.globalData.token = null;
          app.globalData.openid = null;
          app.globalData.hasPatientInfo = false;
          app.globalData.userInfo = null;
          app.globalData.userRole = null;
          
          this.checkLoginStatus();
          wx.showToast({
            title: '已退出登录',
            icon: 'success'
          });
          
          setTimeout(() => {
            wx.reLaunch({
              url: '/pages/login/login'
            });
          }, 1000);
        }
      }
    });
  },

  handleLogin() {
    wx.navigateTo({
      url: '/pages/login/login'
    });
  },

  goToMyAppointments() {
    if (!this.checkLogin()) return;
    wx.switchTab({
      url: '/pages/my-appointments/my-appointments'
    });
  },

  bindPatientInfo() {
    if (!this.checkLogin()) return;
    
    wx.navigateTo({
      url: '/pages/profile/bind-patient'
    });
  },

  viewPatientInfo() {
    if (!this.checkLogin()) return;
    
    wx.navigateTo({
      url: '/pages/patient-info/patient-info?mode=view'
    });
  },

  checkLogin() {
    
    if (!this.data.hasPatientInfo) {
      wx.showToast({
        title: '请先绑定患者信息',
        icon: 'none'
      });
      return;
    }
    
    wx.navigateTo({
      url: '/pages/profile/patient-info'
    });
  },

  goToDoctorWorkspace() {
    if (!this.checkLogin()) return;
    wx.redirectTo({
      url: '/pages/doctor-workspace/doctor-workspace'
    });
  },

  goToMyPatients() {
    if (!this.checkLogin()) return;
    wx.navigateTo({
      url: '/pages/patient-list/patient-list'
    });
  },

  goToAdminDashboard() {
    if (!this.checkLogin()) return;
    wx.redirectTo({
      url: '/subpackages/admin/pages/admin-dashboard/admin-dashboard'
    });
  },

  goToDoctorManage() {
    if (!this.checkLogin()) return;
    wx.navigateTo({
      url: '/pages/doctor-manage/doctor-manage'
    });
  },

  goToSettings() {
    wx.navigateTo({ url: '/pages/settings/settings' });
  },

  goToHelpCenter() {
    wx.navigateTo({ url: '/pages/help/help' });
  },

  goToContact() {
    wx.navigateTo({ url: '/pages/contact/contact' });
  },

  checkLogin() {
    if (!this.data.hasLogin) {
      wx.showModal({
        title: '提示',
        content: '请先登录',
        showCancel: false,
        success: () => {
          this.handleLogin();
        }
      });
      return false;
    }
    return true;
  }
})
