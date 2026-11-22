const app = getApp();
const deptIcon = require('../../utils/dept-icon');

Page({
  data: {
    departments: [],
    hospitalList: [],
    hospitalId: '',
    selectedHospital: '',
    selectedHospitalName: '全部医院',
    selectedHospitalNameForModal: '',
    formDeptIcon: deptIcon.iconPath(''),
    searchKeyword: '',
    showAddModal: false,
    editingDept: null,
    formData: {
      deptName: '',
      description: '',
      diseases: '',
      serviceTime: ''
    }
  },

  onLoad(options) {
    const role = wx.getStorageSync('adminRole');
    const hospitalId = options?.hospitalId || wx.getStorageSync('currentHospitalId') || wx.getStorageSync('hospitalId');
    
    this.setData({ 
      hospitalId: hospitalId,
      isSuperAdmin: role === 'SUPER_ADMIN',
      selectedHospital: hospitalId || ''
    });
    
    this.loadHospitals();
    this.loadDepartments();
  },

  onShow() {
    this.loadDepartments();
  },

  loadHospitals() {
    wx.request({
      url: app.globalData.baseUrl + '/api/hospitals',
      method: 'GET',
      success: (res) => {
        if (res.data && Array.isArray(res.data)) {
          this.setData({ hospitalList: res.data });
          if (this.data.isSuperAdmin && !this.data.selectedHospital) {
            this.setData({ selectedHospitalName: '全部医院' });
          } else if (this.data.selectedHospital) {
            const hospital = res.data.find(h => h.hospitalId === this.data.selectedHospital);
            if (hospital) {
              this.setData({ selectedHospitalName: hospital.hospitalName });
            }
          }
        }
      }
    });
  },

  loadDepartments() {
    wx.showLoading({ title: '加载中...' });

    let url = app.globalData.baseUrl + '/api/departments';
    
    if (this.data.selectedHospital && this.data.selectedHospital !== 'all') {
      url = app.globalData.baseUrl + '/api/departments/hospital/' + this.data.selectedHospital;
    }

    wx.request({
      url: url,
      method: 'GET',
      success: (res) => {
        wx.hideLoading();
        if (res.data && Array.isArray(res.data)) {
          const deptsWithHospital = res.data.map(dept => {
            const hospital = this.data.hospitalList.find(h => h.hospitalId === dept.hospitalId);
            return {
              ...dept,
              hospitalName: hospital ? hospital.hospitalName : (dept.hospitalName || '未知医院'),
              deptIcon: deptIcon.iconPath(dept.deptName || dept.name || '')
            };
          }).filter(dept => !this.data.searchKeyword || 
            (dept.deptName && dept.deptName.includes(this.data.searchKeyword)) ||
            (dept.deptCode && dept.deptCode.includes(this.data.searchKeyword))
          );
          this.setData({ departments: deptsWithHospital });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({
          title: '加载失败',
          icon: 'none'
        });
      }
    });
  },

  onHospitalChange(e) {
    const index = e.detail.value;
    const hospitalList = this.data.hospitalList;
    
    if (index >= 0 && hospitalList[index]) {
      const hospital = hospitalList[index];
      this.setData({ 
        selectedHospital: hospital.hospitalId,
        selectedHospitalName: hospital.hospitalName
      });
    } else {
      this.setData({ 
        selectedHospital: '',
        selectedHospitalName: '全部医院'
      });
    }
    this.loadDepartments();
  },

  onSearchInput(e) {
    const keyword = e.detail.value;
    this.setData({ searchKeyword: keyword });
    this.loadDepartments();
  },

  openAddModal() {
    this.setData({
      showAddModal: true,
      editingDept: null,
      selectedHospitalNameForModal: '',
      formDeptIcon: deptIcon.iconPath(''),
      formData: {
        deptName: '',
        description: '',
        diseases: '',
        serviceTime: ''
      }
    });
  },

  openEditModal(e) {
    const dept = e.currentTarget.dataset.dept;
    const hospital = this.data.hospitalList.find(h => h.hospitalId === dept.hospitalId);
    this.setData({
      showAddModal: true,
      editingDept: dept,
      selectedHospitalNameForModal: hospital ? hospital.hospitalName : dept.hospitalName,
      formDeptIcon: deptIcon.iconPath(dept.deptName || dept.name || ''),
      formData: {
        deptName: dept.deptName || dept.name || '',
        description: dept.description || '',
        diseases: dept.diseases || '',
        serviceTime: dept.serviceTime || ''
      }
    });
  },

  closeModal() {
    this.setData({ showAddModal: false, editingDept: null });
  },

  onHospitalSelectChange(e) {
    const index = e.detail.value;
    const hospitalList = this.data.hospitalList;
    
    if (index >= 0 && hospitalList[index]) {
      const hospital = hospitalList[index];
      this.setData({ 
        selectedHospitalNameForModal: hospital.hospitalName,
        'formData.hospitalId': hospital.hospitalId
      });
    }
  },

  onFormInput(e) {
    const field = e.currentTarget.dataset.field;
    const value = e.detail.value;
    const patch = {
      [`formData.${field}`]: value
    };
    if (field === 'deptName') {
      patch.formDeptIcon = deptIcon.iconPath(value);
    }
    this.setData(patch);
  },

  submitDepartment() {
    const { formData, editingDept } = this.data;

    if (!formData.deptName) {
      wx.showToast({
        title: '请输入科室名称',
        icon: 'none'
      });
      return;
    }

    if (!editingDept && !formData.hospitalId) {
      wx.showToast({
        title: '请选择所属医院',
        icon: 'none'
      });
      return;
    }

    wx.showLoading({ title: '保存中...' });

    const deptData = {
      name: formData.deptName,
      description: formData.description,
      diseases: formData.diseases,
      serviceTime: formData.serviceTime,
      hospitalId: editingDept?.hospitalId || formData.hospitalId
    };
    const token = wx.getStorageSync('token') || wx.getStorageSync('authToken');
    const authHeader = Object.assign({ 'Content-Type': 'application/json' }, token ? { 'X-Auth-Token': token } : {});

    if (editingDept) {
      wx.request({
        url: app.globalData.baseUrl + '/api/departments/' + editingDept.deptId,
        method: 'PUT',
        header: authHeader,
        data: deptData,
        success: (res) => {
          wx.hideLoading();
          if (res.data.success || res.statusCode === 200) {
            wx.showToast({
              title: '更新成功',
              icon: 'success'
            });
            this.closeModal();
            this.loadDepartments();
          } else {
            wx.showToast({
              title: res.data.message || '更新失败',
              icon: 'none'
            });
          }
        },
        fail: () => {
          wx.hideLoading();
          wx.showToast({
            title: '网络请求失败',
            icon: 'none'
          });
        }
      });
    } else {
      wx.request({
        url: app.globalData.baseUrl + '/api/departments',
        method: 'POST',
        header: authHeader,
        data: deptData,
        success: (res) => {
          wx.hideLoading();
          if (res.data.success || res.statusCode === 200) {
            wx.showToast({
              title: '添加成功',
              icon: 'success'
            });
            this.closeModal();
            this.loadDepartments();
          } else {
            wx.showToast({
              title: res.data.message || '添加失败',
              icon: 'none'
            });
          }
        },
        fail: () => {
          wx.hideLoading();
          wx.showToast({
            title: '网络请求失败',
            icon: 'none'
          });
        }
      });
    }
  },

  deleteDepartment(e) {
    const dept = e.currentTarget.dataset.dept;

    wx.showModal({
      title: '确认删除',
      content: '确定要删除科室「' + dept.deptName + '」吗？',
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '删除中...' });

          const token = wx.getStorageSync('token') || wx.getStorageSync('authToken');
          wx.request({
            url: app.globalData.baseUrl + '/api/departments/' + dept.deptId,
            method: 'DELETE',
            header: token ? { 'X-Auth-Token': token } : {},
            success: (res) => {
              wx.hideLoading();
              if (res.data.success || res.statusCode === 200) {
                wx.showToast({
                  title: '删除成功',
                  icon: 'success'
                });
                this.loadDepartments();
              } else {
                wx.showToast({
                  title: res.data.message || '删除失败',
                  icon: 'none'
                });
              }
            },
            fail: () => {
              wx.hideLoading();
              wx.showToast({
                title: '网络请求失败',
                icon: 'none'
              });
            }
          });
        }
      }
    });
  },

  goBack() {
    wx.navigateBack();
  }
})