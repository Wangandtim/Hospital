const app = getApp()
const deptIcon = require('../../utils/dept-icon')

Page({
  data: {
    deptIcon: deptIcon.iconPath(''),
    formData: {
      username: '',
      password: '',
      name: '',
      gender: '',
      hospitalId: '',
      deptId: '',
      title: '',
      expertise: '',
      consultationFee: '30',
      certificateNo: '',
      certificateImage: ''
    },
    genders: ['男', '女'],
    genderIndex: -1,
    hospitals: [],
    hospitalNames: [],
    hospitalIndex: -1,
    selectedHospitalName: '',
    departments: [],
    departmentNames: [],
    departmentIndex: -1,
    selectedDepartmentName: '',
    titles: ['主任医师', '副主任医师', '主治医师', '住院医师', '其他'],
    titleIndex: -1,
    certificateImage: ''
  },

  onLoad: function () {
    wx.showModal({
      title: '医生注册已下线',
      content: '请联系所属医院管理员开通账号',
      showCancel: false,
      success: () => {
        wx.redirectTo({ url: '/pages/login/login' })
      }
    })
  },

  goBack() {
    wx.navigateBack();
  },

  loadHospitals: function () {
    wx.request({
      url: `${app.globalData.baseUrl}/api/hospitals`,
      method: 'GET',
      success: (res) => {
        if (res.data && Array.isArray(res.data)) {
          const hospitals = res.data
          const hospitalNames = hospitals.map(h => h.hospitalName)
          this.setData({ hospitals, hospitalNames })
        }
      }
    })
  },

  loadDepartments: function (hospitalId) {
    wx.request({
      url: `${app.globalData.baseUrl}/api/departments/hospital/${hospitalId}`,
      method: 'GET',
      success: (res) => {
        if (res.data && Array.isArray(res.data)) {
          const departments = res.data
          const departmentNames = departments.map(d => d.deptName)
          this.setData({ 
            departments, 
            departmentNames,
            departmentIndex: -1,
            selectedDepartmentName: '',
            deptIcon: deptIcon.iconPath(''),
            'formData.deptId': ''
          })
        }
      }
    })
  },

  onUsernameInput: function (e) {
    this.setData({ 'formData.username': e.detail.value })
  },

  onPasswordInput: function (e) {
    this.setData({ 'formData.password': e.detail.value })
  },

  onNameInput: function (e) {
    this.setData({ 'formData.name': e.detail.value })
  },

  onGenderChange: function (e) {
    const index = parseInt(e.detail.value)
    this.setData({ 
      genderIndex: index,
      'formData.gender': this.data.genders[index]
    })
  },

  onHospitalChange: function (e) {
    const index = parseInt(e.detail.value)
    const hospital = this.data.hospitals[index]
    this.setData({ 
      hospitalIndex: index,
      'formData.hospitalId': hospital ? hospital.hospitalId : '',
      selectedHospitalName: hospital ? hospital.hospitalName : ''
    })
    
    if (hospital) {
      this.loadDepartments(hospital.hospitalId)
    }
  },

  onDepartmentChange: function (e) {
    const index = parseInt(e.detail.value)
    const department = this.data.departments[index]
    this.setData({ 
      departmentIndex: index,
      'formData.deptId': department ? department.deptId : '',
      selectedDepartmentName: department ? department.deptName : '',
      deptIcon: deptIcon.iconPath(department ? department.deptName : '')
    })
  },

  onTitleChange: function (e) {
    const index = parseInt(e.detail.value)
    this.setData({ 
      titleIndex: index,
      'formData.title': this.data.titles[index]
    })
  },

  onExpertiseInput: function (e) {
    this.setData({ 'formData.expertise': e.detail.value })
  },

  onFeeInput: function (e) {
    this.setData({ 'formData.consultationFee': e.detail.value })
  },

  onCertificateInput: function (e) {
    this.setData({ 'formData.certificateNo': e.detail.value })
  },

  chooseImage: function () {
    wx.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const tempFilePath = res.tempFilePaths[0]
        this.setData({ 
          certificateImage: tempFilePath,
          'formData.certificateImage': tempFilePath
        })
        
        wx.showLoading({ title: '正在识别...' })
        setTimeout(() => {
          wx.hideLoading()
          wx.showToast({ title: '识别成功', icon: 'success' })
        }, 1500)
      }
    })
  },

  handleRegister: function () {
    const data = this.data.formData
    
    if (!data.username || !data.password || !data.name) {
      wx.showToast({ title: '请填写用户名、密码和姓名', icon: 'none' })
      return
    }
    
    if (this.data.genderIndex < 0) {
      wx.showToast({ title: '请选择性别', icon: 'none' })
      return
    }
    
    if (!data.hospitalId) {
      wx.showToast({ title: '请选择所属医院', icon: 'none' })
      return
    }
    
    if (!data.deptId) {
      wx.showToast({ title: '请选择科室', icon: 'none' })
      return
    }
    
    if (this.data.titleIndex < 0) {
      wx.showToast({ title: '请选择职称', icon: 'none' })
      return
    }
    
    if (!data.certificateNo) {
      wx.showToast({ title: '请填写执业证书编号', icon: 'none' })
      return
    }
    
    if (!this.data.certificateImage) {
      wx.showToast({ title: '请上传医师资格证', icon: 'none' })
      return
    }

    wx.showLoading({ title: '提交申请中...' })

    wx.request({
      url: `${app.globalData.baseUrl}/api/doctor-self/register`,
      method: 'POST',
      data: data,
      success: (res) => {
        wx.hideLoading()
        if (res.data && res.data.success) {
          wx.showModal({
            title: '提交成功',
            content: '您的申请已提交，请等待医院管理员审核',
            showCancel: false,
            success: () => {
              wx.redirectTo({ url: '/pages/login/login' })
            }
          })
        } else {
          wx.showToast({ title: res.data?.message || '提交失败', icon: 'none' })
        }
      },
      fail: () => {
        wx.hideLoading()
        wx.showToast({ title: '网络异常', icon: 'none' })
      }
    })
  },

  goToLogin: function () {
    wx.navigateTo({ url: '/pages/login/login' })
  }
})