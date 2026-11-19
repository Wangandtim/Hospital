const app = getApp();
const elderly = require('../../utils/elderly');
const placeholder = require('../../utils/placeholder');

Page({
  data: {
    doctorId: '',
    doctor: null,
    department: null,
    loading: true,
    hasSchedule: false,
    elderly: false
  },

  onLoad(options) {
    console.log('doctor-detail onLoad', options);
    this.setData({ elderly: elderly.isElderly() });
    if (options.id) {
      this.setData({ doctorId: options.id });
      this.loadData(options.id);
    } else if (options.doctorId) {
      this.setData({ doctorId: options.doctorId });
      this.loadData(options.doctorId);
    }
  },

  loadData(doctorId) {
    wx.showLoading({ title: '加载中...' });
    this.setData({ loading: true });
    
    this.loadDoctorDetail(doctorId);
    this.checkTodaySchedule(doctorId);
  },

  loadDoctorDetail(doctorId) {
    wx.request({
      url: app.globalData.baseUrl + '/api/doctors/' + doctorId,
      method: 'GET',
      success: (res) => {
        console.log('医生详情加载成功', res.data);
        const doctor = res.data;
        this.setData({
          doctor: {
            ...doctor,
            ...placeholder.doctorExtra(doctor),
            avatarText: String(doctor.name || '医').slice(-2),
            specialtyTags: (doctor.expertise || '').split(/[，,、；;\s]+/).filter(Boolean).slice(0, 10)
          }
        });
        
        if (doctor.name) {
          wx.setNavigationBarTitle({
            title: doctor.name
          });
        }
        
        if (doctor.deptId) {
          this.loadDepartment(doctor.deptId);
        }
        this.checkLoadComplete();
      },
      fail: (err) => {
        console.error('医生详情加载失败', err);
        this.checkLoadComplete();
        wx.showToast({
          title: '加载失败',
          icon: 'none'
        });
      }
    });
  },

  loadDepartment(deptId) {
    wx.request({
      url: app.globalData.baseUrl + '/api/departments/' + deptId,
      method: 'GET',
      success: (res) => {
        if (res.statusCode === 200 && res.data && res.data.deptId) {
          const department = res.data;
          const level = Number(department.flagshipLevel) || 0;
          department.flagshipText = level === 1
            ? '国家临床重点专科'
            : level === 2
              ? '省级临床重点专科'
              : level === 3
                ? '市级临床重点专科'
                : Number(department.isFlagship) === 1 ? '临床重点专科' : '';
          this.setData({ department: { ...department, ...placeholder.departmentExtra(department) } });
        } else {
          this.setData({ department: null });
        }
        this.checkLoadComplete();
      },
      fail: () => {
        this.checkLoadComplete();
      }
    });
  },

  checkTodaySchedule(doctorId) {
    const today = new Date().toISOString().split('T')[0];
    
    wx.request({
      url: app.globalData.baseUrl + '/api/schedules/doctor/' + doctorId + '/date/' + today,
      method: 'GET',
      success: (res) => {
        let hasSchedule = false;
        
        if (res.data && res.data.success && Array.isArray(res.data.data)) {
          hasSchedule = res.data.data.length > 0;
        } else if (res.data && Array.isArray(res.data)) {
          hasSchedule = res.data.length > 0;
        }
        
        console.log('医生今日排班:', hasSchedule);
        this.setData({ hasSchedule });
      },
      fail: () => {
        this.setData({ hasSchedule: false });
      }
    });
  },

  checkLoadComplete() {
    wx.hideLoading();
    if (this.data.loading) {
      this.setData({ loading: false });
    }
  },

  makeAppointment() {
    if (!this.data.doctor) return;
    
    let url = `/pages/appointment/appointment?doctorId=${this.data.doctor.doctorId}`;
    if (this.data.doctor.hospitalId) {
      url += `&hospitalId=${this.data.doctor.hospitalId}`;
    }
    wx.navigateTo({ url: url });
  },

  goDeptDetail() {
    if (!this.data.department) return;
    wx.navigateTo({ url: `/pages/department-detail/department-detail?id=${this.data.department.deptId}` });
  }
})
