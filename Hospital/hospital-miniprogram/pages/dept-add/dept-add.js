const app = getApp();
const deptIcon = require('../../utils/dept-icon');

Page({
  data: {
    hospitalId: '',
    deptId: '',
    pageTitle: '新增科室',
    deptIcon: deptIcon.iconPath(''),
    levels: ['未设置', '国家级临床重点专科', '省级临床重点专科', '市级临床重点专科'],
    levelIndex: 0,
    formData: {
      deptName: '',
      description: '',
      diseases: '',
      serviceTime: '',
      awards: '',
      cases: ''
    }
  },

  onLoad(options) {
    if (options.hospitalId) {
      this.setData({ hospitalId: options.hospitalId });
    }
    if (options.deptId) {
      this.setData({ deptId: options.deptId, pageTitle: '编辑科室' });
      this.loadDepartment(options.deptId);
    }
  },

  loadDepartment(deptId) {
    wx.showLoading({ title: '加载中...' });
    const token = wx.getStorageSync('token') || wx.getStorageSync('authToken');
    wx.request({
      url: app.globalData.baseUrl + '/api/departments/' + deptId,
      method: 'GET',
      header: token ? { 'X-Auth-Token': token } : {},
      success: (res) => {
        wx.hideLoading();
        const dept = res.data || {};
        const level = Number(dept.flagshipLevel) || 0;
        this.setData({
          hospitalId: dept.hospitalId || this.data.hospitalId,
          levelIndex: level,
          deptIcon: deptIcon.iconPath(dept.deptName || dept.name || ''),
          formData: {
            deptName: dept.deptName || dept.name || '',
            description: dept.description || '',
            diseases: dept.diseases || '',
            serviceTime: dept.serviceTime || '',
            awards: dept.awards || '',
            cases: dept.cases || ''
          }
        });
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '加载失败', icon: 'none' });
      }
    });
  },

  onInputChange(e) {
    const field = e.currentTarget.dataset.field;
    const value = e.detail.value;
    const patch = {
      [`formData.${field}`]: value
    };
    if (field === 'deptName') {
      patch.deptIcon = deptIcon.iconPath(value);
    }
    this.setData(patch);
  },

  onLevelChange(e) {
    this.setData({ levelIndex: Number(e.detail.value) });
  },

  submitForm() {
    const form = this.data.formData;

    if (!form.deptName.trim()) {
      wx.showToast({ title: '请输入科室名称', icon: 'none' });
      return;
    }

    const payload = {
      hospitalId: this.data.hospitalId,
      name: form.deptName.trim(),
      description: form.description.trim(),
      diseases: form.diseases,
      serviceTime: form.serviceTime,
      awards: form.awards,
      cases: form.cases,
      flagshipLevel: this.data.levelIndex,
      isFlagship: this.data.levelIndex > 0 ? 1 : 0
    };

    const editing = Boolean(this.data.deptId);
    const token = wx.getStorageSync('token') || wx.getStorageSync('authToken');
    wx.showLoading({ title: '提交中...' });
    wx.request({
      url: editing
        ? app.globalData.baseUrl + '/api/departments/' + this.data.deptId
        : app.globalData.baseUrl + '/api/departments',
      method: editing ? 'PUT' : 'POST',
      header: Object.assign({ 'Content-Type': 'application/json' }, token ? { 'X-Auth-Token': token } : {}),
      data: payload,
      success: (res) => {
        wx.hideLoading();
        if (res.data && (res.data.success || res.data.deptId)) {
          wx.showToast({ title: editing ? '保存成功' : '添加成功', icon: 'success' });
          setTimeout(() => {
            wx.navigateBack();
          }, 1200);
        } else {
          wx.showToast({ title: (res.data && res.data.message) || '保存失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '网络错误', icon: 'none' });
      }
    });
  },

  goBack() {
    wx.navigateBack();
  }
});
