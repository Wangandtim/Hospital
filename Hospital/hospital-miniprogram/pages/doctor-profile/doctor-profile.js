const app = getApp();
const deptIcon = require('../../utils/dept-icon');

Page({
  data: {
    doctor: null,
    photoUrlFull: '',
    avatarChar: '医',
    department: null,
    deptIcon: deptIcon.iconPath(''),
    hospital: null,
    editMode: false,
    formData: {
      name: '',
      title: '',
      expertise: '',
      phone: '',
      email: '',
      description: ''
    },
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  },

  onLoad() {
    const role = wx.getStorageSync('userRole');
    if (role !== 'doctor') {
      wx.redirectTo({ url: '/pages/login/login' });
      return;
    }
    this.loadDoctorInfo();
  },

  loadDoctorInfo() {
    const doctorId = wx.getStorageSync('doctorId');
    
    if (!doctorId) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '加载中...' });

    wx.request({
      url: app.globalData.baseUrl + '/api/doctors/' + doctorId,
      method: 'GET',
      success: (res) => {
        if (res.data) {
          const doctor = res.data;
          const photo = String(doctor.photoUrl || '').trim();
          const photoUrlFull = photo
            ? (/^https?:\/\//i.test(photo) ? photo : app.globalData.baseUrl + photo)
            : '';
          this.setData({
            doctor: doctor,
            photoUrlFull: photoUrlFull,
            avatarChar: String(doctor.name || '医').trim().charAt(0) || '医',
            formData: {
              name: doctor.name || '',
              title: doctor.title || '',
              expertise: doctor.expertise || '',
              phone: doctor.phone || '',
              email: doctor.email || '',
              description: doctor.description || ''
            }
          });

          if (doctor.deptId) {
            this.loadDepartment(doctor.deptId);
          }
          if (doctor.hospitalId) {
            this.loadHospital(doctor.hospitalId);
          }
        }
        wx.hideLoading();
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '加载失败', icon: 'none' });
      }
    });
  },

  loadDepartment(deptId) {
    wx.request({
      url: app.globalData.baseUrl + '/api/departments/' + deptId,
      method: 'GET',
      success: (res) => {
        if (res.data) {
          this.setData({
            department: res.data,
            deptIcon: deptIcon.iconPath(res.data.deptName || res.data.name || '')
          });
        }
      }
    });
  },

  loadHospital(hospitalId) {
    wx.request({
      url: app.globalData.baseUrl + '/api/hospitals/' + hospitalId,
      method: 'GET',
      success: (res) => {
        if (res.data) {
          this.setData({ hospital: res.data });
        }
      }
    });
  },

  enterEditMode() {
    this.setData({ editMode: true });
  },

  cancelEdit() {
    this.setData({ editMode: false });
    if (this.data.doctor) {
      this.setData({
        formData: {
          name: this.data.doctor.name || '',
          title: this.data.doctor.title || '',
          expertise: this.data.doctor.expertise || '',
          phone: this.data.doctor.phone || '',
          email: this.data.doctor.email || '',
          description: this.data.doctor.description || ''
        }
      });
    }
  },

  onInputChange(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({
      [`formData.${field}`]: e.detail.value
    });
  },

  saveProfile() {
    const doctorId = wx.getStorageSync('doctorId');
    
    wx.request({
      url: app.globalData.baseUrl + '/api/doctor-self/' + doctorId + '/profile',
      method: 'PUT',
      header: { 'content-type': 'application/json' },
      data: this.data.formData,
      success: (res) => {
        if (res.data && res.data.success) {
          wx.showToast({ title: '保存成功', icon: 'success' });
          this.setData({ editMode: false });
          this.loadDoctorInfo();
          
          if (this.data.formData.name) {
            wx.setStorageSync('doctorName', this.data.formData.name);
          }
        } else {
          wx.showToast({ title: res.data?.message || '保存失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.showToast({ title: '保存失败', icon: 'none' });
      }
    });
  },

  showChangePasswordModal() {
    this.setData({
      currentPassword: '',
      newPassword: '',
      confirmPassword: '',
      showPasswordModal: true
    });
  },

  closePasswordModal() {
    this.setData({ showPasswordModal: false });
  },

  onPasswordInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({
      [field]: e.detail.value
    });
  },

  changePassword() {
    if (!this.data.currentPassword) {
      wx.showToast({ title: '请输入当前密码', icon: 'none' });
      return;
    }
    if (!this.data.newPassword) {
      wx.showToast({ title: '请输入新密码', icon: 'none' });
      return;
    }
    if (this.data.newPassword !== this.data.confirmPassword) {
      wx.showToast({ title: '两次密码不一致', icon: 'none' });
      return;
    }
    if (this.data.newPassword.length < 6) {
      wx.showToast({ title: '密码长度至少6位', icon: 'none' });
      return;
    }

    const doctorId = wx.getStorageSync('doctorId');

    wx.request({
      url: app.globalData.baseUrl + '/api/doctor-self/' + doctorId + '/password',
      method: 'PUT',
      header: { 'content-type': 'application/json' },
      data: {
        oldPassword: this.data.currentPassword,
        newPassword: this.data.newPassword
      },
      success: (res) => {
        if (res.data && res.data.success) {
          wx.showToast({ title: '密码修改成功', icon: 'success' });
          this.closePasswordModal();
        } else {
          wx.showToast({ title: res.data?.message || '修改失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.showToast({ title: '修改失败', icon: 'none' });
      }
    });
  },

  handleLogout() {
    wx.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          wx.removeStorageSync('token');
          wx.removeStorageSync('authToken');
          wx.removeStorageSync('openid');
          wx.removeStorageSync('userRole');
          wx.removeStorageSync('doctorId');
          wx.removeStorageSync('doctorName');
          app.globalData.token = null;
          app.globalData.openid = null;
          app.globalData.userRole = null;
          app.globalData.doctorId = null;
          app.globalData.doctorName = null;
          
          wx.redirectTo({ url: '/pages/login/login' });
        }
      }
    });
  },

  goBack() {
    wx.navigateBack();
  },

  stopPropagation() {
    // 阻止事件冒泡
  }
});