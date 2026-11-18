const app = getApp();

Page({
  data: {
    departments: [],
    currentHospital: null,
    hospitalId: null
  },

  onLoad(options) {
    console.log('科室列表页面加载，参数:', options);
    if (options.hospitalId) {
      this.setData({ hospitalId: options.hospitalId });
      this.loadDepartments(options.hospitalId);
    } else {
      wx.showToast({
        title: '未指定医院',
        icon: 'none'
      });
    }
  },

  loadDepartments(hospitalId) {
    console.log('加载科室，医院ID:', hospitalId);
    wx.showLoading({ title: '加载中...' });
    
    wx.request({
      url: app.globalData.baseUrl + '/api/departments/hospital/' + hospitalId + '?bookable=true',
      method: 'GET',
      success: (res) => {
        wx.hideLoading();
        console.log('获取科室成功:', res.data);
        if (res.data && Array.isArray(res.data)) {
          this.setData({ departments: res.data });
          
          // 获取医院信息
          wx.request({
            url: app.globalData.baseUrl + '/api/hospitals/' + hospitalId,
            method: 'GET',
            success: (hospitalRes) => {
              if (hospitalRes.data) {
                this.setData({ currentHospital: hospitalRes.data });
              }
            }
          });
        }
      },
      fail: (err) => {
        wx.hideLoading();
        console.error('获取科室失败:', err);
        wx.showToast({
          title: '获取科室失败',
          icon: 'none'
        });
      }
    });
  },

  viewDept(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/department-detail/department-detail?id=${id}`
    });
  },

  viewDoctorList(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/doctor/doctor?deptId=${id}&hospitalId=${this.data.hospitalId}`
    });
  }
})