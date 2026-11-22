const app = getApp();

Page({
  data: {
    doctorId: '',
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  },

  onLoad(options) {
    this.setData({
      doctorId: options.doctorId || wx.getStorageSync('doctorId') || ''
    });
    // 未完成改密前不标记为已登录，避免回到登录页时被自动放行
    wx.removeStorageSync('userRole');
    app.globalData.userRole = '';
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [field]: e.detail.value });
  },

  submit() {
    const { doctorId, oldPassword, newPassword, confirmPassword } = this.data;

    if (!doctorId) {
      wx.showToast({ title: '登录信息已失效，请重新登录', icon: 'none' });
      return;
    }
    if (!oldPassword || !newPassword) {
      wx.showToast({ title: '请填写原密码和新密码', icon: 'none' });
      return;
    }
    if (newPassword.length < 6) {
      wx.showToast({ title: '新密码至少 6 位', icon: 'none' });
      return;
    }
    if (newPassword === oldPassword) {
      wx.showToast({ title: '新密码不能与原密码相同', icon: 'none' });
      return;
    }
    if (newPassword !== confirmPassword) {
      wx.showToast({ title: '两次输入的新密码不一致', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '保存中...' });
    wx.request({
      url: app.globalData.baseUrl + '/api/doctor-self/' + doctorId + '/password',
      method: 'PUT',
      data: { oldPassword, newPassword },
      success: (res) => {
        wx.hideLoading();
        if (res.data && res.data.success) {
          wx.setStorageSync('userRole', 'doctor');
          app.globalData.userRole = 'doctor';
          wx.showToast({ title: '密码已修改', icon: 'success' });
          setTimeout(() => {
            wx.redirectTo({ url: '/pages/doctor-workspace/doctor-workspace' });
          }, 800);
        } else {
          wx.showToast({ title: (res.data && res.data.message) || '修改失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '网络异常，请稍后重试', icon: 'none' });
      }
    });
  },

  goBack() {
    wx.redirectTo({ url: '/pages/login/login' });
  }
});
