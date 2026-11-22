const app = getApp();
const deptIcon = require('../../utils/dept-icon');

const WEEK_TEXT = ['日', '一', '二', '三', '四', '五', '六'];

Page({
  data: {
    hospitalId: '',
    hospitalName: '',
    depts: [],
    doctors: [],
    dates: [],
    slots: [],
    selectedDept: null,
    selectedDoctor: null,
    selectedDate: '',
    selectedDateText: '',
    selectedTime: '',
    step: 1,
    canBook: false,
    loadingDoctors: false,
    loadingDates: false,
    loadingSlots: false,
    showPay: false,
    paying: false,
    fee: 0,
    currentAppointment: null,
    success: false,
    baseUrl: app.globalData.baseUrl
  },

  onLoad(options) {
    const selected = wx.getStorageSync('selectedHospital') || {};
    const hospitalId = options.hospitalId || selected.hospitalId || app.globalData.selectedHospitalId || '';
    if (!hospitalId) {
      wx.showToast({ title: '请先选择医院', icon: 'none' });
      setTimeout(() => wx.navigateBack(), 1200);
      return;
    }
    this.setData({ hospitalId, hospitalName: selected.hospitalName || '' });
    this.loadHospital();
    this.loadDepts();
  },

  loadHospital() {
    wx.request({
      url: app.globalData.baseUrl + '/api/hospitals/' + this.data.hospitalId,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.hospitalName) {
          this.setData({ hospitalName: res.data.hospitalName });
        }
      }
    });
  },

  loadDepts() {
    wx.showLoading({ title: '加载科室...' });
    wx.request({
      url: app.globalData.baseUrl + '/api/departments/hospital/' + this.data.hospitalId + '?bookable=true',
      method: 'GET',
      success: (res) => {
        wx.hideLoading();
        const list = Array.isArray(res.data) ? res.data : (res.data && res.data.data) || [];
        this.setData({
          depts: list.filter(d => d.deptName).map(d => ({ ...d, deptIcon: deptIcon.iconPath(d.deptName) }))
        });
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '网络异常，请重试', icon: 'none' });
      }
    });
  },

  chooseDept(e) {
    const dept = e.currentTarget.dataset.dept;
    this.setData({
      selectedDept: dept,
      selectedDoctor: null,
      selectedDate: '',
      selectedDateText: '',
      selectedTime: '',
      dates: [],
      slots: [],
      doctors: [],
      step: 2,
      loadingDoctors: true
    });
    wx.pageScrollTo({ scrollTop: 0, duration: 200 });
    this.refreshCanBook();
    wx.request({
      url: app.globalData.baseUrl + '/api/doctors/dept/' + dept.deptId + '/hospital/' + this.data.hospitalId,
      method: 'GET',
      success: (res) => {
        const list = Array.isArray(res.data) ? res.data : (res.data && res.data.data) || [];
        this.setData({
          doctors: list.map(d => ({
            ...d,
            avatarText: String(d.name || '医').slice(0, 1),
            photoUrlFull: d.photoUrl
              ? (String(d.photoUrl).indexOf('http') === 0 ? d.photoUrl : app.globalData.baseUrl + d.photoUrl)
              : ''
          })),
          loadingDoctors: false
        });
      },
      fail: () => {
        this.setData({ loadingDoctors: false });
        wx.showToast({ title: '医生加载失败', icon: 'none' });
      }
    });
  },

  chooseDoctor(e) {
    const doctor = e.currentTarget.dataset.doctor;
    this.setData({
      selectedDoctor: doctor,
      selectedDate: '',
      selectedDateText: '',
      selectedTime: '',
      dates: [],
      slots: [],
      step: 3
    });
    this.refreshCanBook();
    wx.pageScrollTo({ scrollTop: 0, duration: 200 });
    this.loadDates(doctor.doctorId);
  },

  // 回到上一步重新选择
  editStep(e) {
    const target = Number(e.currentTarget.dataset.step);
    if (target === 1) {
      this.setData({
        step: 1,
        selectedDoctor: null,
        selectedDate: '',
        selectedDateText: '',
        selectedTime: '',
        dates: [],
        slots: [],
        doctors: [],
        loadingDoctors: false,
        loadingSlots: false
      });
    } else if (target === 2) {
      this.setData({
        step: 2,
        selectedDate: '',
        selectedDateText: '',
        selectedTime: '',
        dates: [],
        slots: [],
        loadingSlots: false
      });
    }
    this.refreshCanBook();
    wx.pageScrollTo({ scrollTop: 0, duration: 200 });
  },

  refreshCanBook() {
    const canBook = !!(this.data.selectedDept && this.data.selectedDoctor && this.data.selectedDate && this.data.selectedTime);
    if (canBook !== this.data.canBook) {
      this.setData({ canBook });
    }
  },

  loadDates(doctorId) {
    wx.showLoading({ title: '查询号源...' });
    this.setData({ loadingDates: true });
    wx.request({
      url: app.globalData.baseUrl + '/api/schedules/doctor/' + doctorId + '/dates',
      method: 'GET',
      success: (res) => {
        wx.hideLoading();
        let raw = [];
        if (res.data && Array.isArray(res.data.data)) raw = res.data.data;
        else if (Array.isArray(res.data)) raw = res.data;

        const dates = raw
          .map(d => {
            const value = typeof d === 'string' ? d : (d.date || d.scheduleDate || '');
            return value ? value.slice(0, 10) : '';
          })
          .filter(Boolean)
          .slice(0, 5)
          .map(value => ({
            value,
            label: this.dateLabel(value),
            weekday: '周' + WEEK_TEXT[new Date(value.replace(/-/g, '/')).getDay()]
          }));

        this.setData({ dates, loadingDates: false });
        if (dates.length > 0) {
          this.chooseDateByValue(dates[0].value, dates[0].label);
        }
      },
      fail: () => {
        wx.hideLoading();
        this.setData({ loadingDates: false });
        wx.showToast({ title: '号源查询失败', icon: 'none' });
      }
    });
  },

  dateLabel(value) {
    const today = this.formatDate(new Date());
    if (value === today) return '今天';
    const t = new Date(today.replace(/-/g, '/'));
    t.setDate(t.getDate() + 1);
    if (value === this.formatDate(t)) return '明天';
    t.setDate(t.getDate() + 1);
    if (value === this.formatDate(t)) return '后天';
    const d = new Date(value.replace(/-/g, '/'));
    return (d.getMonth() + 1) + '月' + d.getDate() + '日';
  },

  formatDate(date) {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  },

  chooseDate(e) {
    this.chooseDateByValue(e.currentTarget.dataset.value, e.currentTarget.dataset.label);
  },

  chooseDateByValue(value, label) {
    this.setData({ selectedDate: value, selectedDateText: label, selectedTime: '', slots: [], loadingSlots: true });
    this.refreshCanBook();
    this.loadSlots(value);
  },

  loadSlots(date) {
    wx.request({
      url: app.globalData.baseUrl + '/api/schedules/doctor/' + this.data.selectedDoctor.doctorId + '/date/' + date,
      method: 'GET',
      success: (res) => {
        let raw = [];
        if (res.data && Array.isArray(res.data.data)) raw = res.data.data;
        else if (Array.isArray(res.data)) raw = res.data;

        const slots = raw.map(item => {
          const time = item.timeSlot || item.slot || (
            item.startTime && item.endTime ? `${item.startTime}-${item.endTime}` : ''
          );
          return {
            time,
            remaining: item.remaining !== undefined ? item.remaining : item.remain,
            disabled: Number(item.remaining !== undefined ? item.remaining : 99) <= 0
          };
        }).filter(s => s.time);

        this.setData({ slots, loadingSlots: false });
        if (slots.length === 1 && !slots[0].disabled) {
          this.setData({ selectedTime: slots[0].time });
        }
        this.refreshCanBook();
      },
      fail: () => {
        this.setData({ loadingSlots: false });
        wx.showToast({ title: '号源加载失败', icon: 'none' });
      }
    });
  },

  chooseTime(e) {
    const slot = e.currentTarget.dataset.slot;
    if (slot.disabled) {
      wx.showToast({ title: '该时段已约满', icon: 'none' });
      return;
    }
    this.setData({ selectedTime: slot.time });
    this.refreshCanBook();
  },

  confirmBooking() {
    const { selectedDoctor, selectedDept, selectedDate, selectedTime, fee } = this.data;
    if (!selectedDoctor || !selectedDate || !selectedTime) {
      wx.showToast({ title: '请先选好医生和时间', icon: 'none' });
      return;
    }
    const token = wx.getStorageSync('token') || wx.getStorageSync('authToken');
    if (!token) {
      wx.showModal({
        title: '需要登录',
        content: '登录后才能预约挂号',
        confirmText: '去登录',
        success: (res) => {
          if (res.confirm) wx.navigateTo({ url: '/pages/login/login' });
        }
      });
      return;
    }
    if (!wx.getStorageSync('hasCompleteInfo')) {
      wx.showModal({
        title: '完善信息',
        content: '首次预约需要填写就诊人姓名和手机号',
        confirmText: '去填写',
        success: (res) => {
          if (res.confirm) {
            wx.navigateTo({ url: '/pages/patient-info/patient-info' });
          }
        }
      });
      return;
    }

    wx.showLoading({ title: '提交预约...' });
    wx.request({
      url: app.globalData.baseUrl + '/api/appointments',
      method: 'POST',
      header: { 'content-type': 'application/json' },
      data: {
        patientId: wx.getStorageSync('patientId'),
        doctorId: selectedDoctor.doctorId,
        appointmentDate: selectedDate,
        timeSlot: selectedTime,
        patientName: wx.getStorageSync('patientName'),
        patientPhone: wx.getStorageSync('patientPhone') || '',
        questionnaireData: null
      },
      success: (res) => {
        wx.hideLoading();
        if (res.data && res.data.appointmentId) {
          this.setData({ currentAppointment: res.data, fee: selectedDoctor.consultationFee || fee || 30 });
          this.preparePayment(res.data.appointmentId);
        } else if (res.statusCode === 401) {
          wx.showToast({ title: '登录已过期，请重新登录', icon: 'none' });
        } else {
          wx.showToast({ title: (res.data && res.data.message) || '预约失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '网络异常，预约未提交', icon: 'none' });
      }
    });
  },

  preparePayment(appointmentId) {
    wx.request({
      url: app.globalData.baseUrl + '/api/payment/prepare/' + appointmentId,
      method: 'GET',
      success: (res) => {
        const fee = res.data && !res.data.error && res.data.fee;
        this.setData({ showPay: true, fee: fee || this.data.fee });
      },
      fail: () => this.setData({ showPay: true })
    });
  },

  payNow() {
    if (this.data.paying) return;
    this.setData({ paying: true });
    wx.showLoading({ title: '支付中...' });
    wx.request({
      url: app.globalData.baseUrl + '/api/payment/pay',
      method: 'POST',
      header: { 'content-type': 'application/json' },
      data: {
        appointmentId: this.data.currentAppointment.appointmentId,
        paymentMethod: 'wechat',
        amount: Number(this.data.fee) || 30
      },
      success: (res) => {
        wx.hideLoading();
        this.setData({ paying: false });
        if (res.data && res.data.success) {
          const list = wx.getStorageSync('appointments') || [];
          list.unshift({
            appointmentId: this.data.currentAppointment.appointmentId,
            doctorName: this.data.selectedDoctor.name,
            deptName: this.data.selectedDept.deptName,
            hospitalName: this.data.hospitalName,
            appointmentDate: this.data.selectedDate,
            timeSlot: this.data.selectedTime,
            status: 'pending',
            createTime: new Date().toISOString()
          });
          wx.setStorageSync('appointments', list);
          this.setData({ showPay: false, success: true });
        } else {
          wx.showToast({ title: (res.data && res.data.message) || '支付失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.hideLoading();
        this.setData({ paying: false });
        wx.showToast({ title: '网络异常，支付未完成', icon: 'none' });
      }
    });
  },

  cancelPay() {
    this.setData({ showPay: false });
    wx.showToast({ title: '已提交预约，可稍后支付', icon: 'none' });
  },

  goMyAppointments() {
    wx.switchTab({ url: '/pages/my-appointments/my-appointments' });
  },

  goBack() {
    if (this.data.success) {
      wx.navigateBack();
      return;
    }
    wx.navigateBack();
  }
});
