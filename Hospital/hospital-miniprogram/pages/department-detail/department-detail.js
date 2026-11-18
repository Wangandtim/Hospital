const app = getApp();

Page({
  data: {
    deptId: '',
    department: null,
    hospital: null,
    doctors: [],
    loading: true
  },

  onLoad(options) {
    console.log('department-detail onLoad', options);
    if (options.id) {
      this.setData({ deptId: options.id });
      this.loadDepartmentDetail(options.id);
    }
  },

  loadDepartmentDetail(deptId) {
    wx.showLoading({ title: '加载中...' });

    wx.request({
      url: app.globalData.baseUrl + '/api/departments/' + deptId,
      method: 'GET',
      success: (res) => {
        console.log('科室详情加载成功', res.data);
        const department = res.data;
        const level = Number(department.flagshipLevel) || 0;
        department.flagshipText = level === 1
          ? '国家临床重点专科'
          : level === 2
            ? '省级临床重点专科'
            : level === 3
              ? '市级临床重点专科'
              : Number(department.isFlagship) === 1 ? '临床重点专科' : '';
        department.awardList = (department.awards || '').split('\n').map(t => t.trim()).filter(Boolean);
        department.caseList = (department.cases || '').split('\n').map(t => t.trim()).filter(Boolean);
        this.setData({ department: department });

        if (department.deptName) {
          wx.setNavigationBarTitle({
            title: department.deptName
          });
        }

        // 获取医院信息
        if (department.hospitalId) {
          wx.request({
            url: app.globalData.baseUrl + '/api/hospitals/' + department.hospitalId,
            method: 'GET',
            success: (hospitalRes) => {
              if (hospitalRes.data) {
                this.setData({ hospital: hospitalRes.data });
              }
            }
          });
        }

        this.loadDoctorsByDeptId(deptId);
      },
      fail: (err) => {
        console.error('科室详情加载失败', err);
        wx.hideLoading();
        this.setData({ loading: false });
        wx.showToast({
          title: '加载失败',
          icon: 'none'
        });
      }
    });
  },

  loadDoctorsByDeptId(deptId) {
    wx.request({
      url: app.globalData.baseUrl + '/api/doctors/dept/' + deptId,
      method: 'GET',
      success: (res) => {
        console.log('科室医生加载成功', res.data);
        wx.hideLoading();
        this.setData({
          doctors: res.data || [],
          loading: false
        });
      },
      fail: (err) => {
        console.error('科室医生加载失败', err);
        wx.hideLoading();
        this.setData({ loading: false });
      }
    });
  },

  viewDoctor(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/doctor-detail/doctor-detail?id=${id}`
    });
  },

  makeAppointment(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/appointment/appointment?doctorId=${id}`
    });
  }
})