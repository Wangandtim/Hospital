const app = getApp();
const placeholder = require('../../utils/placeholder');

const STATUS_TEXT = {
  0: '已取消',
  1: '待就诊',
  2: '已完成',
  3: '进行中'
};

const FLOW_STEP = {
  0: 0,
  1: 1,
  2: 4,
  3: 2
};

Page({
  data: {
    appointmentId: '',
    loading: true,
    loaded: false,
    detail: null,
    hospitalName: '',
    canCancel: false
  },

  onLoad(options) {
    if (!options.id) {
      wx.showToast({ title: '缺少预约编号', icon: 'none' });
      return;
    }
    this.setData({ appointmentId: options.id });
    this.loadDetail();
  },

  loadDetail() {
    wx.showLoading({ title: '加载中...' });
    wx.request({
      url: app.globalData.baseUrl + '/api/appointments/' + this.data.appointmentId,
      method: 'GET',
      success: (res) => {
        wx.hideLoading();
        const data = res.data || {};
        if (!data.success) {
          this.setData({ loading: false, loaded: true, detail: null });
          wx.showToast({ title: data.message || '预约不存在', icon: 'none' });
          return;
        }

        const doctor = data.doctor || {};
        const patient = data.patient || {};
        const status = data.status;

        const extra = placeholder.appointmentExtra(data);
        const flowCurrent = FLOW_STEP[status] !== undefined ? FLOW_STEP[status] : 1;
        const dateText = String(data.appointmentDate || '');

        this.setData({
          loading: false,
          loaded: true,
          canCancel: status === 1,
          detail: {
            appointmentId: data.appointmentId,
            appointmentDate: dateText,
            dayText: dateText.slice(8, 10) || '--',
            monthText: dateText.slice(5, 7) ? dateText.slice(5, 7) + ' 月' : '',
            timeSlot: data.timeSlot || '',
            status: status,
            statusText: STATUS_TEXT[status] || '未知状态',
            doctorId: doctor.doctorId || data.doctorId || '',
            doctorName: doctor.name || '未知医生',
            doctorTitle: doctor.title || '',
            deptName: (doctor.department && doctor.department.deptName) || '',
            patientName: patient.name || '',
            patientPhone: patient.phone || '',
            createTime: String(data.createTime || '').replace('T', ' ').slice(0, 16),
            queueNo: extra.queueNo,
            insurance: extra.insurance,
            fee: data.fee || 30,
            flowCurrent: flowCurrent,
            statusTone: status === 1 ? 'wait' : status === 2 ? 'done' : status === 3 ? 'done' : 'cancel'
          }
        });

        if (doctor.hospitalId) {
          this.loadHospital(doctor.hospitalId);
        }
      },
      fail: () => {
        wx.hideLoading();
        this.setData({ loading: false, loaded: true, detail: null });
        wx.showToast({ title: '网络异常，请稍后重试', icon: 'none' });
      }
    });
  },

  loadHospital(hospitalId) {
    wx.request({
      url: app.globalData.baseUrl + '/api/hospitals/' + hospitalId,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.hospitalName) {
          this.setData({ hospitalName: res.data.hospitalName });
        }
      }
    });
  },

  cancelAppointment() {
    wx.showModal({
      title: '确认取消',
      content: '确定要取消这个预约吗？',
      success: (res) => {
        if (!res.confirm) return;
        wx.showLoading({ title: '取消中...' });
        wx.request({
          url: app.globalData.baseUrl + '/api/appointments/' + this.data.appointmentId + '/cancel',
          method: 'POST',
          success: (resp) => {
            wx.hideLoading();
            if (resp.statusCode === 200) {
              wx.showToast({ title: '已取消', icon: 'success' });
              this.loadDetail();
            } else {
              wx.showToast({ title: (resp.data && resp.data.error) || '取消失败', icon: 'none' });
            }
          },
          fail: () => {
            wx.hideLoading();
            wx.showToast({ title: '取消失败，请稍后再试', icon: 'none' });
          }
        });
      }
    });
  },

  bookAgain() {
    const detail = this.data.detail;
    if (!detail || !detail.doctorId) return;
    wx.redirectTo({
      url: '/pages/appointment/appointment?doctorId=' + detail.doctorId
    });
  },

  goBack() {
    wx.navigateBack({
      fail: () => wx.switchTab({ url: '/pages/my-appointments/my-appointments' })
    });
  }
});
