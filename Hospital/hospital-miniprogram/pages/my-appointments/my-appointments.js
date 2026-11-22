const app = getApp();
const elderly = require('../../utils/elderly');
const placeholder = require('../../utils/placeholder');
const pager = require('../../utils/pager');
const deptIcon = require('../../utils/dept-icon');

const TABS = [
  { key: 'all', label: '全部' },
  { key: 'unpaid', label: '待付款' },
  { key: 'pending', label: '待就诊' },
  { key: 'completed', label: '已完成' },
  { key: 'cancelled', label: '已取消' }
];

// 服务端分类参数：待就诊对应 upcoming（已缴费且待就诊）
const FILTER_MAP = {
  all: 'all',
  unpaid: 'unpaid',
  pending: 'upcoming',
  completed: 'completed',
  cancelled: 'cancelled'
};

/**
 * 用接口语义字段分类，不直接依赖 status 数值：
 * 待付款 = status=1 且无缴费（后端给 payStatus=UNPAID / needPay=true / statusText=待付款）
 * 待就诊 = 已缴费的待就诊；已完成 / 已取消 以 statusText 为准
 */
function classify(item) {
  const text = String((item && item.statusText) || '');
  const payStatus = String((item && item.payStatus) || '').toUpperCase();
  const needPay = item && item.needPay === true;

  if (text.indexOf('已取消') >= 0) return 'cancelled';
  if (text.indexOf('已完成') >= 0) return 'completed';
  if (text.indexOf('进行中') >= 0 || text.indexOf('就诊中') >= 0) return 'pending';
  if (payStatus === 'UNPAID' || needPay || text.indexOf('待付款') >= 0) return 'unpaid';
  if (text.indexOf('待就诊') >= 0) return 'pending';

  // 兼容尚未补语义字段的旧响应
  if (item && item.status === 0) return 'cancelled';
  if (item && item.status === 2) return 'completed';
  return 'pending';
}

function statusPill(item) {
  const state = classify(item);
  if (state === 'unpaid') return 'pay';
  if (state === 'pending') return 'wait';
  if (state === 'completed') return 'done';
  return 'cancel';
}

Page({
  data: {
    currentTab: 'all',
    tabs: TABS,
    appointments: [],
    elderly: false,
    pager: { status: 'idle', hasMore: true, total: 0, error: '', loading: false },
    showScheduleCancelModal: false,
    cancelledScheduleInfo: null
  },

  onLoad() {
    this.createPager();
  },

  onShow() {
    this.setData({ elderly: elderly.isElderly() });
    this.reload();
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
      fetch: (page, size) => {
        const patientId = wx.getStorageSync('patientId');
        if (!patientId) return Promise.resolve({ data: [], total: 0, hasMore: false });
        const filter = FILTER_MAP[this.data.currentTab] || 'all';
        const url = app.globalData.baseUrl + '/api/appointments/my/' + patientId +
          '/page?page=' + page + '&size=' + size + '&filter=' + filter;
        return new Promise((resolve, reject) => {
          wx.request({
            url: url,
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
        });
      },
      onReset: () => {
        this.setData({ appointments: [] });
      },
      onData: state => {
        const appointments = (state.list || [])
          .map(item => {
            const extra = placeholder.appointmentExtra(item);
            const dateText = String(item.appointmentDate || '');
            return Object.assign({}, item, extra, {
              dayText: dateText.slice(8, 10) || '--',
              monthText: dateText.slice(5, 7) ? dateText.slice(5, 7) + ' 月' : '',
              state: classify(item),
              deptIcon: deptIcon.iconPath(item.deptName),
              pill: statusPill(item)
            });
          });
        this.setData({ appointments: appointments, pager: state.pager });
        this.checkCancelledSchedules();
      }
    });
  },

  reload() {
    if (this.pager) {
      this.pager.reset();
    }
  },

  retryLoad() {
    if (this.pager) {
      this.pager.retry();
    }
  },

  switchTab(e) {
    const key = e.currentTarget.dataset.key;
    if (!key || key === this.data.currentTab) return;
    this.setData({ currentTab: key });
    this.reload();
  },

  cancelAppointment(e) {
    const appointmentId = e.currentTarget.dataset.appointmentId;

    wx.showModal({
      title: '确认取消',
      content: '确定要取消这个预约吗？',
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '取消中...' });
          wx.request({
            url: app.globalData.baseUrl + '/api/appointments/' + appointmentId + '/cancel',
            method: 'POST',
            success: () => {
              wx.hideLoading();
              wx.showToast({ title: '已取消', icon: 'success' });
              this.reload();
            },
            fail: () => {
              wx.hideLoading();
              wx.showToast({ title: '取消失败', icon: 'none' });
            }
          });
        }
      }
    });
  },

  goToDetail(e) {
    const appointmentId = e.currentTarget.dataset.appointmentId;
    wx.navigateTo({
      url: '/pages/appointment-detail/appointment-detail?id=' + appointmentId
    });
  },

  goHome() {
    wx.switchTab({ url: '/pages/index/index' });
  },

  goTriage() {
    wx.navigateTo({ url: '/pages/triage/triage' });
  },

  checkCancelledSchedules() {
    const patientId = wx.getStorageSync('patientId');
    if (!patientId) return;

    const pendingAppointments = this.data.appointments.filter(a => a.state === 'pending');
    if (pendingAppointments.length === 0) return;

    pendingAppointments.forEach(appointment => {
      if (!appointment.scheduleId) return;
      wx.request({
        url: app.globalData.baseUrl + '/api/schedules/' + appointment.scheduleId,
        method: 'GET',
        success: (res) => {
          const schedule = res.data;
          if (schedule && (schedule.status === 0 || schedule.status === 'closed')) {
            this.setData({
              showScheduleCancelModal: true,
              cancelledScheduleInfo: {
                doctorId: appointment.doctorId,
                doctorName: appointment.doctorName || '未知医生',
                appointmentDate: appointment.appointmentDate,
                timeSlot: appointment.timeSlot,
                appointmentId: appointment.appointmentId
              }
            });
          }
        },
        fail: () => {}
      });
    });
  },

  closeScheduleCancelModal() {
    this.setData({ showScheduleCancelModal: false, cancelledScheduleInfo: null });
  },

  stopPropagation() {},

  reschedule() {
    const info = this.data.cancelledScheduleInfo;
    if (info && info.appointmentId) {
      const doctorId = info.doctorId;
      wx.showLoading({ title: '取消原预约...' });
      wx.request({
        url: app.globalData.baseUrl + '/api/appointments/' + info.appointmentId + '/cancel',
        method: 'POST',
        success: () => {
          wx.hideLoading();
          this.setData({ showScheduleCancelModal: false, cancelledScheduleInfo: null });
          wx.showToast({ title: '已取消原预约', icon: 'success' });
          setTimeout(() => {
            wx.navigateTo({ url: '/pages/appointment/appointment?doctorId=' + doctorId });
          }, 1500);
        },
        fail: () => {
          wx.hideLoading();
          this.setData({ showScheduleCancelModal: false, cancelledScheduleInfo: null });
          wx.showToast({ title: '取消失败', icon: 'none' });
        }
      });
    }
  }
});
