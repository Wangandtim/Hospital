const app = getApp();

Page({
  data: {
    hospitalId: '',
    hospitalName: '',
    hospitalLevel: '',
    hospitalPhone: '',
    hospitalAddress: '',
    deptCount: 0,
    doctorCount: 0,
    scheduleCount: 0,
    auditCount: 0
  },

  onLoad() {
    this.loadAdminInfo();
  },

  loadAdminInfo() {
    const hospitalId = wx.getStorageSync('hospitalId') || app.globalData.hospitalId;
    const hospitalName = wx.getStorageSync('hospitalName') || app.globalData.hospitalName || '医院管理';
    
    this.setData({
      hospitalId: hospitalId,
      hospitalName: hospitalName
    });

    if (!hospitalId) {
      wx.showToast({ title: '未获取到医院信息', icon: 'none', duration: 2000 });
      return;
    }

    wx.request({
      url: app.globalData.baseUrl + '/api/hospitals/' + hospitalId,
      method: 'GET',
      success: (res) => {
        if (res.data) {
          this.setData({
            hospitalLevel: res.data.level || '',
            hospitalPhone: res.data.phone || '',
            hospitalAddress: res.data.address || ''
          });
        }
      },
      complete: () => {
        this.loadStatistics();
      }
    });
  },

  loadStatistics() {
    const hospitalId = this.data.hospitalId;
    
    // 获取科室数量
    wx.request({
      url: app.globalData.baseUrl + '/api/departments/hospital/' + hospitalId,
      method: 'GET',
      success: (res) => {
        if (res.data && Array.isArray(res.data)) {
          this.setData({ deptCount: res.data.length });
        }
      }
    });

    // 获取医生数量
    wx.request({
      url: app.globalData.baseUrl + '/api/doctors/hospital/' + hospitalId,
      method: 'GET',
      success: (res) => {
        if (res.data && Array.isArray(res.data)) {
          this.setData({ doctorCount: res.data.length });
        }
      }
    });

    // 获取今日排班数量
    const today = new Date().toISOString().split('T')[0];
    wx.request({
      url: app.globalData.baseUrl + '/api/schedules/hospital/' + hospitalId + '/date/' + today,
      method: 'GET',
      success: (res) => {
        let schedules = [];
        if (res.data && Array.isArray(res.data)) {
          schedules = res.data;
        } else if (res.data && res.data.success && Array.isArray(res.data.data)) {
          schedules = res.data.data;
        }
        this.setData({ scheduleCount: schedules.length });
      }
    });

    // 获取待审核医生数量（当前医生注册即启用，统计停用状态的医生作为待处理项）
    wx.request({
      url: app.globalData.baseUrl + '/api/doctors/hospital/' + hospitalId,
      method: 'GET',
      success: (res) => {
        if (res.data && Array.isArray(res.data)) {
          this.setData({ auditCount: res.data.filter(d => d.status === 0).length });
        }
      }
    });
  },

  goToDeptManagement() {
    wx.navigateTo({
      url: `/pages/hospital-dept-list/hospital-dept-list?hospitalId=${this.data.hospitalId}`
    });
  },

  goToDoctorManagement() {
    wx.navigateTo({
      url: `/pages/hospital-doctor-list/hospital-doctor-list?hospitalId=${this.data.hospitalId}`
    });
  },

  goToScheduleManagement() {
    wx.navigateTo({
      url: `/pages/hospital-schedule-list/hospital-schedule-list?hospitalId=${this.data.hospitalId}`
    });
  },

  goToDoctorAudit() {
    wx.navigateTo({
      url: `/subpackages/admin/pages/doctor-audit/doctor-audit?hospitalId=${this.data.hospitalId}`
    });
  },

  goToAddDept() {
    wx.navigateTo({
      url: `/pages/dept-add/dept-add?hospitalId=${this.data.hospitalId}`
    });
  },

  goToViewDoctors() {
    wx.navigateTo({
      url: `/pages/hospital-doctor-list/hospital-doctor-list?hospitalId=${this.data.hospitalId}`
    });
  },

  goToBatchImport() {
    wx.navigateTo({
      url: '/pages/batch-import/batch-import?mode=doctors'
    });
  },

  goToArticleManage() {
    wx.navigateTo({
      url: `/pages/article-manage/article-manage?hospitalId=${this.data.hospitalId}`
    });
  },

  goToViewSchedule() {
    wx.navigateTo({
      url: `/pages/hospital-schedule-list/hospital-schedule-list?hospitalId=${this.data.hospitalId}`
    });
  },

  handleLogout() {
    wx.showModal({
      title: '确认退出',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          wx.clearStorageSync();
          app.globalData.userRole = null;
          app.globalData.hospitalId = null;
          app.globalData.hospitalName = null;
          wx.redirectTo({
            url: '/pages/login/login'
          });
        }
      }
    });
  }
})