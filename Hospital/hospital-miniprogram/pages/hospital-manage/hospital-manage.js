const app = getApp()

Page({
  data: {
    hospitals: [],
    expandedId: '',
    showModal: false,
    isEdit: false,
    canManage: false,
    searchKeyword: '',
    currentPage: 1,
    totalPages: 1,
    pageSize: 10,
    formData: {
      hospitalId: '',
      hospitalName: '',
      level: '',
      phone: '',
      address: '',
      province: '',
      city: '',
      district: ''
    }
  },

  onLoad: function (options) {
    const adminRole = wx.getStorageSync('adminRole') || '';
    // 医院准入由卫健委管理：院管只读，不出编辑/删除/新增入口
    const readOnly = (options && options.view === 'info') || adminRole === 'HOSPITAL_ADMIN';
    this.setData({ canManage: !readOnly && adminRole === 'SUPER_ADMIN' });
    this.loadHospitals();
  },

  loadHospitals: function () {
    const { currentPage, pageSize, searchKeyword } = this.data

    wx.showLoading({ title: '加载中...' })

    wx.request({
      url: `${app.globalData.baseUrl}/api/hospitals/page`,
      method: 'GET',
      data: {
        page: currentPage - 1,
        size: pageSize,
        keyword: searchKeyword
      },
      success: (res) => {
        wx.hideLoading()
        const data = res.data || {}
        if (res.statusCode >= 200 && res.statusCode < 300 && Array.isArray(data.content)) {
          this.setData({
            hospitals: data.content,
            totalPages: data.totalPages || 1,
            currentPage: (data.currentPage || 0) + 1
          })
        } else {
          this.setData({ hospitals: [], totalPages: 1 })
          wx.showToast({ title: '医院列表加载失败', icon: 'none' })
        }
      },
      fail: () => {
        wx.hideLoading()
        wx.showToast({ title: '网络异常，请稍后重试', icon: 'none' })
      }
    })
  },

  onSearchInput: function (e) {
    const keyword = e.detail.value
    this.setData({ 
      searchKeyword: keyword,
      currentPage: 1
    })
    this.loadHospitals()
  },

  prevPage: function () {
    if (this.data.currentPage > 1) {
      this.setData({ currentPage: this.data.currentPage - 1 })
      this.loadHospitals()
    }
  },

  nextPage: function () {
    if (this.data.currentPage < this.data.totalPages) {
      this.setData({ currentPage: this.data.currentPage + 1 })
      this.loadHospitals()
    }
  },

  toggleExpand: function (e) {
    const id = e.currentTarget.dataset.id
    this.setData({
      expandedId: this.data.expandedId === id ? '' : id
    })
  },

  goToDepartmentManage: function (e) {
    const hospitalId = e.currentTarget.dataset.hospitalId
    const hospitalName = e.currentTarget.dataset.hospitalName
    
    wx.setStorageSync('currentHospitalId', hospitalId)
    wx.setStorageSync('currentHospitalName', hospitalName)
    
    wx.navigateTo({
      url: `/pages/department-manage/department-manage?hospitalId=${hospitalId}&hospitalName=${encodeURIComponent(hospitalName)}`
    })
  },

  goToDoctorManage: function (e) {
    const hospitalId = e.currentTarget.dataset.hospitalId
    const hospitalName = e.currentTarget.dataset.hospitalName
    
    wx.setStorageSync('currentHospitalId', hospitalId)
    wx.setStorageSync('currentHospitalName', hospitalName)
    
    wx.navigateTo({
      url: `/pages/doctor-manage/doctor-manage?hospitalId=${hospitalId}&hospitalName=${encodeURIComponent(hospitalName)}`
    })
  },

  goToScheduleManage: function (e) {
    const hospitalId = e.currentTarget.dataset.hospitalId
    const hospitalName = e.currentTarget.dataset.hospitalName
    
    wx.setStorageSync('currentHospitalId', hospitalId)
    wx.setStorageSync('currentHospitalName', hospitalName)
    
    wx.navigateTo({
      url: `/pages/appointment-manage/appointment-manage?hospitalId=${hospitalId}&hospitalName=${encodeURIComponent(hospitalName)}`
    })
  },

  showAddModal: function () {
    this.setData({
      showModal: true,
      isEdit: false,
      formData: {
        hospitalId: '',
        hospitalName: '',
        level: '',
        phone: '',
        address: '',
        province: '',
        city: '',
        district: ''
      }
    })
  },

  hideModal: function () {
    this.setData({ showModal: false })
  },

  stopPropagation: function () {},

  editHospital: function (e) {
    const id = e.currentTarget.dataset.id
    const item = (this.data.hospitals || []).find(h => h.hospitalId === id)
    if (!item) return
    this.setData({
      showModal: true,
      isEdit: true,
      formData: {
        hospitalId: item.hospitalId,
        hospitalName: item.hospitalName,
        level: item.level,
        phone: item.phone,
        address: item.address,
        province: item.province,
        city: item.city,
        district: item.district
      }
    })
  },

  deleteHospital: function (e) {
    const id = e.currentTarget.dataset.id
    if (!this.data.canManage) return
    wx.showModal({
      title: '确认删除',
      content: '确定要删除这家医院吗？删除后相关的科室和医生数据也将被删除。',
      confirmText: '删除',
      confirmColor: '#ef4444',
      success: (res) => {
        if (res.confirm) {
          wx.request({
            url: `${app.globalData.baseUrl}/api/hospitals/${id}`,
            method: 'DELETE',
            success: (res) => {
              if (res.statusCode >= 200 && res.statusCode < 300) {
                wx.showToast({ title: '删除成功', icon: 'success' })
                this.loadHospitals()
              } else {
                wx.showModal({
                  title: '删除失败',
                  content: (res.data && res.data.message) || ('请求失败（' + res.statusCode + '）'),
                  showCancel: false
                })
              }
            },
            fail: () => {
              wx.showToast({ title: '删除失败', icon: 'none' })
            }
          })
        }
      }
    })
  },

  onNameInput: function (e) {
    this.setData({ 'formData.hospitalName': e.detail.value })
  },

  onLevelInput: function (e) {
    this.setData({ 'formData.level': e.detail.value })
  },

  onPhoneInput: function (e) {
    this.setData({ 'formData.phone': e.detail.value })
  },

  onAddressInput: function (e) {
    this.setData({ 'formData.address': e.detail.value })
  },

  onProvinceInput: function (e) {
    this.setData({ 'formData.province': e.detail.value })
  },

  onCityInput: function (e) {
    this.setData({ 'formData.city': e.detail.value })
  },

  onDistrictInput: function (e) {
    this.setData({ 'formData.district': e.detail.value })
  },

  saveHospital: function () {
    if (!this.data.canManage) return
    const data = this.data.formData
    
    if (!data.hospitalName || !data.address) {
      wx.showToast({ title: '请填写必填项', icon: 'none' })
      return
    }

    const method = this.data.isEdit ? 'PUT' : 'POST'
    const url = this.data.isEdit 
      ? `${app.globalData.baseUrl}/api/hospitals/${data.hospitalId}`
      : `${app.globalData.baseUrl}/api/hospitals`

    wx.request({
      url,
      method,
      data: data,
      success: (res) => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          wx.showToast({ 
            title: this.data.isEdit ? '修改成功' : '添加成功', 
            icon: 'success' 
          })
          this.hideModal()
          this.loadHospitals()
        } else {
          wx.showModal({
            title: '操作失败',
            content: (res.data && res.data.message) || ('请求失败（' + res.statusCode + '）'),
            showCancel: false
          })
        }
      },
      fail: () => {
        wx.showToast({ title: '操作失败', icon: 'none' })
      }
    })
  }
})