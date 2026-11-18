const app = getApp();

Page({
  data: {
    hospitalId: '',
    deptId: '',
    pageTitle: '新增科室',
    levels: ['未设置', '国家级临床重点专科', '省级临床重点专科', '市级临床重点专科'],
    levelIndex: 0,
    formData: {
      deptName: '',
      deptCode: '',
      manager: '',
      phone: '',
      description: '',
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
    wx.request({
      url: app.globalData.baseUrl + '/api/departments/' + deptId,
      method: 'GET',
      success: (res) => {
        wx.hideLoading();
        const dept = res.data || {};
        const level = Number(dept.flagshipLevel) || 0;
        this.setData({
          hospitalId: dept.hospitalId || this.data.hospitalId,
          levelIndex: level,
          formData: {
            deptName: dept.deptName || '',
            deptCode: dept.deptCode || '',
            manager: dept.manager || '',
            phone: dept.phone || '',
            description: dept.description || '',
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
    this.setData({
      [`formData.${field}`]: value
    });
  },

  onLevelChange(e) {
    this.setData({ levelIndex: Number(e.detail.value) });
  },

  submitForm() {
    const { deptName, deptCode } = this.data.formData;
    
    if (!deptName.trim()) {
      wx.showToast({ title: '请输入科室名称', icon: 'none' });
      return;
    }
    
    if (!deptCode.trim()) {
      wx.showToast({ title: '请输入科室编码', icon: 'none' });
      return;
    }

    const payload = {
      hospitalId: this.data.hospitalId,
      deptName: deptName.trim(),
      deptCode: deptCode.trim(),
      manager: this.data.formData.manager.trim(),
      phone: this.data.formData.phone.trim(),
      description: this.data.formData.description.trim(),
      awards: this.data.formData.awards,
      cases: this.data.formData.cases,
      flagshipLevel: this.data.levelIndex,
      isFlagship: this.data.levelIndex > 0 ? 1 : 0
    };

    const editing = Boolean(this.data.deptId);
    wx.showLoading({ title: '提交中...' });
    wx.request({
      url: editing
        ? app.globalData.baseUrl + '/api/departments/' + this.data.deptId
        : app.globalData.baseUrl + '/api/departments',
      method: editing ? 'PUT' : 'POST',
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
