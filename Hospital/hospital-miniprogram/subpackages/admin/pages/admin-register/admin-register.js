const app = getApp();
const config = require('../../../../config/index');

Page({
  data: {
    form: {
      username: '',
      password: '',
      confirmPassword: '',
      realName: '',
      phone: '',
      role: 'HOSPITAL_ADMIN',
      hospitalId: ''
    },
    hospitalList: [],
    hospitalNames: [],
    selectedHospitalName: '',
    idCardImage: '',
    workCardImage: '',
    showSuccess: false,
    apiBase: config.apiBase + '/api'
  },

  onLoad() {
    this.loadHospitals();
  },

  loadHospitals() {
    wx.request({
      url: this.data.apiBase + '/hospitals',
      method: 'GET',
      success: (res) => {
        if (Array.isArray(res.data)) {
          const hospitals = res.data.filter(h => h.status === 1);
          this.setData({
            hospitalList: hospitals,
            hospitalNames: hospitals.map(h => h.hospitalName)
          });
        }
      }
    });
  },

  onUsernameInput(e) {
    this.setData({ 'form.username': e.detail.value });
  },

  onPasswordInput(e) {
    this.setData({ 'form.password': e.detail.value });
  },

  onConfirmPasswordInput(e) {
    this.setData({ 'form.confirmPassword': e.detail.value });
  },

  onRealNameInput(e) {
    this.setData({ 'form.realName': e.detail.value });
  },

  onPhoneInput(e) {
    this.setData({ 'form.phone': e.detail.value });
  },

  onHospitalChange(e) {
    const index = e.detail.value;
    const hospital = this.data.hospitalList[index];
    this.setData({
      'form.hospitalId': hospital.hospitalId,
      selectedHospitalName: hospital.hospitalName
    });
  },

  chooseIdCardImage() {
    wx.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const tempFilePath = res.tempFilePaths[0];
        this.setData({ idCardImage: tempFilePath });
        this.uploadImage(tempFilePath, 'idCard');
      }
    });
  },

  chooseWorkCardImage() {
    wx.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const tempFilePath = res.tempFilePaths[0];
        this.setData({ workCardImage: tempFilePath });
        this.uploadImage(tempFilePath, 'workCard');
      }
    });
  },

  uploadImage(filePath, type) {
    wx.showLoading({ title: '上传中...' });
    const token = wx.getStorageSync('token') || wx.getStorageSync('authToken');
    
    wx.uploadFile({
      url: this.data.apiBase + '/upload',
      filePath: filePath,
      name: 'file',
      header: token ? { 'X-Auth-Token': token } : {},
      success: (res) => {
        wx.hideLoading();
        try {
          const data = JSON.parse(res.data);
          if (res.statusCode >= 200 && res.statusCode < 300 && data.success && data.url) {
            if (type === 'idCard') {
              this.setData({ 'form.idCardImage': data.url });
            } else {
              this.setData({ 'form.workCardImage': data.url });
            }
            wx.showToast({ title: '上传成功', icon: 'success' });
          } else {
            wx.showToast({ title: data.message || '上传失败', icon: 'none' });
          }
        } catch (e) {
          console.error('上传解析失败', e);
          wx.showToast({ title: '上传失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({
          title: '上传失败',
          icon: 'none'
        });
      }
    });
  },

  handleRegister() {
    const { form } = this.data;

    if (!form.username) {
      wx.showToast({ title: '请输入用户名', icon: 'none' });
      return;
    }

    if (!form.password) {
      wx.showToast({ title: '请输入密码', icon: 'none' });
      return;
    }

    if (form.password !== form.confirmPassword) {
      wx.showToast({ title: '两次输入的密码不一致', icon: 'none' });
      return;
    }

    if (!form.realName) {
      wx.showToast({ title: '请输入真实姓名', icon: 'none' });
      return;
    }

    if (!form.hospitalId) {
      wx.showToast({ title: '请选择所属医院', icon: 'none' });
      return;
    }

    if (!this.data.idCardImage) {
      wx.showToast({ title: '请上传身份证照片', icon: 'none' });
      return;
    }

    if (!this.data.workCardImage) {
      wx.showToast({ title: '请上传工作证照片', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '提交中...' });

    wx.request({
      url: this.data.apiBase + '/admin/create',
      method: 'POST',
      data: {
        username: form.username,
        password: form.password,
        realName: form.realName,
        phone: form.phone,
        role: 'HOSPITAL_ADMIN',
        hospitalId: form.hospitalId,
        status: 1,
        auditStatus: 0,
        idCardImage: form.idCardImage || '',
        workCardImage: form.workCardImage || ''
      },
      success: (res) => {
        wx.hideLoading();
        if (res.data.success) {
          this.setData({ showSuccess: true });
        } else {
          wx.showToast({
            title: res.data.message || '注册失败',
            icon: 'none',
            duration: 2000
          });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({
          title: '网络请求失败',
          icon: 'none',
          duration: 2000
        });
      }
    });
  },

  goToLogin() {
    wx.redirectTo({
      url: '/subpackages/admin/pages/admin-login/admin-login'
    });
  },

  goBack() {
    wx.navigateBack();
  }
})