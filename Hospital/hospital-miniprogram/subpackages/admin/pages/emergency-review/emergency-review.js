const app = getApp();

Page({
  data: {
    supervisorPending: [],
    hospitalId: '',
    hospitalName: '',
    lockInfo: null,
    pickerOpen: false,
    provinces: [],
    cities: [],
    districts: [],
    provinceIndex: -1,
    cityIndex: -1,
    districtIndex: -1,
    province: '',
    city: '',
    district: '',
    hospitalKeyword: '',
    hospitalList: [],
    hospitalPage: 0,
    hospitalTotalPages: 1,
    hospitalTotal: 0
  },

  onLoad() {
    this.loadProvinces();
    this.loadHospitalPage(0);
    this.loadSupervisorPending();
  },

  onShow() {
    this.loadSupervisorPending();
    if (this.data.hospitalId) {
      this.loadEmergencyStatus(this.data.hospitalId);
    }
  },

  goBack() {
    wx.navigateBack();
  },

  authHeader() {
    const token = wx.getStorageSync('authToken') || wx.getStorageSync('token') || '';
    return token ? { 'X-Auth-Token': token } : {};
  },

  openHospitalPicker() {
    this.setData({ pickerOpen: true });
  },

  closeHospitalPicker() {
    this.setData({ pickerOpen: false });
  },

  loadProvinces() {
    wx.request({
      url: app.globalData.baseUrl + '/api/hospitals/provinces',
      method: 'GET',
      success: (res) => {
        const list = Array.isArray(res.data) ? res.data : [];
        this.setData({ provinces: list });
      }
    });
  },

  bindProvinceChange(e) {
    const index = Number(e.detail.value);
    const province = this.data.provinces[index] || '';
    this.setData({
      provinceIndex: index,
      province: province,
      cities: [],
      cityIndex: -1,
      city: '',
      districts: [],
      districtIndex: -1,
      district: ''
    });
    if (province) this.loadCities(province);
    this.loadHospitalPage(0);
  },

  loadCities(province) {
    wx.request({
      url: app.globalData.baseUrl + '/api/hospitals/cities?province=' + encodeURIComponent(province),
      method: 'GET',
      success: (res) => {
        this.setData({ cities: Array.isArray(res.data) ? res.data : [] });
      }
    });
  },

  bindCityChange(e) {
    const index = Number(e.detail.value);
    const city = this.data.cities[index] || '';
    this.setData({
      cityIndex: index,
      city: city,
      districts: [],
      districtIndex: -1,
      district: ''
    });
    if (city) this.loadDistricts(this.data.province, city);
    this.loadHospitalPage(0);
  },

  loadDistricts(province, city) {
    wx.request({
      url: app.globalData.baseUrl + '/api/hospitals/districts?province=' + encodeURIComponent(province)
        + '&city=' + encodeURIComponent(city),
      method: 'GET',
      success: (res) => {
        this.setData({ districts: Array.isArray(res.data) ? res.data : [] });
      }
    });
  },

  bindDistrictChange(e) {
    const index = Number(e.detail.value);
    const district = this.data.districts[index] || '';
    this.setData({ districtIndex: index, district: district });
    this.loadHospitalPage(0);
  },

  bindKeywordInput(e) {
    this.setData({ hospitalKeyword: e.detail.value });
  },

  searchHospitals() {
    this.loadHospitalPage(0);
  },

  prevHospitalPage() {
    if (this.data.hospitalPage <= 0) return;
    this.loadHospitalPage(this.data.hospitalPage - 1);
  },

  nextHospitalPage() {
    if (this.data.hospitalPage + 1 >= this.data.hospitalTotalPages) return;
    this.loadHospitalPage(this.data.hospitalPage + 1);
  },

  loadHospitalPage(page) {
    const params = ['page=' + page, 'size=10'];
    if (this.data.province) params.push('province=' + encodeURIComponent(this.data.province));
    if (this.data.city) params.push('city=' + encodeURIComponent(this.data.city));
    if (this.data.district) params.push('district=' + encodeURIComponent(this.data.district));
    const keyword = (this.data.hospitalKeyword || '').trim();
    if (keyword) params.push('keyword=' + encodeURIComponent(keyword));

    wx.request({
      url: app.globalData.baseUrl + '/api/hospitals/page?' + params.join('&'),
      method: 'GET',
      success: (res) => {
        const data = res.data || {};
        const list = data.content || [];
        this.setData({
          hospitalList: list,
          hospitalPage: data.currentPage || 0,
          hospitalTotalPages: Math.max(1, data.totalPages || 1),
          hospitalTotal: data.totalElements || 0
        });
        if (!this.data.hospitalId && list.length) {
          this.setData({ hospitalId: list[0].hospitalId, hospitalName: list[0].hospitalName });
          this.loadEmergencyStatus(list[0].hospitalId);
        }
      }
    });
  },

  chooseHospital(e) {
    const id = e.currentTarget.dataset.id;
    const name = e.currentTarget.dataset.name;
    if (!id) return;
    this.setData({
      hospitalId: id,
      hospitalName: name,
      lockInfo: null,
      pickerOpen: false
    });
    this.loadEmergencyStatus(id);
  },

  loadEmergencyStatus(hospitalId) {
    wx.request({
      url: app.globalData.baseUrl + '/api/emergency/status/' + hospitalId,
      method: 'GET',
      header: this.authHeader(),
      success: (res) => {
        if (res.data && res.data.success) {
          const data = res.data;
          this.setData({
            lockInfo: {
              locked: !!data.locked,
              lockReason: data.lockReason || '',
              lockUntil: String(data.lockUntil || '').replace('T', ' '),
              todayWrites: data.todayWrites || 0
            }
          });
        }
      }
    });
  },

  freezeHospital() {
    const hospitalId = this.data.hospitalId;
    if (!hospitalId) return;
    wx.showModal({
      title: '冻结应急通道',
      content: '将终止本院所有应急权限，并冻结 24 小时。',
      confirmText: '冻结',
      cancelText: '取消',
      success: (modalRes) => {
        if (!modalRes.confirm) return;
        wx.showLoading({ title: '处理中...' });
        wx.request({
          url: app.globalData.baseUrl + '/api/emergency/revoke-all/' + hospitalId,
          method: 'POST',
          header: this.authHeader(),
          success: (res) => {
            wx.hideLoading();
            if (res.data && res.data.success) {
              wx.showToast({ title: '已冻结', icon: 'success' });
              this.loadEmergencyStatus(hospitalId);
            } else {
              wx.showModal({
                title: '操作失败',
                content: (res.data && res.data.message) || '请稍后重试。',
                showCancel: false
              });
            }
          },
          fail: () => {
            wx.hideLoading();
            wx.showModal({ title: '网络超时', content: '请检查网络后重试。', showCancel: false });
          }
        });
      }
    });
  },

  unfreezeHospital() {
    const hospitalId = this.data.hospitalId;
    if (!hospitalId) return;
    wx.showModal({
      title: '解除冻结',
      content: '解除后本院可重新申请应急权限。',
      confirmText: '解除',
      cancelText: '取消',
      success: (modalRes) => {
        if (!modalRes.confirm) return;
        wx.showLoading({ title: '处理中...' });
        wx.request({
          url: app.globalData.baseUrl + '/api/emergency/unfreeze/' + hospitalId,
          method: 'POST',
          header: this.authHeader(),
          success: (res) => {
            wx.hideLoading();
            if (res.data && res.data.success) {
              wx.showToast({ title: '已解除', icon: 'success' });
              this.loadEmergencyStatus(hospitalId);
            } else {
              wx.showModal({
                title: '操作失败',
                content: (res.data && res.data.message) || '请稍后重试。',
                showCancel: false
              });
            }
          },
          fail: () => {
            wx.hideLoading();
            wx.showModal({ title: '网络超时', content: '请检查网络后重试。', showCancel: false });
          }
        });
      }
    });
  },

  loadSupervisorPending() {
    wx.request({
      url: app.globalData.baseUrl + '/api/emergency/supervisor/pending',
      method: 'GET',
      header: this.authHeader(),
      success: (res) => {
        if (res.data && res.data.success) {
          this.setData({ supervisorPending: res.data.data || [] });
        }
      }
    });
  },

  previewEmergencyCert(e) {
    const requestId = e.currentTarget.dataset.requestId;
    const type = e.currentTarget.dataset.type;
    if (!requestId || !type) return;
    const authToken = wx.getStorageSync('authToken') || wx.getStorageSync('token') || '';

    wx.showLoading({ title: '加载证件...' });
    wx.downloadFile({
      url: app.globalData.baseUrl + '/api/emergency/cert/' + requestId + '/' + type,
      header: authToken ? { 'X-Auth-Token': authToken } : {},
      success: (res) => {
        wx.hideLoading();
        if (res.statusCode === 200 && res.tempFilePath) {
          wx.previewImage({ urls: [res.tempFilePath] });
        } else {
          wx.showModal({
            title: '无法查看',
            content: '证件不存在或当前账号无权查看。',
            showCancel: false
          });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showModal({
          title: '网络超时',
          content: '证件加载失败，请检查网络后重试。',
          showCancel: false
        });
      }
    });
  },

  confirmSupervisorRequest(e) {
    const requestId = e.currentTarget.dataset.id;
    if (!requestId) return;
    wx.showModal({
      title: '确认归档',
      content: '请先查看身份证与医师资格证，确认证件与本人一致、报告无异常。确认后归档。',
      confirmText: '确认归档',
      cancelText: '取消',
      success: (modalRes) => {
        if (!modalRes.confirm) return;
        wx.showLoading({ title: '提交中...' });
        wx.request({
          url: app.globalData.baseUrl + '/api/emergency/supervisor/' + requestId + '/confirm',
          method: 'POST',
          header: this.authHeader(),
          data: { certChecked: true, remark: '卫健委确认归档' },
          success: (res) => {
            wx.hideLoading();
            if (res.data && res.data.success) {
              wx.showToast({ title: '已确认归档', icon: 'success' });
              this.loadSupervisorPending();
            } else {
              wx.showModal({
                title: '操作失败',
                content: (res.data && res.data.message) || '请稍后重试。',
                showCancel: false
              });
            }
          },
          fail: () => {
            wx.hideLoading();
            wx.showModal({
              title: '网络超时',
              content: '请检查网络后重试。',
              showCancel: false
            });
          }
        });
      }
    });
  },

  rejectSupervisorRequest(e) {
    const requestId = e.currentTarget.dataset.id;
    if (!requestId) return;
    wx.showModal({
      title: '驳回异常',
      content: '驳回后本院应急通道将被冻结，请填写原因。',
      editable: true,
      placeholderText: '请输入驳回原因',
      confirmText: '驳回',
      cancelText: '取消',
      success: (modalRes) => {
        if (!modalRes.confirm) return;
        const reason = (modalRes.content || '').trim();
        if (reason.length < 2) {
          wx.showModal({ title: '请填写原因', content: '驳回原因至少 2 个字。', showCancel: false });
          return;
        }
        wx.showLoading({ title: '提交中...' });
        wx.request({
          url: app.globalData.baseUrl + '/api/emergency/supervisor/' + requestId + '/reject',
          method: 'POST',
          header: this.authHeader(),
          data: { reason: reason },
          success: (res) => {
            wx.hideLoading();
            if (res.data && res.data.success) {
              wx.showToast({ title: '已驳回并冻结', icon: 'success' });
              this.loadSupervisorPending();
            } else {
              wx.showModal({
                title: '操作失败',
                content: (res.data && res.data.message) || '请稍后重试。',
                showCancel: false
              });
            }
          },
          fail: () => {
            wx.hideLoading();
            wx.showModal({
              title: '网络超时',
              content: '请检查网络后重试。',
              showCancel: false
            });
          }
        });
      }
    });
  }
});
