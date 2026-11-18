const app = getApp();
const support = require('../../config/support');
const elderly = require('../../utils/elderly');

Page({
  data: {
    elderly: false,
    selectedHospital: null,
    version: '1.0.0',
    supportPhone: support.hotline
  },

  onShow() {
    this.refresh();
  },

  refresh() {
    const selectedHospital = wx.getStorageSync('selectedHospital') || null;
    this.setData({
      elderly: elderly.isElderly(),
      selectedHospital: selectedHospital
    });
  },

  // 长辈模式开关
  onElderlyChange(e) {
    const on = !!e.detail.value;
    elderly.setElderly(on);
    this.setData({ elderly: on });

    if (typeof wx.vibrateShort === 'function') {
      wx.vibrateShort({ type: 'light', fail: () => {} });
    }

    wx.showToast({
      title: on ? '已开启长辈模式' : '已关闭长辈模式',
      icon: 'none',
      duration: 1500
    });
  },

  // 更换默认就诊医院
  goChangeHospital() {
    wx.navigateTo({ url: '/pages/hospital-list/hospital-list' });
  },

  // 清除本地缓存（保留登录态与长辈模式）
  clearCache() {
    wx.showModal({
      title: '清除缓存',
      content: '将清除已缓存的医院、科室、医生数据，不影响登录状态。',
      confirmText: '清除',
      cancelText: '取消',
      success: (res) => {
        if (!res.confirm) return;
        wx.removeStorageSync('hospitals');
        wx.removeStorageSync('departments');
        wx.removeStorageSync('doctors');
        wx.removeStorageSync('cacheUpdateTime');
        if (app.globalData) {
          app.globalData.cacheUpdateTime = null;
        }
        wx.showToast({ title: '缓存已清除', icon: 'success' });
      }
    });
  },

  checkUpdate() {
    if (typeof wx.getUpdateManager !== 'function') {
      wx.showToast({ title: '当前已是最新版本', icon: 'none' });
      return;
    }

    wx.getUpdateManager().checkForUpdate();
    wx.showToast({ title: '正在检查更新', icon: 'none' });
  },

  showAbout() {
    wx.showModal({
      title: support.platformName,
      content: `版本 1.0.0\n客服热线：${support.hotline}\n服务时间：${support.workTime}`,
      showCancel: false,
      confirmText: '知道了'
    });
  },

  // 智能健康助手
  goAgent() {
    wx.navigateTo({ url: '/pages/triage/triage' });
  },

  goHelp() {
    wx.navigateTo({ url: '/pages/help/help' });
  },

  goContact() {
    wx.navigateTo({ url: '/pages/contact/contact' });
  }
});
