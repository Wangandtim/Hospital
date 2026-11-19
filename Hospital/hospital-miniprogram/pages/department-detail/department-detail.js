const app = getApp();
const placeholder = require('../../utils/placeholder');
const pager = require('../../utils/pager');

Page({
  data: {
    deptId: '',
    department: null,
    hospital: null,
    doctors: [],
    pager: { status: 'idle', hasMore: true, total: 0, error: '', loading: false },
    loading: true
  },

  onLoad(options) {
    if (options.id) {
      this.setData({ deptId: options.id });
      this.createPager();
      this.loadDepartmentDetail(options.id);
      this.reloadDoctors();
    }
  },

  onUnload() {
    if (this.pager) {
      this.pager.destroy();
    }
  },

  onReachBottom() {
    if (this.pager) {
      this.pager.next();
    }
  },

  createPager() {
    this.pager = pager.create({
      size: 20,
      scope: this,
      fetch: (page, size) => new Promise((resolve, reject) => {
        wx.request({
          url: app.globalData.baseUrl + '/api/doctors/dept/' + this.data.deptId +
            '/page?page=' + page + '&size=' + size,
          method: 'GET',
          success: res => {
            if (res.statusCode >= 200 && res.statusCode < 300) {
              resolve(res.data);
            } else {
              reject(new Error('HTTP ' + res.statusCode));
            }
          },
          fail: reject
        });
      }),
      onReset: () => {
        this.setData({ doctors: [] });
      },
      onData: state => {
        const doctors = (state.list || []).map(doctor => ({
          ...doctor,
          ...placeholder.doctorExtra(doctor),
          avatarText: String(doctor.name || '医').slice(-2)
        }));
        this.setData({ doctors: doctors, pager: state.pager });
      }
    });
  },

  reloadDoctors() {
    if (this.pager) {
      this.pager.reset();
    }
  },

  retryDoctors() {
    if (this.pager) {
      this.pager.retry();
    }
  },

  loadDepartmentDetail(deptId) {
    wx.request({
      url: app.globalData.baseUrl + '/api/departments/' + deptId,
      method: 'GET',
      success: (res) => {
        const department = res.data;
        if (!department) {
          this.setData({ loading: false });
          return;
        }
        const extra = placeholder.departmentExtra(department);
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
        department.diseaseTags = (department.diseases || '').split(/[，,、\s]+/).filter(Boolean).slice(0, 8);
        this.setData({ department: { ...department, ...extra }, loading: false });

        if (department.deptName) {
          wx.setNavigationBarTitle({ title: department.deptName });
        }

        if (department.hospitalId) {
          wx.request({
            url: app.globalData.baseUrl + '/api/hospitals/' + department.hospitalId,
            method: 'GET',
            success: (hospitalRes) => {
              if (hospitalRes.data) {
                this.setData({
                  hospital: {
                    ...hospitalRes.data,
                    ...placeholder.hospitalExtra(hospitalRes.data)
                  }
                });
              }
            }
          });
        }
      },
      fail: (err) => {
        console.error('科室详情加载失败', err);
        this.setData({ loading: false });
        wx.showToast({ title: '加载失败，请重试', icon: 'none' });
      }
    });
  },

  viewDoctor(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/doctor-detail/doctor-detail?id=${id}` });
  },

  makeAppointment(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/appointment/appointment?doctorId=${id}` });
  },

  goDoctorList() {
    wx.navigateTo({
      url: `/pages/doctor/doctor?deptId=${this.data.deptId}&hospitalId=${this.data.hospital && this.data.hospital.hospitalId}`
    });
  }
});
