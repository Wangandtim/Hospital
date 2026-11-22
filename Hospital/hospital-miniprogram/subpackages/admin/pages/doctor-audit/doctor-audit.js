const app = getApp();
const deptIcon = require('../../../../utils/dept-icon');

Page({
  data: {
    auditList: [],
    filter: 'pending',
    pendingCount: 0,
    showRejectModal: false,
    rejectReason: '',
    currentDoctor: null,
    hospitalId: ''
  },

  onLoad() {
    this.data.hospitalId = wx.getStorageSync('hospitalId');
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
    const token = wx.getStorageSync('token') || wx.getStorageSync('authToken');

    const statusMap = {
      pending: 0,
      approved: 1,
      rejected: 2
    };

    let url = app.globalData.baseUrl + '/api/doctors/audit/list';
    const params = { auditStatus: statusMap[this.data.filter], page: 0, size: 100 };
    
    if (this.data.hospitalId) {
      params.hospitalId = this.data.hospitalId;
    }

    wx.request({
      url: url,
      method: 'GET',
      data: params,
      header: token ? { 'X-Auth-Token': token } : {},
      success: (res) => {
        wx.hideLoading();
        const list = res.data && Array.isArray(res.data.data)
          ? res.data.data
          : (Array.isArray(res.data) ? res.data : null);
        if (list) {
          const auditList = list.map(a => Object.assign({}, a, {
            deptIcon: deptIcon.iconPath(a.deptName)
          }));
          this.setData({ 
            auditList: auditList,
            pendingCount: this.data.filter === 'pending'
              ? auditList.filter(a => a.auditStatus === 0).length
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

  approveDoctor(e) {
    const doctor = e.currentTarget.dataset.doctor;
    const token = wx.getStorageSync('token') || wx.getStorageSync('authToken');
    
    wx.showModal({
      title: '确认通过',
      content: '确定要通过「' + doctor.name + '」的医生申请吗？',
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '处理中...' });

          wx.request({
            url: app.globalData.baseUrl + '/api/doctors/' + doctor.doctorId + '/audit',
            method: 'POST',
            header: Object.assign({ 'Content-Type': 'application/json' }, token ? { 'X-Auth-Token': token } : {}),
            data: { auditStatus: 1, comment: '' },
            success: (res) => {
              wx.hideLoading();
              if (res.data && res.data.success) {
                wx.showToast({ title: '审核通过', icon: 'success' });
                this.loadAuditList();
              } else {
                wx.showToast({ title: (res.data && res.data.message) || '审核失败', icon: 'none' });
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

  rejectDoctor(e) {
    const doctor = e.currentTarget.dataset.doctor;
    this.setData({
      showRejectModal: true,
      currentDoctor: doctor,
      rejectReason: ''
    });
  },

  closeRejectModal() {
    this.setData({ showRejectModal: false, currentDoctor: null, rejectReason: '' });
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

    const token = wx.getStorageSync('token') || wx.getStorageSync('authToken');
    wx.request({
      url: app.globalData.baseUrl + '/api/doctors/' + this.data.currentDoctor.doctorId + '/audit',
      method: 'POST',
      header: Object.assign({ 'Content-Type': 'application/json' }, token ? { 'X-Auth-Token': token } : {}),
      data: { auditStatus: 2, comment: this.data.rejectReason },
      success: (res) => {
        wx.hideLoading();
        if (res.data && res.data.success) {
          wx.showToast({ title: '已拒绝', icon: 'success' });
          this.closeRejectModal();
          this.loadAuditList();
        } else {
          wx.showToast({ title: (res.data && res.data.message) || '操作失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '网络请求失败', icon: 'none' });
      }
    });
  }
})