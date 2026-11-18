Page({
  data: {
    reason: '存在需要立即就医的症状',
    pattern: '',
    emergencyPhone: '120',
    hospitalName: '',
    hospitalAddress: '',
    hospitalPhone: '',
    hospitalDistanceKm: null,
    hospitalDistanceText: ''
  },

  onLoad(options) {
    const distance = options.hospitalDistanceKm ? Number(options.hospitalDistanceKm) : null;
    this.setData({
      reason: options.reason ? decodeURIComponent(options.reason) : '存在需要立即就医的症状',
      pattern: options.pattern || '',
      emergencyPhone: options.emergencyPhone ? decodeURIComponent(options.emergencyPhone) : '120',
      hospitalName: options.hospitalName ? decodeURIComponent(options.hospitalName) : '',
      hospitalAddress: options.hospitalAddress ? decodeURIComponent(options.hospitalAddress) : '',
      hospitalPhone: options.hospitalPhone ? decodeURIComponent(options.hospitalPhone) : '',
      hospitalDistanceKm: distance,
      hospitalDistanceText: distance !== null ? '（约 ' + distance + ' 公里）' : ''
    });
  },

  call120() {
    wx.makePhoneCall({ phoneNumber: this.data.emergencyPhone || '120', fail: () => { /* 用户取消 */ } });
  },

  callHospital() {
    if (!this.data.hospitalPhone) {
      return;
    }
    wx.makePhoneCall({ phoneNumber: this.data.hospitalPhone, fail: () => { /* 用户取消 */ } });
  },

  goNearby() {
    wx.switchTab({ url: '/pages/nearby/nearby' });
  },

  goHome() {
    wx.switchTab({ url: '/pages/index/index' });
  }
});
