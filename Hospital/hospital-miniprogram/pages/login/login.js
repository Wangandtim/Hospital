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
    codeCountdown: 0,
    errors: { phone: '', password: '', code: '', username: '', form: '' }
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

  onFieldBlur(e) {
    const field = e && e.currentTarget ? e.currentTarget.dataset.field : null;
    if (field === 'phone') {
      const value = this.data.patientPhone;
      this.setData({ 'errors.phone': value && !/^1[3-9]\d{9}$/.test(value) ? '手机号格式不正确' : '' });
    } else if (field === 'code') {
      const value = this.data.smsCode;
      this.setData({ 'errors.code': value && String(value).length !== 6 ? '验证码为 6 位数字' : '' });
    }
    this.setData({ focusField: null });
  },

  clearErrors() {
    this.setData({ errors: { phone: '', password: '', code: '', username: '', form: '' } });
  },

  setErrors(map) {
    const patch = {};
    Object.keys(map).forEach(key => { patch['errors.' + key] = map[key]; });
    this.setData(patch);
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
      codeCountdown: 0,
      errors: { phone: '', password: '', code: '', username: '', form: '' }
    });
  },

  togglePatientLoginMode() {
    const nextMode = this.data.patientLoginMode === 'password' ? 'code' : 'password';
    this.setData({
      patientLoginMode: nextMode,
      focusField: null,
      errors: { phone: '', password: '', code: '', username: '', form: '' }
    });
  },

  onSmsCodeInput(e) {
    this.setData({ smsCode: e.detail.value, 'errors.code': '', 'errors.form': '' });
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
      this.setErrors({ form: '接口地址未配置，请检查 config/index.js' });
      return;
    }
    const phone = this.data.patientPhone;
    if (!/^1[3-9]\d{9}$/.test(phone)) {
      this.setErrors({ phone: '请输入正确的手机号', form: '' });
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
          this.setErrors({ form: (res.data && res.data.message) || '发送失败，请稍后重试' });
        }
      },
      fail: (err) => {
        wx.hideLoading();
        console.error('[login] send-code failed', err);
        this.setErrors({ form: '网络异常，请稍后重试' });
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
    this.setData({ doctorUsername: e.detail.value, 'errors.username': '', 'errors.form': '' });
  },

  onPasswordInput(e) {
    this.setData({ doctorPassword: e.detail.value, 'errors.password': '', 'errors.form': '' });
  },

  onPatientPasswordInput(e) {
    this.setData({ patientPassword: e.detail.value, 'errors.password': '', 'errors.form': '' });
  },

  onPhoneInput(e) {
    this.setData({ patientPhone: e.detail.value, 'errors.phone': '', 'errors.form': '' });
  },

  wechatLogin() {
    if (this.loggingIn) {
      return;
    }
    this.loggingIn = true;
    wx.showLoading({ title: '微信登录中...', mask: false });

    // 每次点击都重新取 code：code 只能使用一次，失败后再次点击会拿到新的 code
    wx.login({
      success: (loginRes) => {
        if (!loginRes.code) {
          this.finishWechatLogin();
          wx.showToast({ title: '未获取到微信授权码，请重试', icon: 'none' });
          return;
        }
        this.requestWechatLogin(loginRes.code);
      },
      fail: (err) => {
        this.finishWechatLogin();
        console.error('[login] wx.login failed', err);
        wx.showToast({ title: '微信授权失败，请重试', icon: 'none' });
      }
    });
  },

  requestWechatLogin(code) {
    if (!app.globalData.baseUrl) {
      this.finishWechatLogin();
      wx.showToast({ title: '接口地址未配置，请检查 config/index.js', icon: 'none' });
      return;
    }

    wx.request({
      url: app.globalData.baseUrl + '/api/wechat/login',
      method: 'POST',
      timeout: 10000,
      data: { code },
      success: (res) => {
        this.finishWechatLogin();
        const body = res.data || {};
        if (res.statusCode === 200 && body.success) {
          this.applyPatientLoginResult(body);
        } else {
          wx.showToast({
            title: body.message || '微信登录失败，请重试',
            icon: 'none',
            duration: 2500
          });
        }
      },
      fail: (err) => {
        this.finishWechatLogin();
        console.error('[login] wechat login failed', err);
        wx.showToast({ title: '网络异常，请稍后重试', icon: 'none' });
      }
    });
  },

  finishWechatLogin() {
    this.loggingIn = false;
    wx.hideLoading();
  },

  applyPatientLoginResult(body) {
    const token = body.token || '';

    wx.setStorageSync('token', token);
    wx.setStorageSync('authToken', token);
    wx.setStorageSync('userRole', 'patient');
    wx.setStorageSync('patientId', body.patientId);
    wx.setStorageSync('patientPhone', body.phone || '');
    wx.setStorageSync('patientName', body.patientName || '');
    wx.setStorageSync('hasCompleteInfo', body.hasCompleteInfo || false);
    if (body.openid) {
      wx.setStorageSync('wxOpenid', body.openid);
    }

    app.globalData.token = token;
    app.globalData.userRole = 'patient';
    app.globalData.patientId = body.patientId;
    app.globalData.hasPatientInfo = body.hasCompleteInfo || false;
    app.globalData.wxOpenid = body.openid || '';

    wx.showToast({ title: '登录成功', icon: 'success' });

    setTimeout(() => {
      this.navigateByRole('patient');
    }, 600);
  },

  handleLogin() {
    if (this.loggingIn) {
      return;
    }
    this.clearErrors();
    if (!this.data.selectedRole) {
      this.setErrors({ form: '请选择身份' });
      return;
    }

    if (this.data.selectedRole === 'doctor') {
      if (!this.data.doctorUsername) {
        this.setErrors({ username: '请输入医生账号' });
        return;
      }
      if (!this.data.doctorPassword) {
        this.setErrors({ password: '请输入登录密码' });
        return;
      }
      this.doctorLogin();
    } else {
      if (!this.data.patientPhone) {
        this.setErrors({ phone: '请输入手机号' });
        return;
      }
      if (!/^1[3-9]\d{9}$/.test(this.data.patientPhone)) {
        this.setErrors({ phone: '手机号格式不正确' });
        return;
      }
      if (this.data.patientLoginMode === 'code') {
        if (!this.data.smsCode) {
          this.setErrors({ code: '请输入验证码' });
          return;
        }
        if (String(this.data.smsCode).length !== 6) {
          this.setErrors({ code: '验证码为 6 位数字' });
          return;
        }
      } else if (!this.data.patientPassword) {
        this.setErrors({ password: '请输入密码' });
        return;
      }
      this.patientLogin();
    }
  },

  patientLogin() {
    if (!app.globalData.baseUrl) {
      this.setErrors({ form: '接口地址未配置，请检查 config/index.js' });
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
          this.setErrors({ form: (res.data && res.data.message) || '登录失败，请检查手机号与密码' });
        }
      },
      fail: (err) => {
        this.loggingIn = false;
        wx.hideLoading();
        console.error('[login] patient login failed', err);
        this.setErrors({ form: '网络异常，请稍后重试' });
      }
    });
  },

  doctorLogin() {
    if (!app.globalData.baseUrl) {
      this.setErrors({ form: '接口地址未配置，请检查 config/index.js' });
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

          if (res.data.mustChangePassword) {
            wx.showToast({ title: '首次登录请先修改密码', icon: 'none' });
            setTimeout(() => {
              wx.redirectTo({
                url: '/pages/force-password/force-password?doctorId=' + res.data.doctorId
              });
            }, 1000);
            return;
          }

          wx.showToast({
            title: '登录成功',
            icon: 'success'
          });

          setTimeout(() => {
            this.navigateByRole('doctor');
          }, 600);
        } else {
          this.setErrors({ form: (res.data && res.data.message) || '账号或密码错误' });
        }
      },
      fail: (err) => {
        this.loggingIn = false;
        wx.hideLoading();
        console.error('[login] doctor login failed', err);
        this.setErrors({ form: '登录失败，请稍后重试' });
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

  goToEmergencyLogin() {
    wx.redirectTo({
      url: '/subpackages/admin/pages/emergency-login/emergency-login'
    });
  }
})
