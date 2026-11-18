const app = getApp();
const elderly = require('../../utils/elderly');

Page({
  data: {
    selectedRole: 'patient',
    focusField: null,
    elderly: false,
    doctorUsername: '',
    doctorPassword: '',
    patientPhone: '',
    patientPassword: '',
    patientLoginMode: 'password',
    smsCode: '',
    codeCountdown: 0
  },

  onLoad() {
    this.setData({ elderly: elderly.isElderly() });
    const token = wx.getStorageSync('token');
    const role = wx.getStorageSync('userRole');
    if (token && role) {
      this.navigateByRole(role);
    }
  },

  onFieldFocus(e) {
    const field = e.currentTarget.dataset.field;
    if (field) {
      this.setData({ focusField: field });
    }
  },

  onFieldBlur() {
    this.setData({ focusField: null });
  },

  onUnload() {
    this.clearCodeTimer();
  },

  selectRole(e) {
    const role = e.currentTarget.dataset.role;
    if (!role || role === this.data.selectedRole) {
      return;
    }
    this.clearCodeTimer();
    this.setData({ 
      selectedRole: role,
      doctorUsername: '',
      doctorPassword: '',
      patientPhone: '',
      patientPassword: '',
      patientLoginMode: 'password',
      smsCode: '',
      codeCountdown: 0
    });
  },

  togglePatientLoginMode() {
    const nextMode = this.data.patientLoginMode === 'password' ? 'code' : 'password';
    this.setData({
      patientLoginMode: nextMode,
      focusField: null
    });
  },

  onSmsCodeInput(e) {
    this.setData({ smsCode: e.detail.value });
  },

  clearCodeTimer() {
    if (this.codeTimer) {
      clearInterval(this.codeTimer);
      this.codeTimer = null;
    }
  },

  sendSmsCode() {
    if (this.data.codeCountdown > 0) {
      return;
    }
    if (!app.globalData.baseUrl) {
      wx.showToast({ title: '接口地址未配置，请检查 config/index.js', icon: 'none' });
      return;
    }
    const phone = this.data.patientPhone;
    if (!/^1[3-9]\d{9}$/.test(phone)) {
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '发送中...', mask: false });
    wx.request({
      url: app.globalData.baseUrl + '/api/patients/send-code',
      method: 'POST',
      timeout: 10000,
      data: { phone: phone },
      success: (res) => {
        wx.hideLoading();
        if (res.statusCode === 200 && res.data.success) {
          wx.showToast({
            title: res.data.devCode ? ('验证码：' + res.data.devCode) : '验证码已发送',
            icon: 'none',
            duration: 3000
          });
          this.startCodeCountdown();
        } else {
          wx.showToast({
            title: (res.data && res.data.message) || '发送失败',
            icon: 'none'
          });
        }
      },
      fail: (err) => {
        wx.hideLoading();
        console.error('[login] send-code failed', err);
        wx.showToast({ title: '网络异常，请稍后重试', icon: 'none' });
      }
    });
  },

  startCodeCountdown() {
    this.clearCodeTimer();
    this.setData({ codeCountdown: 60 });
    this.codeTimer = setInterval(() => {
      const next = this.data.codeCountdown - 1;
      if (next <= 0) {
        this.clearCodeTimer();
        this.setData({ codeCountdown: 0 });
      } else {
        this.setData({ codeCountdown: next });
      }
    }, 1000);
  },

  onUsernameInput(e) {
    this.setData({ doctorUsername: e.detail.value });
  },

  onPasswordInput(e) {
    this.setData({ doctorPassword: e.detail.value });
  },

  onPatientPasswordInput(e) {
    this.setData({ patientPassword: e.detail.value });
  },

  onPhoneInput(e) {
    this.setData({ patientPhone: e.detail.value });
  },

  handleLogin() {
    if (this.loggingIn) {
      return;
    }
    if (!this.data.selectedRole) {
      wx.showToast({
        title: '请选择身份',
        icon: 'none'
      });
      return;
    }

    if (this.data.selectedRole === 'doctor') {
      if (!this.data.doctorUsername || !this.data.doctorPassword) {
        wx.showToast({
          title: '请输入账号和密码',
          icon: 'none'
        });
        return;
      }
      this.doctorLogin();
    } else {
      if (!this.data.patientPhone) {
        wx.showToast({
          title: '请输入手机号',
          icon: 'none'
        });
        return;
      }
      if (this.data.patientLoginMode === 'code') {
        if (!this.data.smsCode) {
          wx.showToast({
            title: '请输入验证码',
            icon: 'none'
          });
          return;
        }
      } else if (!this.data.patientPassword) {
        wx.showToast({
          title: '请输入密码',
          icon: 'none'
        });
        return;
      }
      this.patientLogin();
    }
  },

  patientLogin() {
    if (!app.globalData.baseUrl) {
      wx.showToast({ title: '接口地址未配置，请检查 config/index.js', icon: 'none' });
      return;
    }
    this.loggingIn = true;
    wx.showLoading({ title: '登录中...', mask: false });

    const phone = this.data.patientPhone;
    const useCode = this.data.patientLoginMode === 'code';

    wx.request({
      url: app.globalData.baseUrl + (useCode ? '/api/patients/login-by-code' : '/api/patients/login'),
      method: 'POST',
      timeout: 10000,
      data: useCode
        ? { phone: phone, code: this.data.smsCode }
        : { phone: phone, password: this.data.patientPassword },
      success: (res) => {
        this.loggingIn = false;
        wx.hideLoading();

        if (res.statusCode === 200 && res.data.success) {
          const token = res.data.token || '';
          
          wx.setStorageSync('token', token);
          wx.setStorageSync('authToken', token);
          wx.setStorageSync('userRole', 'patient');
          wx.setStorageSync('patientId', res.data.patientId);
          wx.setStorageSync('patientPhone', res.data.phone);
          wx.setStorageSync('patientName', res.data.patientName || '');
          wx.setStorageSync('hasCompleteInfo', res.data.hasCompleteInfo || false);
          
          app.globalData.token = token;
          app.globalData.userRole = 'patient';
          app.globalData.patientId = res.data.patientId;
          app.globalData.hasPatientInfo = res.data.hasCompleteInfo || false;

          wx.showToast({
            title: '登录成功',
            icon: 'success'
          });

          setTimeout(() => {
            this.navigateByRole('patient');
          }, 600);
        } else {
          wx.showToast({
            title: res.data.message || '登录失败',
            icon: 'none',
            duration: 2000
          });
        }
      },
      fail: (err) => {
        this.loggingIn = false;
        wx.hideLoading();
        console.error('[login] patient login failed', err);
        wx.showToast({
          title: '网络异常，请稍后重试',
          icon: 'none',
          duration: 2000
        });
      }
    });
  },

  doctorLogin() {
    if (!app.globalData.baseUrl) {
      wx.showToast({ title: '接口地址未配置，请检查 config/index.js', icon: 'none' });
      return;
    }
    this.loggingIn = true;
    wx.showLoading({ title: '登录中...', mask: false });

    wx.request({
      url: app.globalData.baseUrl + '/api/doctor-self/login',
      method: 'POST',
      timeout: 10000,
      data: {
        username: this.data.doctorUsername,
        password: this.data.doctorPassword
      },
      success: (res) => {
        this.loggingIn = false;
        wx.hideLoading();
        
        if (res.statusCode === 200 && res.data.success) {
          const token = res.data.token || '';
          wx.setStorageSync('token', token);
          wx.setStorageSync('authToken', token);
          wx.setStorageSync('userRole', 'doctor');
          wx.setStorageSync('doctorId', res.data.doctorId);
          wx.setStorageSync('doctorName', res.data.doctorName);
          wx.setStorageSync('doctorUsername', this.data.doctorUsername);
          wx.setStorageSync('hospitalId', res.data.hospitalId);
          
          app.globalData.token = res.data.token;
          app.globalData.userRole = 'doctor';
          app.globalData.doctorId = res.data.doctorId;
          app.globalData.doctorName = res.data.doctorName;
          app.globalData.hospitalId = res.data.hospitalId;

          wx.showToast({
            title: '登录成功',
            icon: 'success'
          });

          setTimeout(() => {
            this.navigateByRole('doctor');
          }, 600);
        } else {
          wx.showToast({
            title: res.data.message || '账号或密码错误',
            icon: 'none',
            duration: 2000
          });
        }
      },
      fail: (err) => {
        this.loggingIn = false;
        wx.hideLoading();
        console.error('[login] doctor login failed', err);
        wx.showToast({
          title: '登录失败，请稍后重试',
          icon: 'none',
          duration: 2000
        });
      }
    });
  },

  navigateByRole(role) {
    if (role === 'patient') {
      wx.switchTab({
        url: '/pages/index/index'
      });
    } else if (role === 'doctor') {
      wx.redirectTo({
        url: '/pages/doctor-workspace/doctor-workspace'
      });
    } else if (role === 'admin') {
      const adminRole = wx.getStorageSync('adminRole');
      wx.redirectTo({
        url: adminRole === 'SUPER_ADMIN'
          ? '/subpackages/admin/pages/admin-dashboard/admin-dashboard'
          : '/subpackages/admin/pages/hospital-admin-dashboard/hospital-admin-dashboard'
      });
    }
  },

  goToAdminLogin() {
    wx.redirectTo({
      url: '/subpackages/admin/pages/admin-login/admin-login'
    });
  },

  goToPatientRegister() {
    wx.navigateTo({
      url: '/pages/patient-register/patient-register'
    });
  },

  goToDoctorRegister() {
    wx.navigateTo({
      url: '/pages/doctor-register/doctor-register'
    });
  },

  goToEmergencyLogin() {
    wx.redirectTo({
      url: '/subpackages/admin/pages/emergency-login/emergency-login'
    });
  }
})
