const app = getApp();

Page({
  data: {
    patientInfo: null
  },

  onLoad() {
    this.loadPatientInfo();
  },

  onShow() {
    this.loadPatientInfo();
  },

  loadPatientInfo() {
    const patientInfo = wx.getStorageSync('patientInfo');
    this.setData({ patientInfo: patientInfo });
  },

  editPatientInfo() {
    wx.navigateTo({
      url: '/pages/profile/bind-patient'
    });
  },

  goBack() {
    wx.navigateBack();
  }
});
