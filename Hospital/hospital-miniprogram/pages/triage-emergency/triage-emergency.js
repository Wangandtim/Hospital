Page({
  data: {
    reason: '存在需要立即就医的症状',
    pattern: '',
    emergencyPhone: '120',
    hospitalName: '',
    hospitalAddress: '',
    hospitalPhone: '',
    hospitalDistanceKm: null,
    hospitalDistanceText: '',
    steps: [
      { title: '不要自行驾车', desc: '如无人陪同，请拨打 120 并说明地址与症状。' },
      { title: '带上身份证与医保卡', desc: '有既往病历、用药清单请一并带上。' },
      { title: '到急诊分诊台报到', desc: '如实说明胸痛、呼吸困难、意识变化等情况。' },
      { title: '保持电话畅通', desc: '家属可拨 029-8763 0999 询问急诊位置。' }
    ]
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

  goNavigation() {
    wx.navigateTo({ url: '/pages/navigation/navigation' });
  },

  goHome() {
    wx.switchTab({ url: '/pages/index/index' });
  }
});
