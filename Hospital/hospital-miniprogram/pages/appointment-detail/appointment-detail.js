const app = getApp();
const placeholder = require('../../utils/placeholder');
const deptIcon = require('../../utils/dept-icon');
const qrCanvas = require('../../utils/qr-canvas');

const VOUCHER_MINUTES = 30;

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
    canCancel: false,
    voucherCode: '',
    voucherShort: '',
    voucherLoading: false,
    voucherTip: '',
    voucherCountdown: ''
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
            deptIcon: deptIcon.iconPath((doctor.department && doctor.department.deptName) || ''),
            patientName: patient.name || '',
            patientPhone: patient.phone || '',
            createTime: String(data.createTime || '').replace('T', ' ').slice(0, 16),
            // 序号与医保金额以服务端为准，未报到时序号为空，不再使用本地占位值
            queueNo: data.queueNo || '—',
            insurance: data.feeDetail && data.feeDetail.insurance != null ? data.feeDetail.insurance : 0,
            fee: data.fee || 30,
            flowCurrent: flowCurrent,
            statusState: status === 1 ? 'ok' : status === 0 ? 'full' : 'ok',
            statusTone: status === 1 ? 'wait' : status === 2 ? 'done' : status === 3 ? 'done' : 'cancel'
          }
        });

        if (doctor.hospitalId) {
          this.loadHospital(doctor.hospitalId);
        }

        if (status === 1) {
          this.fetchVoucher();
        } else {
          this.setData({
            voucherCode: '',
            voucherTip: '当前预约状态不可出示就诊码'
          });
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

  onUnload() {
    if (this._voucherTimer) {
      clearInterval(this._voucherTimer);
      this._voucherTimer = null;
    }
    if (this._voucherDrawTimer) {
      clearTimeout(this._voucherDrawTimer);
      this._voucherDrawTimer = null;
    }
  },

  fetchVoucher() {
    const detail = this.data.detail;
    if (!detail || !detail.appointmentId) return;
    if (this.data.voucherLoading) return;

    const token = wx.getStorageSync('authToken');
    this.setData({ voucherLoading: true, voucherTip: '', voucherCountdown: '' });

    wx.request({
      url: app.globalData.baseUrl + '/api/appointments/' + detail.appointmentId + '/voucher',
      method: 'POST',
      header: token ? { 'X-Auth-Token': token } : {},
      success: (res) => {
        const body = res.data;
        let code = '';
        let message = '';
        if (typeof body === 'string') {
          code = body.trim();
        } else if (body && typeof body === 'object') {
          code = String(body.voucher || body.data || '').trim();
          message = body.message || body.error || '';
        }

        if (!code) {
          this.setData({
            voucherLoading: false,
            voucherCode: '',
            voucherShort: '',
            voucherTip: message || '当前预约不可出示就诊码，或已超过就诊日期'
          });
          return;
        }

        this.setData({
          voucherCode: code,
          voucherShort: code.length > 18 ? code.slice(0, 18) + '…' : code,
          voucherLoading: false,
          voucherTip: '',
          voucherCountdown: ''
        });
        this.startVoucherCountdown();
        if (this._voucherDrawTimer) clearTimeout(this._voucherDrawTimer);
        this._voucherDrawTimer = setTimeout(() => {
          qrCanvas.draw('voucherQr', code, this, 200);
        }, 120);
      },
      fail: () => {
        this.setData({
          voucherLoading: false,
          voucherCode: '',
          voucherShort: '',
          voucherTip: '凭证获取失败，请稍后重试'
        });
      }
    });
  },

  copyVoucher() {
    if (!this.data.voucherCode) return;
    wx.setClipboardData({
      data: this.data.voucherCode,
      success: () => wx.showToast({ title: '凭证已复制', icon: 'none' })
    });
  },

  startVoucherCountdown() {
    if (this._voucherTimer) {
      clearInterval(this._voucherTimer);
    }
    const expireAt = Date.now() + VOUCHER_MINUTES * 60 * 1000;

    const update = () => {
      const left = expireAt - Date.now();
      if (left <= 0) {
        clearInterval(this._voucherTimer);
        this._voucherTimer = null;
        this.setData({
          voucherCode: '',
          voucherShort: '',
          voucherCountdown: '',
          voucherTip: '就诊码已失效，请重新获取'
        });
        return;
      }
      const seconds = Math.floor(left / 1000);
      const mm = String(Math.floor(seconds / 60)).padStart(2, '0');
      const ss = String(seconds % 60).padStart(2, '0');
      this.setData({ voucherCountdown: `凭证 ${mm}:${ss} 后失效` });
    };

    update();
    this._voucherTimer = setInterval(update, 1000);
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
