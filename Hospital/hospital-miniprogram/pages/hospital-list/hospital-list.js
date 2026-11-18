const app = getApp();

Page({
  data: {
    hospitals: [],
    provinces: [],
    cities: [],
    districts: [],
    selectedProvince: '',
    selectedCity: '',
    selectedDistrict: '',
    currentPage: 0,
    pageSize: 10,
    totalPages: 1,
    totalElements: 0,
    hasNext: false,
    hasPrevious: false,
    isLoading: false,
    showFilterPanel: false
  },

  onLoad() {
    this.loadProvinces();
    this.loadHospitals();
  },

  loadProvinces() {
    wx.request({
      url: app.globalData.baseUrl + '/api/hospitals/provinces',
      method: 'GET',
      success: (res) => {
        if (res.data && Array.isArray(res.data)) {
          this.setData({ provinces: res.data });
        }
      }
    });
  },

  loadHospitals(page = 0) {
    this.setData({ isLoading: true });
    
    let url = app.globalData.baseUrl + `/api/hospitals/page?page=${page}&size=${this.data.pageSize}`;
    
    if (this.data.selectedProvince) {
      url += `&province=${encodeURIComponent(this.data.selectedProvince)}`;
    }
    if (this.data.selectedCity) {
      url += `&city=${encodeURIComponent(this.data.selectedCity)}`;
    }
    if (this.data.selectedDistrict) {
      url += `&district=${encodeURIComponent(this.data.selectedDistrict)}`;
    }

    wx.request({
      url: url,
      method: 'GET',
      success: (res) => {
        this.setData({ isLoading: false });
        if (res.data) {
          this.setData({
            hospitals: res.data.content || [],
            currentPage: res.data.currentPage || 0,
            totalPages: res.data.totalPages || 1,
            totalElements: res.data.totalElements || 0,
            hasNext: res.data.hasNext || false,
            hasPrevious: res.data.hasPrevious || false
          });
        }
      },
      fail: () => {
        this.setData({ isLoading: false });
        wx.showToast({ title: '加载失败', icon: 'none' });
      }
    });
  },

  onProvinceChange(e) {
    const index = e.detail.value;
    const province = this.data.provinces[index];
    this.setData({
      selectedProvince: province || '',
      selectedCity: '',
      selectedDistrict: '',
      cities: [],
      districts: [],
      currentPage: 0
    });

    if (province) {
      this.loadCities(province);
    }
    
    this.loadHospitals(0);
  },

  loadCities(province) {
    wx.request({
      url: app.globalData.baseUrl + `/api/hospitals/cities?province=${encodeURIComponent(province)}`,
      method: 'GET',
      success: (res) => {
        if (res.data && Array.isArray(res.data)) {
          this.setData({ cities: res.data });
        }
      }
    });
  },

  onCityChange(e) {
    const index = e.detail.value;
    const city = this.data.cities[index];
    this.setData({
      selectedCity: city || '',
      selectedDistrict: '',
      districts: [],
      currentPage: 0
    });

    if (city && this.data.selectedProvince) {
      this.loadDistricts(this.data.selectedProvince, city);
    }
    
    this.loadHospitals(0);
  },

  loadDistricts(province, city) {
    wx.request({
      url: app.globalData.baseUrl + `/api/hospitals/districts?province=${encodeURIComponent(province)}&city=${encodeURIComponent(city)}`,
      method: 'GET',
      success: (res) => {
        if (res.data && Array.isArray(res.data)) {
          this.setData({ districts: res.data });
        }
      }
    });
  },

  onDistrictChange(e) {
    const index = e.detail.value;
    const district = this.data.districts[index];
    this.setData({
      selectedDistrict: district || '',
      currentPage: 0
    });
    this.loadHospitals(0);
  },

  toggleFilterPanel() {
    this.setData({ showFilterPanel: !this.data.showFilterPanel });
  },

  clearFilter() {
    this.setData({
      selectedProvince: '',
      selectedCity: '',
      selectedDistrict: '',
      cities: [],
      districts: [],
      currentPage: 0
    });
    this.loadHospitals(0);
  },

  selectHospital(e) {
    const hospital = e.currentTarget.dataset.hospital;
    wx.setStorageSync('selectedHospital', hospital);
    app.globalData.selectedHospital = hospital;
    
    wx.showModal({
      title: '选择成功',
      content: `已选择${hospital.hospitalName}，将返回首页查看该医院的数据`,
      confirmText: '确定',
      success: (res) => {
        if (res.confirm) {
          wx.switchTab({
            url: '/pages/index/index'
          });
        }
      }
    });
  },

  prevPage() {
    if (this.data.hasPrevious) {
      this.loadHospitals(this.data.currentPage - 1);
    }
  },

  nextPage() {
    if (this.data.hasNext) {
      this.loadHospitals(this.data.currentPage + 1);
    }
  },

  goToPage(e) {
    const page = e.currentTarget.dataset.page;
    if (page >= 0 && page < this.data.totalPages) {
      this.loadHospitals(page);
    }
  }
});