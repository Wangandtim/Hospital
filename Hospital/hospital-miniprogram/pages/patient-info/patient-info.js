const app = getApp();

Page({
  data: {
    patient: {
      name: '',
      gender: '',
      age: '',
      phone: '',
      medicalHistory: ''
    },
    genderOptions: [
      { value: 'M', label: '男' },
      { value: 'F', label: '女' }
    ],
    fromUrl: '',
    mode: 'edit'
  },

  onLoad(options) {
    this.setData({
      fromUrl: options.fromUrl || '',
      mode: options.mode || 'edit'
    });

    const patientId = wx.getStorageSync('patientId');
    const patientPhone = wx.getStorageSync('patientPhone');
    const patientName = wx.getStorageSync('patientName');

    this.setData({
      'patient.phone': patientPhone || '',
      'patient.name': patientName || ''
    });

    if (patientId) {
      this.loadPatientInfo(patientId);
    }
  },

  loadPatientInfo(patientId) {
    wx.request({
      url: app.globalData.baseUrl + '/api/patients/' + patientId,
      method: 'GET',
      success: (res) => {
        if (res.data) {
          const patient = res.data;
          this.setData({
            patient: {
              name: patient.name || '',
              gender: patient.gender || '',
              age: patient.age || '',
              phone: patient.phone || '',
              medicalHistory: patient.medicalHistory || ''
            }
          });
        }
      }
    });
  },

  onNameInput(e) {
    this.setData({ 'patient.name': e.detail.value });
  },

  onAgeInput(e) {
    this.setData({ 'patient.age': e.detail.value });
  },

  onGenderChange(e) {
    const gender = e.currentTarget.dataset.value;
    this.setData({ 'patient.gender': gender });
  },

  onMedicalHistoryInput(e) {
    this.setData({ 'patient.medicalHistory': e.detail.value });
  },

  submitForm() {
    const { patient } = this.data;

    if (!patient.name || !patient.name.trim()) {
      wx.showToast({ title: '请输入姓名', icon: 'none' });
      return;
    }

    if (!patient.gender) {
      wx.showToast({ title: '请选择性别', icon: 'none' });
      return;
    }

    if (!patient.age || parseInt(patient.age) <= 0) {
      wx.showToast({ title: '请输入有效的年龄', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '保存中...' });

    const patientId = wx.getStorageSync('patientId');

    wx.request({
      url: app.globalData.baseUrl + '/api/patients/' + patientId,
      method: 'PUT',
      data: {
        name: patient.name,
        gender: patient.gender,
        age: parseInt(patient.age),
        phone: patient.phone,
        medicalHistory: patient.medicalHistory
      },
      success: (res) => {
        wx.hideLoading();
        
        if (res.statusCode === 200) {
          wx.setStorageSync('patientName', this.data.patient.name);
          wx.setStorageSync('hasCompleteInfo', true);
          app.globalData.hasPatientInfo = true;
          
          wx.showToast({
            title: '保存成功',
            icon: 'success'
          });

          setTimeout(() => {
            if (this.data.fromUrl) {
              wx.redirectTo({
                url: decodeURIComponent(this.data.fromUrl)
              });
            } else {
              wx.switchTab({
                url: '/pages/index/index'
              });
            }
          }, 1500);
        } else {
          wx.showToast({ title: '保存失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '网络错误', icon: 'none' });
      }
    });
  },

  skip() {
    if (this.data.fromUrl) {
      wx.redirectTo({
        url: decodeURIComponent(this.data.fromUrl)
      });
    } else {
      wx.switchTab({
        url: '/pages/index/index'
      });
    }
  },

  goBack() {
    wx.navigateBack();
  },

  goToEdit() {
    wx.redirectTo({
      url: '/pages/patient-info/patient-info?mode=edit'
    });
  }
});