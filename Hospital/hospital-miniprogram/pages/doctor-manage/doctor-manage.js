const app = getApp();

Page({
  data: {
    doctors: [],
    hospitalGroups: [],
    hospitalList: [],
    departments: [],
    hospitalId: '',
    searchKeyword: '',
    expandedHospital: '',
    showAddModal: false,
    editingDoctor: null,
    selectedHospitalName: '请选择医院',
    selectedDeptName: '请选择科室',
    formData: {
      name: '',
      title: '',
      deptId: '',
      hospitalId: '',
      expertise: '',
      gender: 'M',
      phone: '',
      consultationFee: 30
    }
  },

  onLoad(options) {
    const role = wx.getStorageSync('adminRole');
    const hospitalId = options?.hospitalId || wx.getStorageSync('currentHospitalId') || wx.getStorageSync('hospitalId');
    
    this.setData({ 
      hospitalId: hospitalId,
      isSuperAdmin: role === 'SUPER_ADMIN'
    });
    
    this.loadHospitals();
  },

  onShow() {
    this.loadDoctors();
  },

  loadHospitals() {
    wx.request({
      url: app.globalData.baseUrl + '/api/hospitals',
      method: 'GET',
      success: (res) => {
        if (res.data && Array.isArray(res.data)) {
          this.setData({ hospitalList: res.data });
          if (this.data.hospitalId) {
            const hospital = res.data.find(h => h.hospitalId === this.data.hospitalId);
            if (hospital) {
              this.setData({ selectedHospitalName: hospital.hospitalName });
              this.loadDepartments(this.data.hospitalId);
            }
          }
          this.loadDoctors();
        }
      }
    });
  },

  loadDepartments(hospitalId) {
    wx.request({
      url: app.globalData.baseUrl + '/api/departments/hospital/' + hospitalId,
      method: 'GET',
      success: (res) => {
        if (res.data && Array.isArray(res.data)) {
          this.setData({ departments: res.data });
        }
      }
    });
  },

  loadDoctors() {
    wx.showLoading({ title: '加载中...' });

    let url = app.globalData.baseUrl + '/api/doctors';
    
    if (this.data.hospitalId && !this.data.isSuperAdmin) {
      url = app.globalData.baseUrl + '/api/doctors/hospital/' + this.data.hospitalId;
    }

    wx.request({
      url: url,
      method: 'GET',
      success: (res) => {
        wx.hideLoading();
        if (res.data && Array.isArray(res.data)) {
          let filteredDoctors = res.data;
          
          if (this.data.searchKeyword) {
            const keyword = this.data.searchKeyword.toLowerCase();
            filteredDoctors = filteredDoctors.filter(doc => 
              (doc.name && doc.name.toLowerCase().includes(keyword)) ||
              (doc.doctorId && doc.doctorId.includes(keyword))
            );
          }
          
          this.setData({ doctors: filteredDoctors });
          this.groupByHospitalAndDept(filteredDoctors);
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

  groupByHospitalAndDept(doctors) {
    const hospitalGroups = {};
    
    doctors.forEach(doc => {
      const hospitalId = doc.hospitalId;
      const deptId = doc.deptId;
      
      if (!hospitalGroups[hospitalId]) {
        const hospital = this.data.hospitalList.find(h => h.hospitalId === hospitalId);
        hospitalGroups[hospitalId] = {
          hospitalId: hospitalId,
          hospitalName: hospital ? hospital.hospitalName : (doc.hospitalName || '未知医院'),
          deptGroups: {}
        };
      }
      
      if (!hospitalGroups[hospitalId].deptGroups[deptId]) {
        const dept = this.data.departments.find(d => d.deptId === deptId);
        hospitalGroups[hospitalId].deptGroups[deptId] = {
          deptId: deptId,
          deptName: dept ? dept.deptName : (doc.deptName || '未知科室'),
          doctors: []
        };
      }
      
      hospitalGroups[hospitalId].deptGroups[deptId].doctors.push(doc);
    });

    const result = Object.values(hospitalGroups).map(hospitalGroup => ({
      ...hospitalGroup,
      deptGroups: Object.values(hospitalGroup.deptGroups).sort((a, b) => a.deptName.localeCompare(b.deptName))
    })).sort((a, b) => a.hospitalName.localeCompare(b.hospitalName));
    
    this.setData({ hospitalGroups: result });
  },

  toggleHospital(e) {
    const hospitalId = e.currentTarget.dataset.hospitalId;
    this.setData({
      expandedHospital: this.data.expandedHospital === hospitalId ? '' : hospitalId
    });
  },

  onSearchInput(e) {
    const keyword = e.detail.value;
    this.setData({ searchKeyword: keyword });
    this.loadDoctors();
  },

  openAddModal() {
    this.setData({
      showAddModal: true,
      editingDoctor: null,
      selectedHospitalName: '请选择医院',
      selectedDeptName: '请选择科室',
      formData: {
        name: '',
        title: '',
        deptId: '',
        hospitalId: '',
        expertise: '',
        gender: 'M',
        phone: '',
        consultationFee: 30
      },
      departments: []
    });
  },

  openEditModal(e) {
    const doctor = e.currentTarget.dataset.doctor;
    const hospital = this.data.hospitalList.find(h => h.hospitalId === doctor.hospitalId);
    const dept = this.data.departments.find(d => d.deptId === doctor.deptId);
    
    this.setData({
      showAddModal: true,
      editingDoctor: doctor,
      selectedHospitalName: hospital ? hospital.hospitalName : doctor.hospitalName,
      selectedDeptName: dept ? dept.deptName : (doctor.deptName || '未知科室'),
      formData: {
        name: doctor.name,
        title: doctor.title,
        deptId: doctor.deptId,
        hospitalId: doctor.hospitalId,
        expertise: doctor.expertise,
        gender: doctor.gender,
        phone: doctor.phone || '',
        consultationFee: doctor.consultationFee || 30
      }
    });
  },

  closeModal() {
    this.setData({ showAddModal: false, editingDoctor: null });
  },

  onHospitalSelectChange(e) {
    const index = e.detail.value;
    const hospitalList = this.data.hospitalList;
    
    if (index >= 0 && hospitalList[index]) {
      const hospital = hospitalList[index];
      this.setData({ 
        selectedHospitalName: hospital.hospitalName,
        'formData.hospitalId': hospital.hospitalId,
        'formData.deptId': '',
        selectedDeptName: '请选择科室'
      });
      this.loadDepartments(hospital.hospitalId);
    }
  },

  onDeptChange(e) {
    const index = e.detail.value;
    const departments = this.data.departments;
    
    if (index >= 0 && departments[index]) {
      this.setData({
        'formData.deptId': departments[index].deptId,
        selectedDeptName: departments[index].deptName
      });
    }
  },

  onFormInput(e) {
    const field = e.currentTarget.dataset.field;
    let value = e.detail.value;
    if (field === 'consultationFee') {
      value = parseInt(value) || 0;
    }
    this.setData({
      [`formData.${field}`]: value
    });
  },

  onGenderChange(e) {
    this.setData({
      'formData.gender': e.detail.value
    });
  },

  submitDoctor() {
    const { formData, editingDoctor } = this.data;

    if (!formData.name || !formData.deptId || !formData.title) {
      wx.showToast({
        title: '请填写必填项',
        icon: 'none'
      });
      return;
    }

    if (!editingDoctor && !formData.hospitalId) {
      wx.showToast({
        title: '请选择所属医院',
        icon: 'none'
      });
      return;
    }

    wx.showLoading({ title: '保存中...' });

    const doctorData = {
      ...formData,
      hospitalId: editingDoctor?.hospitalId || formData.hospitalId
    };

    if (editingDoctor) {
      wx.request({
        url: app.globalData.baseUrl + '/api/doctors/' + editingDoctor.doctorId,
        method: 'PUT',
        data: doctorData,
        success: (res) => {
          wx.hideLoading();
          if (res.data.success || res.statusCode === 200) {
            wx.showToast({
              title: '更新成功',
              icon: 'success'
            });
            this.closeModal();
            this.loadDoctors();
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
        url: app.globalData.baseUrl + '/api/doctors',
        method: 'POST',
        data: doctorData,
        success: (res) => {
          wx.hideLoading();
          if (res.data.success || res.statusCode === 200) {
            wx.showToast({
              title: '添加成功',
              icon: 'success'
            });
            this.closeModal();
            this.loadDoctors();
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

  deleteDoctor(e) {
    const doctor = e.currentTarget.dataset.doctor;

    wx.showModal({
      title: '确认删除',
      content: '确定要删除医生「' + doctor.name + '」吗？此操作不可恢复。',
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '删除中...' });

          wx.request({
            url: app.globalData.baseUrl + '/api/doctors/' + doctor.doctorId,
            method: 'DELETE',
            success: (res) => {
              wx.hideLoading();
              if (res.data.success || res.statusCode === 200) {
                wx.showToast({
                  title: '删除成功',
                  icon: 'success'
                });
                this.loadDoctors();
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