const app = getApp();

Page({
  data: {
    patients: [],
    searchKeyword: '',
    filteredPatients: []
  },

  onLoad() {
    const role = wx.getStorageSync('userRole');
    if (role !== 'doctor') {
      wx.redirectTo({
        url: '/pages/login/login'
      });
      return;
    }
    
    this.loadPatientData();
  },

  onShow() {
    this.loadPatientData();
  },

  loadPatientData() {
    const doctorId = wx.getStorageSync('doctorId');
    
    wx.showLoading({ title: '加载中...' });
    
    wx.request({
      url: app.globalData.baseUrl + '/api/doctor/' + doctorId + '/patients',
      method: 'GET',
      success: (res) => {
        wx.hideLoading();
        
        if (res.statusCode === 200 && res.data.success) {
          this.setData({
            patients: res.data.patients || [],
            filteredPatients: res.data.patients || []
          });
        } else {
          wx.showToast({
            title: res.data.message || '加载数据失败',
            icon: 'none'
          });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({
          title: '网络错误',
          icon: 'none'
        });
      }
    });
  },

  onSearchInput(e) {
    const keyword = e.detail.value.toLowerCase();
    this.setData({ searchKeyword: keyword });
    
    if (!keyword) {
      this.setData({ filteredPatients: this.data.patients });
      return;
    }
    
    const filtered = this.data.patients.filter(patient => 
      patient.name.toLowerCase().includes(keyword)
    );
    this.setData({ filteredPatients: filtered });
  },

  viewPatientDetail(e) {
    const patientId = e.currentTarget.dataset.id;
    wx.showToast({
      title: '患者详情开发中',
      icon: 'none'
    });
  },

  getGenderText(gender) {
    return gender === 'M' ? '男' : gender === 'F' ? '女' : '未知';
  }
})
