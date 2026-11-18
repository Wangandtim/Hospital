const app = getApp();

Page({
  data: {
    patientName: '',
    idCard: '',
    phone: '',
    gender: 0,
    birthday: ''
  },

  onLoad() {
    const patientInfo = wx.getStorageSync('patientInfo');
    if (patientInfo) {
      this.setData({
        patientName: patientInfo.patientName || '',
        idCard: patientInfo.idCard || '',
        phone: patientInfo.phone || '',
        gender: patientInfo.gender || 0,
        birthday: patientInfo.birthday || ''
      });
    }
  },

  onNameInput(e) {
    this.setData({ patientName: e.detail.value });
  },

  onIdCardInput(e) {
    this.setData({ idCard: e.detail.value });
  },

  onPhoneInput(e) {
    this.setData({ phone: e.detail.value });
  },

  selectGender(e) {
    const gender = parseInt(e.currentTarget.dataset.gender);
    this.setData({ gender: gender });
  },

  onDateChange(e) {
    this.setData({ birthday: e.detail.value });
  },

  submitPatientInfo() {
    const { patientName, idCard, phone, gender, birthday } = this.data;

    if (!patientName) {
      wx.showToast({
        title: '请输入姓名',
        icon: 'none'
      });
      return;
    }

    if (!idCard) {
      wx.showToast({
        title: '请输入身份证号',
        icon: 'none'
      });
      return;
    }

    if (!phone) {
      wx.showToast({
        title: '请输入手机号码',
        icon: 'none'
      });
      return;
    }

    if (gender === 0) {
      wx.showToast({
        title: '请选择性别',
        icon: 'none'
      });
      return;
    }

    if (!birthday) {
      wx.showToast({
        title: '请选择出生日期',
        icon: 'none'
      });
      return;
    }

    const patientInfo = {
      patientName,
      idCard,
      phone,
      gender,
      birthday
    };

    wx.setStorageSync('patientInfo', patientInfo);
    app.globalData.hasPatientInfo = true;
    wx.setStorageSync('hasPatientInfo', true);

    wx.showToast({
      title: '绑定成功',
      icon: 'success'
    });

    setTimeout(() => {
      wx.navigateBack();
    }, 1500);
  },

  goBack() {
    wx.navigateBack();
  }
});
