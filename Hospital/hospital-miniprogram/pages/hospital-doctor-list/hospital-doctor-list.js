const app = getApp();
const deptIcon = require('../../utils/dept-icon');

Page({
  data: {
    hospitalId: '',
    hospitalName: '',
    doctors: [],
    departments: {},
    deptIcons: {},
    deptNames: {},
    searchText: '',
    selectedDept: '',
    filteredDoctors: []
  },

  onLoad(options) {
    if (options.hospitalId) {
      this.setData({ hospitalId: options.hospitalId });
    }
    this.loadHospitalName();
    this.loadDoctors();
    this.loadDepartments();
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

  loadDoctors() {
    wx.showLoading({ title: '加载中...' });
    wx.request({
      url: app.globalData.baseUrl + '/api/doctors/hospital/' + this.data.hospitalId,
      method: 'GET',
      success: (res) => {
        wx.hideLoading();
        if (res.data && Array.isArray(res.data)) {
          const doctors = res.data.map(item => ({
            ...item,
            avatarText: String(item.name || '医').slice(0, 1)
          }));
          this.setData({ 
            doctors: doctors,
            filteredDoctors: doctors
          });
        }
      },
      fail: () => {
        wx.hideLoading();
        this.setData({ doctors: [], filteredDoctors: [] });
      }
    });
  },

  loadDepartments() {
    wx.request({
      url: app.globalData.baseUrl + '/api/departments/hospital/' + this.data.hospitalId,
      method: 'GET',
      success: (res) => {
        const departments = {};
        const deptIcons = {};
        const deptNames = {};
        if (res.data && Array.isArray(res.data)) {
          res.data.forEach(dept => {
            departments[dept.deptId] = dept;
            deptNames[dept.deptId] = dept.deptName;
            deptIcons[dept.deptId] = deptIcon.iconPath(dept.deptName);
          });
        }
        this.setData({ departments, deptIcons, deptNames });
      }
    });
  },

  onSearchInput(e) {
    const searchText = e.detail.value;
    this.setData({ searchText });
    this.filterDoctors();
  },

  onDeptChange(e) {
    const selectedDept = e.detail.value;
    this.setData({ selectedDept });
    this.filterDoctors();
  },

  filterDoctors() {
    let filtered = this.data.doctors;
    
    if (this.data.selectedDept) {
      filtered = filtered.filter(doc => doc.deptId === this.data.selectedDept);
    }
    
    if (this.data.searchText) {
      const keyword = this.data.searchText.toLowerCase();
      filtered = filtered.filter(doc => 
        doc.name.toLowerCase().includes(keyword) ||
        (doc.doctorId && doc.doctorId.toLowerCase().includes(keyword))
      );
    }
    
    this.setData({ filteredDoctors: filtered });
  },

  getDeptName(deptId) {
    return this.data.departments[deptId]?.deptName || '未知科室';
  },

  goToDoctorDetail(doctorId) {
    wx.navigateTo({
      url: `/pages/doctor-detail/doctor-detail?doctorId=${doctorId}&hospitalId=${this.data.hospitalId}`
    });
  },

  goBack() {
    wx.navigateBack();
  }
});