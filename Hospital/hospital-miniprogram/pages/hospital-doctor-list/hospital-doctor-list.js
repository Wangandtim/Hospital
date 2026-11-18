const app = getApp();

Page({
  data: {
    hospitalId: '',
    hospitalName: '',
    doctors: [],
    departments: {},
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
          this.setData({ 
            doctors: res.data,
            filteredDoctors: res.data
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
        if (res.data && Array.isArray(res.data)) {
          res.data.forEach(dept => {
            departments[dept.deptId] = dept;
          });
        }
        this.setData({ departments });
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