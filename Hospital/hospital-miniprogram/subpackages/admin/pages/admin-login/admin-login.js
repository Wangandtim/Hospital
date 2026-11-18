const app = getApp();
const config = require('../../../../config/index');

Page({
  data: {
    username: '',
    password: '',
    apiBase: config.apiBase + '/api'
  },

  onUsernameInput(e) {
    this.setData({ username: e.detail.value });
  },

  onPasswordInput(e) {
    this.setData({ password: e.detail.value });
  },

  handleLogin() {
    if (this.loggingIn) {
      return;
    }
    const { username, password } = this.data;

    if (!username || !password) {
      wx.showToast({
        title: '请输入用户名和密码',
        icon: 'none'
      });
      return;
    }

    wx.showLoading({ title: '登录中...' });
    this.loggingIn = true;

    wx.request({
      url: this.data.apiBase + '/admin/login',
      method: 'POST',
      data: {
        username: username,
        password: password
      },
      success: (res) => {
          this.loggingIn = false;
          wx.hideLoading();
          if (res.data.success) {
            wx.setStorageSync('authToken', res.data.token || '');
            wx.setStorageSync('token', res.data.token || '');
            wx.setStorageSync('adminId', res.data.adminId);
            wx.setStorageSync('username', res.data.username);
            wx.setStorageSync('realName', res.data.realName);
            wx.setStorageSync('hospitalId', res.data.hospitalId);
            wx.setStorageSync('hospitalName', res.data.hospitalName);
            wx.setStorageSync('adminRole', res.data.role || 'HOSPITAL_ADMIN');
            wx.setStorageSync('userRole', 'admin');
            app.globalData.userRole = 'admin';
            app.globalData.hospitalId = res.data.hospitalId;
            app.globalData.hospitalName = res.data.hospitalName;
            app.globalData.adminRole = res.data.role || 'HOSPITAL_ADMIN';

            wx.showToast({
              title: '登录成功',
              icon: 'success'
            });

            setTimeout(() => {
              // 根据角色跳转到不同的后台页面
              const role = res.data.role || 'HOSPITAL_ADMIN';
              if (role === 'SUPER_ADMIN') {
                wx.redirectTo({
                  url: '/subpackages/admin/pages/admin-dashboard/admin-dashboard'
                });
              } else {
                wx.redirectTo({
                  url: '/subpackages/admin/pages/hospital-admin-dashboard/hospital-admin-dashboard'
                });
              }
            }, 600);
          } else {
            wx.showToast({
              title: res.data.message || '登录失败',
              icon: 'none',
              duration: 2000
            });
          }
        },
      fail: () => {
        this.loggingIn = false;
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
  },

  goToRegister() {
    wx.navigateTo({
      url: '/subpackages/admin/pages/admin-register/admin-register'
    });
  }
})