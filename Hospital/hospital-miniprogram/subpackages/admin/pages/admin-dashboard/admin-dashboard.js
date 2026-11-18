const app = getApp();
const config = require('../../../../config/index');

Page({
  data: {
    stats: {
      hospitalCount: 0
    },
    apiBase: config.apiBase + '/api',
    hospitalId: '',
    hospitalName: '',
    adminRole: '',
    roleDisplayName: '',
    pendingEmergencyCount: 0,
    pendingConfirmCount: 0
  },

  onLoad() {
    const role = wx.getStorageSync('userRole');
    const hospitalId = wx.getStorageSync('hospitalId');
    const hospitalName = wx.getStorageSync('hospitalName');
    const adminRole = wx.getStorageSync('adminRole');

    if (role !== 'admin') {
      wx.redirectTo({
        url: '/pages/login/login'
      });
      return;
    }

    this.setData({
      hospitalId: hospitalId,
      hospitalName: hospitalName || '未知医院',
      adminRole: adminRole || 'HOSPITAL_ADMIN',
      roleDisplayName: this.getRoleDisplayName(adminRole)
    });
  },

  onShow() {
    if (this.data.adminRole === 'SUPER_ADMIN') {
      this.loadSuperAdminStats();
      this.loadSupervisorPending();
    }
    if (this.data.adminRole === 'HOSPITAL_ADMIN' && this.data.hospitalId) {
      this.loadPendingEmergency();
      this.startPendingPolling();
    }
  },

  onHide() {
    this.stopPendingPolling();
  },

  onUnload() {
    this.stopPendingPolling();
  },

  startPendingPolling() {
    this.stopPendingPolling();
    this.pendingTimer = setInterval(() => this.loadPendingEmergency(), 20000);
  },

  stopPendingPolling() {
    if (this.pendingTimer) {
      clearInterval(this.pendingTimer);
      this.pendingTimer = null;
    }
  },

  /** 管理员打开工作台时主动拉取本院应急通道使用记录，避免错过异常。 */
  loadPendingEmergency() {
    const hospitalId = this.data.hospitalId;
    if (!hospitalId) return;
    wx.request({
      url: this.data.apiBase + '/emergency/requests/' + hospitalId,
      method: 'GET',
      success: (res) => {
        const data = (res.data && res.data.success) ? (res.data.data || []) : [];
        // 仅统计需要院管关注的状态：使用中/待报告/待确认/逾期，已归档不计入提醒
        const count = data.filter(item => [0, 1, 2, 6, 7].indexOf(item.status) >= 0).length;
        this.setData({ pendingEmergencyCount: count });
        if (count === 0) {
          this._alertedCount = 0;
          return;
        }
        if (this._alertedCount !== count) {
          this._alertedCount = count;
          wx.showModal({
            title: '应急通道使用提醒',
            content: '本院有 ' + count + ' 条应急通道使用记录，权限已自动开通。请核对证件与结束报告，发现异常可立即终止或冻结。',
            confirmText: '去查看',
            cancelText: '稍后处理',
            success: (modalRes) => {
              if (modalRes.confirm) {
                this.goToMyHospitalKey();
              }
            }
          });
        }
      }
    });
  },

  goToEmergencyApproval() {
    this.goToMyHospitalKey();
  },

  loadSupervisorPending() {
    wx.request({
      url: this.data.apiBase + '/emergency/supervisor/pending',
      method: 'GET',
      success: (res) => {
        const data = (res.data && res.data.success) ? (res.data.data || []) : [];
        this.setData({ pendingConfirmCount: data.length });
      }
    });
  },

  getRoleDisplayName(role) {
    switch (role) {
      case 'SUPER_ADMIN': return '总管理员';
      case 'HOSPITAL_ADMIN': return '医院管理员';
      case 'EMERGENCY_ADMIN': return '应急管理员';
      default: return '管理员';
    }
  },

  loadSuperAdminStats() {
    wx.request({
      url: this.data.apiBase + '/hospitals',
      method: 'GET',
      success: (res) => {
        const count = Array.isArray(res.data) ? res.data.length : 0;
        this.setData({
          'stats.hospitalCount': count
        });
      }
    });
  },

  handleLogout() {
    wx.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          wx.removeStorageSync('authToken');
          wx.removeStorageSync('token');
          wx.removeStorageSync('userRole');
          wx.removeStorageSync('username');
          wx.removeStorageSync('adminId');
          wx.removeStorageSync('hospitalId');
          wx.removeStorageSync('hospitalName');
          wx.removeStorageSync('adminRole');
          app.globalData.userRole = null;
          app.globalData.hospitalId = null;
          app.globalData.hospitalName = null;
          app.globalData.adminRole = null;

          wx.redirectTo({
            url: '/pages/login/login'
          });
        }
      }
    });
  },

  goToHospitalManage() {
    wx.navigateTo({
      url: '/pages/hospital-manage/hospital-manage'
    });
  },

  goToHospitalInfo() {
    wx.navigateTo({
      url: '/pages/hospital-manage/hospital-manage?view=info'
    });
  },

  goToBatchImport() {
    wx.navigateTo({
      url: '/pages/batch-import/batch-import?mode=admins'
    });
  },

  goToMyHospitalKey() {
    if (!this.data.hospitalId) {
      wx.showModal({
        title: '无法获取医院信息',
        content: '当前登录信息缺少医院标识，请重新登录。',
        showCancel: false
      });
      return;
    }
    wx.navigateTo({
      url: '/pages/key-management/key-management?hospitalId=' + this.data.hospitalId
    });
  },

  goToKeyManagement() {
    wx.navigateTo({
      url: '/pages/key-management/key-management'
    });
  },

  goToEmergencyReview() {
    wx.navigateTo({
      url: '/subpackages/admin/pages/emergency-review/emergency-review'
    });
  },

  goToAdminManage() {
    wx.navigateTo({
      url: '/subpackages/admin/pages/admin-manage/admin-manage'
    });
  },

  goToAdminAudit() {
    wx.navigateTo({
      url: '/subpackages/admin/pages/admin-audit/admin-audit'
    });
  },

  goToDoctorManage() {
    wx.navigateTo({
      url: '/pages/doctor-manage/doctor-manage'
    });
  },

  goToDepartmentManage() {
    wx.navigateTo({
      url: '/pages/department-manage/department-manage'
    });
  },

  goToAppointmentManage() {
    wx.navigateTo({
      url: '/pages/appointment-manage/appointment-manage'
    });
  },

  goToDoctorList() {
    wx.navigateTo({
      url: '/pages/doctor-manage/doctor-manage?view=list'
    });
  }
})