const app = getApp();

Page({
  data: {
    auditList: [],
    filter: 'pending',
    pendingCount: 0,
    showRejectModal: false,
    rejectReason: '',
    currentAdmin: null
  },

  onLoad() {
    this.loadAuditList();
  },

  onShow() {
    this.loadAuditList();
  },

  goBack() {
    wx.navigateBack();
  },

  setFilter(e) {
    const filter = e.currentTarget.dataset.filter;
    this.setData({ filter });
    this.loadAuditList();
  },

  loadAuditList() {
    wx.showLoading({ title: '加载中...' });

    const statusMap = {
      pending: 0,
      approved: 1,
      rejected: 2
    };

    wx.request({
      url: app.globalData.baseUrl + '/api/admin/audit/list',
      method: 'GET',
      header: { 'X-Auth-Token': wx.getStorageSync('authToken') || '' },
      data: { auditStatus: statusMap[this.data.filter] },
      success: (res) => {
        wx.hideLoading();
        const list = res.data && Array.isArray(res.data.data)
          ? res.data.data
          : (Array.isArray(res.data) ? res.data : null);
        if (list) {
          this.setData({ 
            auditList: list,
            pendingCount: this.data.filter === 'pending'
              ? list.filter(a => a.auditStatus === 0).length
              : this.data.pendingCount
          });
        } else {
          wx.showToast({ title: '加载失败，请重新登录后重试', icon: 'none' });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '加载失败', icon: 'none' });
      }
    });
  },

  approveAdmin(e) {
    const admin = e.currentTarget.dataset.admin;
    
    wx.showModal({
      title: '确认通过',
      content: '确定要通过「' + admin.realName + '」的管理员申请吗？',
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '处理中...' });

          wx.request({
            url: app.globalData.baseUrl + '/api/admin/' + admin.adminId + '/audit',
            method: 'POST',
            header: { 'X-Auth-Token': wx.getStorageSync('authToken') || '' },
            data: { auditStatus: 1, comment: '' },
            success: (res) => {
              wx.hideLoading();
              if (res.data && res.data.success) {
                wx.showToast({ title: '审核通过', icon: 'success' });
                this.loadAuditList();
              } else {
                wx.showToast({ title: res.data.message || '审核失败', icon: 'none' });
              }
            },
            fail: () => {
              wx.hideLoading();
              wx.showToast({ title: '网络请求失败', icon: 'none' });
            }
          });
        }
      }
    });
  },

  rejectAdmin(e) {
    const admin = e.currentTarget.dataset.admin;
    this.setData({
      showRejectModal: true,
      currentAdmin: admin,
      rejectReason: ''
    });
  },

  closeRejectModal() {
    this.setData({ showRejectModal: false, currentAdmin: null, rejectReason: '' });
  },

  onRejectReasonInput(e) {
    this.setData({ rejectReason: e.detail.value });
  },

  submitReject() {
    if (!this.data.rejectReason) {
      wx.showToast({ title: '请输入拒绝原因', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '处理中...' });

    wx.request({
      url: app.globalData.baseUrl + '/api/admin/' + this.data.currentAdmin.adminId + '/audit',
      method: 'POST',
      header: { 'X-Auth-Token': wx.getStorageSync('authToken') || '' },
      data: { auditStatus: 2, comment: this.data.rejectReason },
      success: (res) => {
        wx.hideLoading();
        if (res.data && res.data.success) {
          wx.showToast({ title: '已拒绝', icon: 'success' });
          this.closeRejectModal();
          this.loadAuditList();
        } else {
          wx.showToast({ title: res.data.message || '操作失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '网络请求失败', icon: 'none' });
      }
    });
  }
})