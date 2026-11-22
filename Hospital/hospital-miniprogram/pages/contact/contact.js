const app = getApp();
const support = require('../../config/support');
const deptIcon = require('../../utils/dept-icon');

Page({
  data: {
    hotline: support.hotline,
    email: support.email,
    workTime: support.workTime,
    emergencyPhone: support.emergencyPhone,

    hospital: null,
    appointment: null,
    appointmentDeptIcon: '',
    appointmentCount: 0,
    loading: true,
    feedbackTargetName: '平台客服',

    rating: 5,
    ratingStars: [1, 2, 3, 4, 5],
    tagOptions: [
      { name: '候诊时间', active: false },
      { name: '服务态度', active: false },
      { name: '就诊环境', active: false },
      { name: '缴费流程', active: false },
      { name: '其他', active: false }
    ],
    selectedTags: [],
    content: '',
    contact: '',
    submitting: false,
    lastTicket: ''
  },

  onShow() {
    this.loadContext();
  },

  // 读取患者待就诊预约，并匹配医院联系方式
  loadContext() {
    const patientId = wx.getStorageSync('patientId');
    if (!patientId) {
      this.useFallbackHospital();
      return;
    }

    wx.request({
      url: app.globalData.baseUrl + '/api/appointments/my/' + patientId + '/page',
      method: 'GET',
      data: { page: 0, size: 20 },
      success: (res) => {
        // 分页接口返回 { data: [...] }，兼容历史 list 结构
        const list = (res.data && (res.data.data || res.data.content)) || [];
        const rows = Array.isArray(list) ? list : [];
        const today = this.formatDate(new Date());

        const isPending = (item) => {
          const status = item.status;
          return status === 1 || status === '1' || status === 3 || status === '3';
        };

        const upcoming = rows
          .filter((item) => isPending(item) && String(item.appointmentDate || '') >= today)
          .sort((a, b) => {
            const dateCompare = String(a.appointmentDate).localeCompare(String(b.appointmentDate));
            if (dateCompare !== 0) return dateCompare;
            return String(a.timeSlot || '').localeCompare(String(b.timeSlot || ''));
          });

        // 没有待就诊预约时，展示最近一次已完成的就诊，仍可直接联系医院
        const history = rows
          .filter((item) => item.status === 2 || item.status === '2')
          .sort((a, b) => String(b.appointmentDate).localeCompare(String(a.appointmentDate)));

        const target = upcoming[0] || history[0] || null;

        if (!target) {
          this.useFallbackHospital();
          return;
        }

        const appointment = {
          appointmentId: target.appointmentId,
          stateText: upcoming.length > 0 ? '待就诊' : '最近就诊',
          dateText: this.formatDateText(target.appointmentDate),
          timeSlot: target.timeSlot || '',
          doctorName: target.doctorName || '',
          doctorTitle: target.doctorTitle || '',
          deptName: target.deptName || '',
          hospitalName: target.hospitalName || ''
        };

        this.setData({
          appointment: appointment,
          appointmentDeptIcon: deptIcon.iconPath(appointment.deptName),
          appointmentCount: upcoming.length
        });

        this.resolveHospital(target.hospitalName, appointment);
      },
      fail: () => {
        this.useFallbackHospital();
      }
    });
  },

  // 用后端医院库补全电话、地址与坐标；失败则回退到首页选中医院
  resolveHospital(hospitalName, appointment) {
    wx.request({
      url: app.globalData.baseUrl + '/api/hospitals/page',
      method: 'GET',
      data: { page: 0, size: 20, keyword: hospitalName },
      success: (res) => {
        const list = (res.data && res.data.content) || [];
        const matched = list.find((h) => h.hospitalName === hospitalName);
        if (matched) {
          this.setData({
            hospital: {
              hospitalId: matched.hospitalId,
              name: matched.hospitalName,
              phone: matched.phone || '',
              address: matched.address || '',
              latitude: matched.latitude,
              longitude: matched.longitude,
              level: matched.level || ''
            },
            feedbackTargetName: matched.hospitalName,
            loading: false
          });
        } else {
          this.useFallbackHospital(appointment);
        }
      },
      fail: () => {
        this.useFallbackHospital(appointment);
      }
    });
  },

  useFallbackHospital(appointment) {
    const selected = wx.getStorageSync('selectedHospital');
    if (selected && (selected.hospitalId || selected.hospitalName)) {
      this.setData({
        hospital: {
          hospitalId: selected.hospitalId,
          name: selected.hospitalName,
          phone: selected.phone || '',
          address: selected.address || '',
          latitude: selected.latitude,
          longitude: selected.longitude,
          level: selected.level || ''
        },
        feedbackTargetName: selected.hospitalName || '平台客服',
        appointment: appointment || null,
        appointmentDeptIcon: deptIcon.iconPath(appointment ? appointment.deptName : ''),
        loading: false
      });
      return;
    }

    this.setData({
      hospital: null,
      feedbackTargetName: '平台客服',
      appointment: appointment || null,
      appointmentDeptIcon: deptIcon.iconPath(appointment ? appointment.deptName : ''),
      loading: false
    });
  },

  formatDate(date) {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  },

  formatDateText(value) {
    if (!value) return '';
    const text = String(value);
    if (text.length < 10) return text;
    return `${text.slice(5, 7)}月${text.slice(8, 10)}日`;
  },

  callHotline() {
    wx.makePhoneCall({
      phoneNumber: support.hotline,
      fail: () => wx.showToast({ title: '拨号已取消', icon: 'none' })
    });
  },

  callEmergency() {
    wx.makePhoneCall({
      phoneNumber: support.emergencyPhone,
      fail: () => wx.showToast({ title: '拨号已取消', icon: 'none' })
    });
  },

  callHospital() {
    if (!this.data.hospital || !this.data.hospital.phone) {
      wx.showToast({ title: '该医院暂无联系电话', icon: 'none' });
      return;
    }
    wx.makePhoneCall({
      phoneNumber: this.data.hospital.phone,
      fail: () => wx.showToast({ title: '拨号已取消', icon: 'none' })
    });
  },

  navigateToHospital() {
    const hospital = this.data.hospital;
    if (!hospital || !hospital.latitude || !hospital.longitude) {
      wx.showToast({ title: '暂无医院坐标', icon: 'none' });
      return;
    }
    wx.openLocation({
      latitude: Number(hospital.latitude),
      longitude: Number(hospital.longitude),
      name: hospital.name,
      address: hospital.address,
      scale: 16
    });
  },

  goAppointments() {
    wx.switchTab({ url: '/pages/my-appointments/my-appointments' });
  },

  goSelectHospital() {
    wx.navigateTo({ url: '/pages/hospital-list/hospital-list' });
  },

  copyText(e) {
    const text = e.currentTarget.dataset.text;
    if (!text) return;
    wx.setClipboardData({
      data: text,
      success: () => wx.showToast({ title: '已复制', icon: 'success' })
    });
  },

  setRating(e) {
    const value = Number(e.currentTarget.dataset.value);
    this.setData({ rating: value });
    if (typeof wx.vibrateShort === 'function') {
      wx.vibrateShort({ type: 'light', fail: () => {} });
    }
  },

  toggleTag(e) {
    const index = Number(e.currentTarget.dataset.index);
    const options = this.data.tagOptions.slice();
    const target = options[index];
    if (!target) return;

    if (!target.active) {
      const activeCount = options.filter((item) => item.active).length;
      if (activeCount >= 4) {
        wx.showToast({ title: '最多选择 4 个标签', icon: 'none' });
        return;
      }
    }

    options[index] = { name: target.name, active: !target.active };
    const selectedTags = options.filter((item) => item.active).map((item) => item.name);
    this.setData({ tagOptions: options, selectedTags: selectedTags });
  },

  onContentInput(e) {
    this.setData({ content: e.detail.value });
  },

  onContactInput(e) {
    this.setData({ contact: e.detail.value });
  },

  submitFeedback() {
    if (this.data.submitting) return;

    const content = this.data.content.trim();
    if (content.length < 5) {
      wx.showToast({ title: '请至少填写 5 个字', icon: 'none' });
      return;
    }

    const hospital = this.data.hospital;
    const appointment = this.data.appointment;
    this.setData({ submitting: true });

    wx.request({
      url: app.globalData.baseUrl + '/api/feedback',
      method: 'POST',
      header: { 'content-type': 'application/json' },
      data: {
        patientId: wx.getStorageSync('patientId') || '',
        hospitalId: hospital ? hospital.hospitalId : '',
        hospitalName: hospital ? hospital.name : '平台客服',
        appointmentId: appointment ? appointment.appointmentId : '',
        rating: this.data.rating,
        tags: this.data.selectedTags.join(','),
        content: content,
        contact: this.data.contact.trim()
      },
      success: (res) => {
        this.setData({ submitting: false });
        if (res.statusCode === 200 && res.data && res.data.success) {
          const ticket = (res.data.feedbackId || '').slice(-8).toUpperCase();
          this.setData({
            lastTicket: ticket,
            content: '',
            contact: '',
            selectedTags: [],
            rating: 5,
            tagOptions: this.data.tagOptions.map((item) => ({ name: item.name, active: false }))
          });
          wx.showModal({
            title: '反馈已提交',
            content: `工单号 ${ticket}\n医院服务台会尽快查看，紧急问题请直接拨打医院电话。`,
            showCancel: false,
            confirmText: '知道了'
          });
        } else if (res.statusCode === 401) {
          wx.showToast({ title: '登录已失效，请重新登录后提交', icon: 'none' });
        } else {
          wx.showToast({
            title: (res.data && (res.data.message || res.data.error)) || '提交失败，请稍后再试',
            icon: 'none'
          });
        }
      },
      fail: () => {
        this.setData({ submitting: false });
        wx.showToast({ title: '网络异常，反馈未提交', icon: 'none' });
      }
    });
  },

  goHelp() {
    wx.navigateTo({ url: '/pages/help/help' });
  },

  goSettings() {
    wx.navigateTo({ url: '/pages/settings/settings' });
  }
});
