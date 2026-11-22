const app = getApp();
const config = require('../../../../config/index');
const deptIcon = require('../../../../utils/dept-icon');

Page({
  data: {
    hospitalId: '',
    hospitalName: '',
    departmentList: [],
    searchKeyword: '',
    showAddModal: false,
    isEdit: false,
    currentDeptId: '',
    form: {
      deptName: '',
      deptCode: '',
      manager: '',
      phone: ''
    },
    apiBase: config.apiBase + '/api',
    remainText: '',
    expireSoon: false,
    submitting: false
  },

  onLoad() {
    const isDoctorEntry = wx.getStorageSync('emergencyEntry') === 'doctor';
    this.doctorEntry = isDoctorEntry;

    const hospitalId = wx.getStorageSync('emergencyHospitalId') || wx.getStorageSync('hospitalId');
    const hospitalName = wx.getStorageSync('emergencyHospitalName') || wx.getStorageSync('hospitalName');
    const token = wx.getStorageSync('emergencyToken');

    if (!token || !hospitalId) {
      wx.redirectTo({
        url: '/subpackages/admin/pages/emergency-login/emergency-login'
      });
      return;
    }

    this.setData({
      hospitalId: hospitalId,
      hospitalName: hospitalName || '未知医院',
      emergencyEntry: isDoctorEntry ? 'doctor' : 'admin'
    });

    this.startExpireTimer();
    this.loadDepartments();
  },

  onUnload() {
    if (this.expireTimer) {
      clearInterval(this.expireTimer);
      this.expireTimer = null;
    }
  },

  // 应急权限倒计时：剩余 5 分钟提示，到期自动退出
  startExpireTimer() {
    const expireTime = wx.getStorageSync('expireTime');
    if (!expireTime) {
      return;
    }

    const normalized = String(expireTime).replace(/(\.\d{3})\d{1,}/, '$1');
    const endTime = new Date(normalized).getTime();
    if (isNaN(endTime)) {
      return;
    }

    const tick = () => {
      const remain = endTime - Date.now();
      if (remain <= 0) {
        clearInterval(this.expireTimer);
        this.expireTimer = null;
        this.handleTokenExpire('应急操作权限已到期，请重新登录。');
        return;
      }
      const minutes = Math.floor(remain / 60000);
      const seconds = Math.floor((remain % 60000) / 1000);
      const expireSoon = remain <= 5 * 60000;
      this.setData({
        remainText: `${minutes}分${String(seconds).padStart(2, '0')}秒`,
        expireSoon: expireSoon
      });
      if (expireSoon && !this.expireWarned) {
        this.expireWarned = true;
        wx.showToast({ title: '应急权限即将到期', icon: 'none' });
      }
    };

    tick();
    this.expireTimer = setInterval(tick, 1000);
  },

  loadDepartments() {
    const token = wx.getStorageSync('emergencyToken');
    wx.showLoading({ title: '加载中...' });
    
    wx.request({
      url: this.data.apiBase + '/departments/hospital/' + this.data.hospitalId,
      method: 'GET',
      header: {
        'Authorization': 'Bearer ' + token
      },
      success: (res) => {
        wx.hideLoading();
        if (res.statusCode === 401 || (res.data && res.data.code === 4013)) {
          this.handleTokenExpire();
          return;
        }
        if (Array.isArray(res.data)) {
          const departmentList = res.data.map(d => Object.assign({}, d, {
            deptIcon: deptIcon.iconPath(d.deptName)
          }));
          this.allDepartments = departmentList;
          this.setData({
            departmentList: departmentList
          });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showModal({
          title: '网络超时',
          content: '科室列表加载失败，请检查网络后重试。',
          confirmText: '重试',
          cancelText: '返回',
          success: (modalRes) => {
            if (modalRes.confirm) {
              this.loadDepartments();
            } else {
              wx.navigateBack();
            }
          }
        });
      }
    });
  },

  onSearchInput(e) {
    const keyword = e.detail.value;
    this.setData({ searchKeyword: keyword });
    this.filterDepartments(keyword);
  },

  filterDepartments(keyword) {
    if (!keyword) {
      this.setData({ departmentList: this.allDepartments || [] });
      return;
    }

    const source = this.allDepartments || [];
    const filtered = source.filter(d => 
      d.deptName && d.deptName.includes(keyword)
    );
    this.setData({ departmentList: filtered });
  },

  goToAdd() {
    this.setData({
      showAddModal: true,
      isEdit: false,
      currentDeptId: '',
      form: {
        deptName: '',
        deptCode: '',
        manager: '',
        phone: ''
      }
    });
  },

  goToEdit(e) {
    const deptId = e.currentTarget.dataset.deptId;
    const dept = this.data.departmentList.find(d => d.deptId === deptId);
    
    if (dept) {
      this.setData({
        showAddModal: true,
        isEdit: true,
        currentDeptId: deptId,
        form: {
          deptName: dept.deptName || '',
          deptCode: dept.deptCode || '',
          manager: dept.manager || '',
          phone: dept.phone || ''
        }
      });
    }
  },

  goToDetail() {},

  onDeptNameInput(e) {
    this.setData({ 'form.deptName': e.detail.value });
  },

  onDeptCodeInput(e) {
    this.setData({ 'form.deptCode': e.detail.value });
  },

  onManagerInput(e) {
    this.setData({ 'form.manager': e.detail.value });
  },

  onPhoneInput(e) {
    this.setData({ 'form.phone': e.detail.value });
  },

  closeModal() {
    this.setData({ showAddModal: false });
  },

  stopPropagation() {},

  handleSubmit() {
    const deptName = (this.data.form.deptName || '').trim();
    if (!deptName) {
      wx.showModal({
        title: '科室名称不能为空',
        content: '请输入科室名称后再提交。',
        showCancel: false
      });
      return;
    }

    const duplicated = (this.allDepartments || []).some(d =>
      d.deptId !== this.data.currentDeptId && d.deptName === deptName
    );
    if (duplicated) {
      wx.showModal({
        title: '科室已存在',
        content: `“${deptName}”已在该医院存在，请勿重复添加。`,
        showCancel: false
      });
      return;
    }

    if (this.data.submitting) {
      return;
    }
    this.setData({ submitting: true });

    const token = wx.getStorageSync('emergencyToken');
    const url = this.data.isEdit 
      ? this.data.apiBase + '/departments/' + this.data.currentDeptId
      : this.data.apiBase + '/departments';
    const method = this.data.isEdit ? 'PUT' : 'POST';

    wx.showLoading({ title: '提交中...' });

    let data = {
      deptName: this.data.form.deptName,
      deptCode: this.data.form.deptCode,
      manager: this.data.form.manager,
      phone: this.data.form.phone,
      hospitalId: this.data.hospitalId
    };

    if (!this.data.isEdit) {
      data.description = '';
      data.diseases = '';
      data.serviceTime = '';
      data.isFlagship = 0;
    }

    wx.request({
      url: url,
      method: method,
      data: data,
      header: {
        'Authorization': 'Bearer ' + token
      },
      success: (res) => {
        wx.hideLoading();
        this.setData({ submitting: false });
        if (res.statusCode === 401 || (res.data && res.data.code === 4013)) {
          this.handleTokenExpire();
          return;
        }
        if (res.statusCode === 200 && (!res.data || res.data.success !== false)) {
          wx.showToast({
            title: this.data.isEdit ? '修改成功' : '添加成功',
            icon: 'success'
          });
          this.closeModal();
          this.loadDepartments();
        } else {
          const message = (res.data && res.data.message) || (res.data && res.data.error) || '操作失败';
          wx.showModal({
            title: '保存失败',
            content: message,
            showCancel: false
          });
        }
      },
      fail: () => {
        wx.hideLoading();
        this.setData({ submitting: false });
        wx.showModal({
          title: '网络超时',
          content: '科室未保存，请检查网络后重试。',
          confirmText: '重试',
          cancelText: '取消',
          success: (modalRes) => {
            if (modalRes.confirm) {
              this.handleSubmit();
            }
          }
        });
      }
    });
  },

  handleDelete(e) {
    const deptId = e.currentTarget.dataset.deptId;
    
    wx.showModal({
      title: '确认删除',
      content: '确定要删除该科室吗？',
      success: (res) => {
        if (res.confirm) {
          const token = wx.getStorageSync('emergencyToken');
          wx.showLoading({ title: '删除中...' });

          wx.request({
            url: this.data.apiBase + '/departments/' + deptId,
            method: 'DELETE',
            header: {
              'Authorization': 'Bearer ' + token
            },
            success: (res) => {
              wx.hideLoading();
              if (res.statusCode === 401 || (res.data && res.data.code === 4013)) {
                this.handleTokenExpire();
                return;
              }
              if (res.statusCode === 200 && (!res.data || res.data.success !== false)) {
                wx.showToast({
                  title: '删除成功',
                  icon: 'success'
                });
                this.loadDepartments();
              } else {
                wx.showModal({
                  title: '删除失败',
                  content: (res.data && res.data.message) || (res.data && res.data.error) || '该科室可能仍有关联医生或排班，无法删除。',
                  showCancel: false
                });
              }
            },
            fail: () => {
              wx.hideLoading();
              wx.showModal({
                title: '网络超时',
                content: '删除未完成，请检查网络后重试。',
                confirmText: '重试',
                cancelText: '取消',
                success: (modalRes) => {
                  if (modalRes.confirm) {
                    this.handleDelete({ currentTarget: { dataset: { deptId: deptId } } });
                  }
                }
              });
            }
          });
        }
      }
    });
  },

  exitDoctorEmergency() {
    if (this.expireTimer) {
      clearInterval(this.expireTimer);
      this.expireTimer = null;
    }
    const token = wx.getStorageSync('emergencyToken');
    if (token) {
      wx.request({
        url: this.data.apiBase + '/emergency/logout',
        method: 'POST',
        data: { token: token },
        header: { 'Authorization': 'Bearer ' + token }
      });
    }
    wx.removeStorageSync('emergencyToken');
    wx.removeStorageSync('emergencyEntry');
    wx.removeStorageSync('emergencyHospitalId');
    wx.removeStorageSync('emergencyHospitalName');
    wx.navigateBack({
      fail: () => {
        wx.switchTab({ url: '/pages/index/index' });
      }
    });
  },

  handleLogout() {
    if (this.doctorEntry) {
      wx.showModal({
        title: '退出应急权限',
        content: '退出后仅失去临时科室权限，你的医生账号仍保持登录。',
        confirmText: '退出',
        cancelText: '取消',
        success: (res) => {
          if (res.confirm) {
            this.exitDoctorEmergency();
          }
        }
      });
      return;
    }

    wx.showModal({
      title: '确认退出',
      content: '确定要退出应急登录吗？',
      success: (res) => {
        if (res.confirm) {
          const token = wx.getStorageSync('emergencyToken');
          
          wx.request({
            url: this.data.apiBase + '/emergency/logout',
            method: 'POST',
            data: { token: token },
            header: {
              'Authorization': 'Bearer ' + token
            }
          });

          if (this.expireTimer) {
            clearInterval(this.expireTimer);
            this.expireTimer = null;
          }

          wx.removeStorageSync('emergencyToken');
          wx.removeStorageSync('token');
          wx.removeStorageSync('authToken');
          wx.removeStorageSync('userRole');
          wx.removeStorageSync('hospitalId');
          wx.removeStorageSync('hospitalName');
          wx.removeStorageSync('expireTime');
          
          app.globalData.token = null;
          app.globalData.userRole = null;
          app.globalData.hospitalId = null;
          app.globalData.hospitalName = null;

          wx.redirectTo({
            url: '/subpackages/admin/pages/emergency-login/emergency-login'
          });
        }
      }
    });
  },

  handleTokenExpire(message) {
    if (this.expired) {
      return;
    }
    this.expired = true;

    if (this.expireTimer) {
      clearInterval(this.expireTimer);
      this.expireTimer = null;
    }

    if (this.doctorEntry) {
      wx.removeStorageSync('emergencyToken');
      wx.removeStorageSync('emergencyEntry');
      wx.removeStorageSync('emergencyHospitalId');
      wx.removeStorageSync('emergencyHospitalName');
      wx.showModal({
        title: '应急权限已到期',
        content: '临时科室权限已失效，你的医生账号仍保持登录，可重新申请。',
        showCancel: false,
        confirmText: '返回工作台',
        success: () => {
          wx.navigateBack({
            fail: () => wx.switchTab({ url: '/pages/index/index' })
          });
        }
      });
      return;
    }

    wx.showModal({
      title: '应急权限已失效',
      content: message || '应急登录已过期，请重新使用当日密钥登录。',
      showCancel: false,
      confirmText: '重新登录',
      success: () => {
        wx.removeStorageSync('emergencyToken');
        wx.removeStorageSync('token');
        wx.removeStorageSync('authToken');
        wx.removeStorageSync('userRole');
        wx.removeStorageSync('hospitalId');
        wx.removeStorageSync('hospitalName');
        wx.removeStorageSync('expireTime');

        app.globalData.token = null;
        app.globalData.userRole = null;
        app.globalData.hospitalId = null;
        app.globalData.hospitalName = null;

        wx.redirectTo({
          url: '/subpackages/admin/pages/emergency-login/emergency-login'
        });
      }
    });
  }
})