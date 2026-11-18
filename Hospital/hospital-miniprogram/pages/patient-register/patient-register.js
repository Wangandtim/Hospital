const app = getApp();

Page({
  data: {
    formData: {
      phone: '',
      password: '',
      confirmPassword: '',
      name: '',
      gender: '',
      age: '',
      idCard: '',
      medicalHistory: ''
    },
    genders: ['男', '女'],
    genderIndex: -1
  },

  onLoad() {
  },

  goBack() {
    wx.navigateBack();
  },

  onPhoneInput(e) {
    this.setData({ 'formData.phone': e.detail.value });
  },

  onPasswordInput(e) {
    this.setData({ 'formData.password': e.detail.value });
  },

  onConfirmPasswordInput(e) {
    this.setData({ 'formData.confirmPassword': e.detail.value });
  },

  onNameInput(e) {
    this.setData({ 'formData.name': e.detail.value });
  },

  onGenderChange(e) {
    const index = parseInt(e.detail.value);
    this.setData({
      genderIndex: index,
      'formData.gender': this.data.genders[index]
    });
  },

  onAgeInput(e) {
    this.setData({ 'formData.age': e.detail.value });
  },

  onIdCardInput(e) {
    this.setData({ 'formData.idCard': e.detail.value });
  },

  onMedicalHistoryInput(e) {
    this.setData({ 'formData.medicalHistory': e.detail.value });
  },

  handleRegister() {
    const { phone, password, confirmPassword, name, gender } = this.data.formData;

    if (!phone) {
      wx.showToast({ title: '请输入手机号', icon: 'none' });
      return;
    }

    if (!/^1[3-9]\d{9}$/.test(phone)) {
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' });
      return;
    }

    if (!password) {
      wx.showToast({ title: '请输入密码', icon: 'none' });
      return;
    }

    if (password.length < 6) {
      wx.showToast({ title: '密码至少6位', icon: 'none' });
      return;
    }

    if (password !== confirmPassword) {
      wx.showToast({ title: '两次输入的密码不一致', icon: 'none' });
      return;
    }

    if (!name) {
      wx.showToast({ title: '请输入姓名', icon: 'none' });
      return;
    }

    if (this.data.genderIndex < 0) {
      wx.showToast({ title: '请选择性别', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '注册中...' });

    wx.request({
      url: app.globalData.baseUrl + '/api/patients/register',
      method: 'POST',
      header: {
        'content-type': 'application/json'
      },
      data: this.data.formData,
      success: (res) => {
        wx.hideLoading();
        if (res.data && res.data.success) {
          wx.showModal({
            title: '注册成功',
            content: '注册成功，请登录',
            showCancel: false,
            success: () => {
              wx.navigateTo({ url: '/pages/login/login' });
            }
          });
        } else {
          wx.showToast({ title: res.data?.message || '注册失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '网络异常', icon: 'none' });
      }
    });
  },

  goToLogin() {
    wx.navigateTo({ url: '/pages/login/login' });
  }
})