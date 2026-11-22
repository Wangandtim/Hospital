const app = getApp();
const deptIcon = require('../../utils/dept-icon');

Page({
  data: {
    hospitalId: '',
    hospitalName: '',
    departments: [],
    searchText: '',
    filteredDepts: []
  },

  onLoad(options) {
    if (options.hospitalId) {
      this.setData({ hospitalId: options.hospitalId });
    }
    this.loadHospitalName();
    this.loadDepartments();
  },

  onShow() {
    // 编辑保存返回后刷新列表
    if (this._loaded) {
      this.loadDepartments();
    }
    this._loaded = true;
  },

  loadHospitalName() {
    wx.request({
      url: app.globalData.baseUrl + '/api/hospitals/' + this.data.hospitalId,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.hospitalName) {
          this.setData({ hospitalName: res.data.hospitalName });
        }
      }
    });
  },

  loadDepartments() {
    wx.showLoading({ title: '加载中...' });
    wx.request({
      url: app.globalData.baseUrl + '/api/departments/hospital/' + this.data.hospitalId,
      method: 'GET',
      success: (res) => {
        wx.hideLoading();
        if (res.data && Array.isArray(res.data)) {
          const list = res.data.map(dept => ({ ...dept, deptIcon: deptIcon.iconPath(dept.deptName) }));
          this.setData({ 
            departments: list,
            filteredDepts: list
          });
        }
      },
      fail: () => {
        wx.hideLoading();
        this.setData({ departments: [], filteredDepts: [] });
      }
    });
  },

  onSearchInput(e) {
    const searchText = e.detail.value;
    this.setData({ searchText });
    this.filterDepts(searchText);
  },

  filterDepts(keyword) {
    let filtered = this.data.departments;
    if (keyword) {
      const lowerKeyword = keyword.toLowerCase();
      filtered = filtered.filter(dept =>
        (dept.deptName || '').toLowerCase().includes(lowerKeyword)
      );
    }
    this.setData({ filteredDepts: filtered });
  },

  goToDeptDetail(e) {
    const deptId = e.currentTarget.dataset.deptid;
    if (!deptId) return;
    wx.navigateTo({
      url: `/pages/department-detail/department-detail?id=${deptId}`
    });
  },

  editDept(e) {
    const deptId = e.currentTarget.dataset.deptid;
    if (!deptId) return;
    wx.navigateTo({
      url: `/pages/dept-add/dept-add?deptId=${deptId}&hospitalId=${this.data.hospitalId}`
    });
  },

  goBack() {
    wx.navigateBack();
  }
});