const app = getApp();
const config = require('../../../../config/index');

Page({
  data: {
    adminList: [],
    searchKeyword: '',
    apiBase: config.apiBase + '/api',
    hospitalId: '',
    adminRole: '',
    currentPage: 0,
    pageSize: 10,
    total: 0,
    totalPages: 0,
    showModal: false,
    saving: false,
    currentAdminId: '',
    form: {
      username: '',
      realName: '',
      phone: '',
      role: '',
      hospitalId: '',
      auditStatus: null,
      status: 1,
      password: ''
    }
  },

  onLoad() {
    this.setData({
      hospitalId: wx.getStorageSync('hospitalId'),
      adminRole: wx.getStorageSync('adminRole')
    });
    this.loadAdminList();
  },

  onShow() {
    this.loadAdminList();
  },

  loadAdminList(page = 0) {
    let url = this.data.apiBase + '/admin/list';
    const params = [];
    
    if (this.data.adminRole === 'HOSPITAL_ADMIN' && this.data.hospitalId) {
      params.push('hospitalId=' + this.data.hospitalId);
    }
    
    if (this.data.searchKeyword) {
      params.push('keyword=' + this.data.searchKeyword);
    }
    
    params.push('page=' + page);
    params.push('size=' + this.data.pageSize);
    
    if (params.length > 0) {
      url += '?' + params.join('&');
    }

    wx.request({
      url: url,
      method: 'GET',
      header: { 'X-Auth-Token': wx.getStorageSync('authToken') || '' },
      success: (res) => {
        if (res.data.success && Array.isArray(res.data.data)) {
          this.setData({
            adminList: res.data.data,
            currentPage: res.data.page,
            total: res.data.total,
            totalPages: res.data.totalPages
          });
        }
      }
    });
  },

  onSearchInput(e) {
    this.setData({ searchKeyword: e.detail.value });
    this.loadAdminList();
  },

  showFilter() {
    wx.showActionSheet({
      itemList: ['全部', '超级管理员', '医院管理员', '应急管理员'],
      success: (res) => {
        const roles = ['', 'SUPER_ADMIN', 'HOSPITAL_ADMIN', 'EMERGENCY_ADMIN'];
        const selectedRole = roles[res.tapIndex];
        if (selectedRole) {
          wx.request({
            url: this.data.apiBase + '/admin/list?role=' + selectedRole + '&page=0&size=' + this.data.pageSize,
            method: 'GET',
            header: { 'X-Auth-Token': wx.getStorageSync('authToken') || '' },
            success: (res) => {
              if (res.data.success && Array.isArray(res.data.data)) {
                this.setData({
                  adminList: res.data.data,
                  currentPage: res.data.page,
                  total: res.data.total,
                  totalPages: res.data.totalPages
                });
              }
            }
          });
        } else {
          this.loadAdminList(0);
        }
      }
    });
  },

  onPrevPage() {
    if (this.data.currentPage > 0) {
      this.loadAdminList(this.data.currentPage - 1);
    }
  },

  onNextPage() {
    if (this.data.currentPage < this.data.totalPages - 1) {
      this.loadAdminList(this.data.currentPage + 1);
    }
  },

  getRoleDisplayName(role) {
    switch (role) {
      case 'SUPER_ADMIN': return '超级管理员';
      case 'HOSPITAL_ADMIN': return '医院管理员';
      case 'EMERGENCY_ADMIN': return '应急管理员';
      default: return role;
    }
  },

  goBack() {
    wx.navigateBack();
  },

  goToAdd() {
    // 单个管理员由本人从登录页申请；超管需要直接建号时走批量导入
    wx.navigateTo({
      url: '/pages/batch-import/batch-import?mode=admins'
    });
  },

  goToDetail(e) {
    const adminId = e.currentTarget.dataset.adminId;
    const admin = (this.data.adminList || []).find(item => item.adminId === adminId);
    if (!admin) return;
    this.setData({
      showModal: true,
      currentAdminId: admin.adminId,
      form: {
        username: admin.username || '',
        realName: admin.realName || '',
        phone: admin.phone || '',
        role: admin.role || '',
        hospitalId: admin.hospitalId || '',
        auditStatus: admin.auditStatus,
        status: admin.status === 0 ? 0 : 1,
        password: ''
      }
    });
  },

  closeModal() {
    if (this.data.saving) return;
    this.setData({ showModal: false });
  },

  noop() {},

  onRealNameInput(e) {
    this.setData({ 'form.realName': e.detail.value });
  },

  onPhoneInput(e) {
    this.setData({ 'form.phone': e.detail.value });
  },

  onPasswordInput(e) {
    this.setData({ 'form.password': e.detail.value });
  },

  onStatusChange(e) {
    this.setData({ 'form.status': e.detail.value ? 1 : 0 });
  },

  saveAdmin() {
    if (this.data.saving) return;
    const form = this.data.form;
    if (!form.realName) {
      wx.showToast({ title: '请填写真实姓名', icon: 'none' });
      return;
    }
    if (form.password && form.password.length < 6) {
      wx.showToast({ title: '密码至少 6 位', icon: 'none' });
      return;
    }

    const payload = {
      realName: form.realName,
      phone: form.phone,
      status: form.status
    };
    if (form.password) {
      payload.password = form.password;
    }

    this.setData({ saving: true });
    wx.request({
      url: this.data.apiBase + '/admin/' + this.data.currentAdminId,
      method: 'PUT',
      header: { 'X-Auth-Token': wx.getStorageSync('authToken') || '' },
      data: payload,
      success: (res) => {
        this.setData({ saving: false });
        const data = res.data || {};
        if (res.statusCode >= 200 && res.statusCode < 300 && data.success) {
          wx.showToast({ title: '已保存', icon: 'success' });
          this.setData({ showModal: false });
          this.loadAdminList(this.data.currentPage);
        } else {
          wx.showModal({
            title: '保存失败',
            content: data.message || ('请求失败（' + res.statusCode + '）'),
            showCancel: false
          });
        }
      },
      fail: () => {
        this.setData({ saving: false });
        wx.showModal({ title: '网络超时', content: '保存失败，请检查网络后重试。', showCancel: false });
      }
    });
  },

  deleteAdmin() {
    const adminId = this.data.currentAdminId;
    if (!adminId) return;
    if (adminId === wx.getStorageSync('adminId')) {
      wx.showModal({ title: '不能删除自己', content: '请使用其他总管理员账号操作。', showCancel: false });
      return;
    }
    wx.showModal({
      title: '删除管理员',
      content: '删除后该账号立即失效且无法恢复，确定继续吗？',
      confirmText: '删除',
      confirmColor: '#ef4444',
      cancelText: '取消',
      success: (modalRes) => {
        if (!modalRes.confirm) return;
        wx.request({
          url: this.data.apiBase + '/admin/' + adminId,
          method: 'DELETE',
          header: { 'X-Auth-Token': wx.getStorageSync('authToken') || '' },
          success: (res) => {
            const data = res.data || {};
            if (res.statusCode >= 200 && res.statusCode < 300 && data.success) {
              wx.showToast({ title: '已删除', icon: 'success' });
              this.setData({ showModal: false });
              this.loadAdminList(this.data.currentPage);
            } else {
              wx.showModal({
                title: '删除失败',
                content: data.message || ('请求失败（' + res.statusCode + '）'),
                showCancel: false
              });
            }
          },
          fail: () => {
            wx.showModal({ title: '网络超时', content: '删除失败，请检查网络后重试。', showCancel: false });
          }
        });
      }
    });
  }
})