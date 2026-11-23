const app = getApp();
const elderly = require('../../utils/elderly');
const placeholder = require('../../utils/placeholder');
const deptIcon = require('../../utils/dept-icon');

function resolveMedia(url) {
  const text = String(url || '');
  if (!text) return '';
  if (text.indexOf('http://') === 0 || text.indexOf('https://') === 0 || text.indexOf('wxfile://') === 0) {
    return text;
  }
  return app.globalData.baseUrl + (text.charAt(0) === '/' ? text : '/' + text);
}

function parseList(data) {
  if (Array.isArray(data)) return data;
  if (data && Array.isArray(data.data)) return data.data;
  if (data && Array.isArray(data.list)) return data.list;
  return [];
}

Page({
  data: {
    doctorId: '',
    doctor: null,
    department: null,
    loading: true,
    hasSchedule: false,
    elderly: false,
    isAdmin: false,
    certificates: [],
    certPreviewUrls: []
  },

  onLoad(options) {
    this.setData({
      elderly: elderly.isElderly(),
      isAdmin: wx.getStorageSync('userRole') === 'admin'
    });
    if (options.id) {
      this.setData({ doctorId: options.id });
      this.loadData(options.id);
    } else if (options.doctorId) {
      this.setData({ doctorId: options.doctorId });
      this.loadData(options.doctorId);
    }
  },

  onShow() {
    // 管理端保存介绍后返回本页，重新拉取；首次进入由 onLoad 负责
    if (this._loaded && this.data.doctorId) {
      this.loadData(this.data.doctorId);
    }
    this._loaded = true;
  },

  loadData(doctorId) {
    wx.showLoading({ title: '加载中...' });
    this.setData({ loading: true });
    
    this.loadDoctorDetail(doctorId);
    this.loadCertificates(doctorId);
    this.checkTodaySchedule(doctorId);
  },

  loadCertificates(doctorId) {
    wx.request({
      url: app.globalData.baseUrl + '/api/doctors/' + doctorId + '/certificates',
      method: 'GET',
      success: (res) => {
        const list = parseList(res.data).filter(item => item && item.title);
        this.setData({
          certificates: list.map(item => ({ ...item, imageUrlFull: resolveMedia(item.imageUrl) })),
          certPreviewUrls: list.map(item => resolveMedia(item.imageUrl)).filter(Boolean)
        });
      },
      fail: () => {
        this.setData({ certificates: [], certPreviewUrls: [] });
      }
    });
  },

  previewCertificate(e) {
    const index = Number(e.currentTarget.dataset.index);
    const item = this.data.certificates[index];
    if (!item || !item.imageUrlFull) return;
    wx.previewImage({
      current: item.imageUrlFull,
      urls: this.data.certPreviewUrls
    });
  },

  loadDoctorDetail(doctorId) {
    wx.request({
      url: app.globalData.baseUrl + '/api/doctors/' + doctorId,
      method: 'GET',
      success: (res) => {
        const doctor = res.data;
        // 本周出诊只取后端真实排班；全部无号时不展示该模块
        const scheduleRows = (doctor.scheduleDays || [])
          .filter(day => Number(day.slots) > 0)
          .map(day => ({
            label: day.week + ' · ' + String(day.date || '').slice(5),
            detail: '可约 ' + Number(day.slots) + ' 个时段',
            pill: Number(day.remaining) > 0 ? '余 ' + day.remaining : '已约满',
            tone: Number(day.remaining) > 0 ? 'wait' : 'cancel'
          }));
        this.setData({
          doctor: {
            ...doctor,
            ...placeholder.doctorExtra(doctor),
            avatarText: String(doctor.name || '医').slice(0, 1),
            photoUrlFull: resolveMedia(doctor.photoUrl),
            specialtyTags: (doctor.expertise || '').split(/[，,、；;\s]+/).filter(Boolean).slice(0, 10),
            scheduleRows
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
          this.setData({
            department: {
              ...department,
              ...placeholder.departmentExtra(department),
              deptIcon: deptIcon.iconPath(department.deptName),
              floorZone: department.location || department.floorZone || ''
            }
          });
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

  goEditIntro() {
    wx.navigateTo({
      url: '/subpackages/admin/pages/doctor-intro-edit/doctor-intro-edit?doctorId=' + this.data.doctorId
    });
  },

  goDeptDetail() {    if (!this.data.department) return;
    wx.navigateTo({ url: `/pages/department-detail/department-detail?id=${this.data.department.deptId}` });
  }
})
