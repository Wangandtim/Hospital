const app = getApp();
const placeholder = require('../../utils/placeholder');
const pager = require('../../utils/pager');

Page({
  data: {
    hospitals: [],
    provinces: [],
    cities: [],
    districts: [],
    selectedProvince: '',
    selectedCity: '',
    selectedDistrict: '',
    provinceIndex: 0,
    cityIndex: 0,
    districtIndex: 0,
    pager: { status: 'idle', hasMore: true, total: 0, error: '', loading: false },
    isLoading: true,
    showFilterPanel: false
  },

  onLoad() {
    this.createPager();
    this.loadProvinces();
    this.reload();
  },

  onUnload() {
    if (this.pager) {
      this.pager.destroy();
    }
  },

  onReachBottom() {
    if (this.pager) {
      this.pager.next();
    }
  },

  createPager() {
    this.pager = pager.create({
      size: 20,
      scope: this,
      fetch: (page, size) => {
        let url = app.globalData.baseUrl + `/api/hospitals/page?page=${page}&size=${size}`;
        if (this.data.selectedProvince) {
          url += `&province=${encodeURIComponent(this.data.selectedProvince)}`;
        }
        if (this.data.selectedCity) {
          url += `&city=${encodeURIComponent(this.data.selectedCity)}`;
        }
        if (this.data.selectedDistrict) {
          url += `&district=${encodeURIComponent(this.data.selectedDistrict)}`;
        }
        return new Promise((resolve, reject) => {
          wx.request({
            url: url,
            method: 'GET',
            success: res => resolve(res.data),
            fail: reject
          });
        });
      },
      onReset: () => {
        this.setData({ hospitals: [], isLoading: true });
      },
      onData: state => {
        const hospitals = (state.list || []).map(hospital => {
          const extra = placeholder.hospitalExtra(hospital);
          return {
            ...hospital,
            ...extra,
            distanceText: hospital.distanceKm ? hospital.distanceKm + 'km' : '距离未知',
            addressText: hospital.address || [hospital.province, hospital.city, hospital.district].filter(Boolean).join(' · '),
            regionText: [hospital.province, hospital.city, hospital.district].filter(Boolean).join(' · ')
          };
        });
        this.setData({
          hospitals: hospitals,
          pager: state.pager,
          isLoading: state.pager.status === 'loading'
        });
      }
    });
  },

  reload() {
    if (this.pager) {
      this.pager.reset();
    }
  },

  retryLoad() {
    if (this.pager) {
      this.pager.retry();
    }
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

  onProvinceChange(e) {
    const index = e.detail.value;
    const province = this.data.provinces[index];
    this.setData({
      selectedProvince: province || '',
      provinceIndex: index,
      selectedCity: '',
      cityIndex: 0,
      selectedDistrict: '',
      districtIndex: 0,
      cities: [],
      districts: []
    });

    if (province) {
      this.loadCities(province);
    }
    this.reload();
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
      cityIndex: index,
      selectedDistrict: '',
      districtIndex: 0,
      districts: []
    });

    if (city && this.data.selectedProvince) {
      this.loadDistricts(this.data.selectedProvince, city);
    }
    this.reload();
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
    this.setData({ selectedDistrict: district || '', districtIndex: index });
    this.reload();
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
      districts: []
    });
    this.reload();
  },

  selectHospital(e) {
    const hospital = e.currentTarget.dataset.hospital;
    wx.setStorageSync('selectedHospital', hospital);
    app.globalData.selectedHospital = hospital;

    wx.showModal({
      title: '已选择',
      content: `已选择${hospital.hospitalName}，返回首页查看该医院的号源`,
      confirmText: '确定',
      success: (res) => {
        if (res.confirm) {
          wx.switchTab({ url: '/pages/index/index' });
        }
      }
    });
  }
});
