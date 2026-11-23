const app = getApp();

Page({
  data: {
    selectedDate: '',
    selectedDateLabel: '',
    selectedSlots: [],
    disabledSlots: [],
    remark: '',
    requiredSlots: 6,
    maxSlots: 18,
    patientOptions: ['1', '2', '3', '4', '5'],
    patientIndex: 1,
    submitting: false,
    canConfirm: false,
    slotCells: [],
    timeSlots: [
      '08:00-08:20', '08:20-08:40', '08:40-09:00',
      '09:00-09:20', '09:20-09:40', '09:40-10:00',
      '10:00-10:20', '10:20-10:40', '10:40-11:00',
      '11:00-11:20', '11:20-11:40', '11:40-12:00',
      '14:00-14:20', '14:20-14:40', '14:40-15:00',
      '15:00-15:20', '15:20-15:40', '15:40-16:00',
      '16:00-16:20', '16:20-16:40', '16:40-17:00',
      '17:00-17:20', '17:20-17:40', '17:40-18:00',
      '18:00-18:20', '18:20-18:40', '18:40-19:00',
      '19:00-19:20', '19:20-19:40', '19:40-20:00',
      '20:00-20:20', '20:20-20:40', '20:40-21:00'
    ]
  },

  onLoad(options) {
    const date = (options && options.date) || '';
    this.refreshSlotCells();
    if (date) {
      this.setData({ selectedDate: date, selectedDateLabel: this.formatDateLabel(date) });
      this.loadDisabledSlotsForDate(date);
    }
  },

  formatLocalDate(date) {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  },

  formatDateLabel(dateStr) {
    if (!dateStr) return '';
    const parts = dateStr.split('-');
    if (parts.length !== 3) return dateStr;
    return `${parseInt(parts[1])}月${parseInt(parts[2])}日`;
  },

  toMinutes(timeStr) {
    const parts = String(timeStr || '').trim().split(':');
    if (parts.length < 2) return 0;
    return parseInt(parts[0]) * 60 + parseInt(parts[1]);
  },

  updateCanConfirm() {
    const canConfirm = !!this.data.selectedDate &&
      this.data.selectedSlots.length >= this.data.requiredSlots;
    this.setData({ canConfirm });
  },

  refreshSlotCells() {
    const selected = this.data.selectedSlots;
    const disabled = this.data.disabledSlots;
    this.setData({
      slotCells: this.data.timeSlots.map((time, index) => ({
        time,
        selected: selected.indexOf(index) !== -1,
        disabled: disabled.indexOf(index) !== -1
      }))
    });
  },

  // 解析 /api/schedules/doctor/{id}/date/{date} 的 {success, data} 包装
  loadDisabledSlotsForDate(date) {
    const doctorId = wx.getStorageSync('doctorId');
    if (!doctorId || !date) return;

    wx.request({
      url: app.globalData.baseUrl + '/api/schedules/doctor/' + doctorId + '/date/' + date,
      method: 'GET',
      success: (res) => {
        let schedules = [];
        if (res.data && res.data.success && Array.isArray(res.data.data)) {
          schedules = res.data.data;
        } else if (Array.isArray(res.data)) {
          schedules = res.data;
        }

        const disabled = [];
        schedules.forEach(schedule => {
          const parts = String(schedule.timeSlot || '').split('-');
          if (parts.length !== 2) return;
          const start = this.toMinutes(parts[0]);
          const end = this.toMinutes(parts[1]);
          this.data.timeSlots.forEach((slot, index) => {
            const slotParts = slot.split('-');
            const slotStart = this.toMinutes(slotParts[0]);
            const slotEnd = this.toMinutes(slotParts[1]);
            if (slotStart < end && start < slotEnd && disabled.indexOf(index) === -1) {
              disabled.push(index);
            }
          });
        });
        this.setData({ disabledSlots: disabled });
        this.refreshSlotCells();
      },
      fail: () => {
      }
    });
  },

  showDatePicker() {
    const today = new Date();
    const days = [];
    for (let i = 0; i < 14; i++) {
      const date = new Date(today);
      date.setDate(today.getDate() + i);
      const value = this.formatLocalDate(date);
      days.push({
        value,
        label: this.formatDateLabel(value)
      });
    }

    wx.showActionSheet({
      itemList: days.map(d => d.label),
      success: (res) => {
        const selected = days[res.tapIndex];
        this.setData({
          selectedDate: selected.value,
          selectedDateLabel: selected.label,
          selectedSlots: [],
          canConfirm: false
        });
        this.refreshSlotCells();
        this.loadDisabledSlotsForDate(selected.value);
      }
    });
  },

  toggleTimeSlot(e) {
    const index = parseInt(e.currentTarget.dataset.index);

    if (this.data.disabledSlots.indexOf(index) !== -1) {
      wx.showToast({ title: '该时段已被占用', icon: 'none', duration: 1500 });
      return;
    }

    const selectedSlots = [...this.data.selectedSlots];
    const slotIndex = selectedSlots.indexOf(index);

    if (slotIndex === -1) {
      if (selectedSlots.length >= this.data.maxSlots) {
        wx.showToast({ title: '最多选择 6 小时（18 个时段）', icon: 'none', duration: 1500 });
        return;
      }
      selectedSlots.push(index);
      wx.vibrateShort({ type: 'light' });
    } else {
      selectedSlots.splice(slotIndex, 1);
      wx.vibrateShort({ type: 'light' });
    }

    selectedSlots.sort((a, b) => a - b);
    this.setData({ selectedSlots });
    this.refreshSlotCells();
    this.updateCanConfirm();
  },

  isSlotSelected(index) {
    return this.data.selectedSlots.indexOf(index) !== -1;
  },

  isSlotDisabled(index) {
    return this.data.disabledSlots.indexOf(index) !== -1;
  },

  onRemarkInput(e) {
    this.setData({ remark: e.detail.value });
  },

  onPatientChange(e) {
    this.setData({ patientIndex: e.detail.value });
  },

  createSchedule(schedule) {
    return new Promise(resolve => {
      wx.request({
        url: app.globalData.baseUrl + '/api/schedules',
        method: 'POST',
        header: { 'Content-Type': 'application/json' },
        data: schedule,
        success: (res) => {
          resolve(!!(res.data && res.data.success));
        },
        fail: () => resolve(false)
      });
    });
  },

  confirmSchedule() {
    if (!this.data.selectedDate) {
      wx.showModal({ title: '还没选日期', content: '请先选择要设置保底排班的日期。', showCancel: false });
      return;
    }
    if (!this.data.canConfirm) {
      wx.showModal({
        title: '排班时长不足',
        content: `当前已选 ${this.data.selectedSlots.length} 个时段，每位医生每天至少 2 小时（6 个 20 分钟时段），请继续选择。`,
        showCancel: false
      });
      return;
    }
    if (this.data.submitting) {
      return;
    }

    const doctorId = wx.getStorageSync('doctorId');
    const hospitalId = wx.getStorageSync('hospitalId');
    if (!doctorId || !hospitalId) {
      wx.showModal({
        title: '登录状态失效',
        content: '登录信息已丢失，请重新登录后再设置排班。',
        showCancel: false,
        success: () => wx.reLaunch({ url: '/pages/login/login' })
      });
      return;
    }

    const maxPatients = parseInt(this.data.patientOptions[this.data.patientIndex]) || 2;

    const schedules = this.data.selectedSlots.map(index => ({
      doctorId: doctorId,
      hospitalId: hospitalId,
      workDate: this.data.selectedDate,
      timeSlot: this.data.timeSlots[index],
      maxPatients: maxPatients,
      status: 1
    }));

    this.setData({ submitting: true });
    wx.showLoading({ title: '保存中...' });

    Promise.all(schedules.map(item => this.createSchedule(item))).then(results => {
      wx.hideLoading();
      this.setData({ submitting: false });
      const failedCount = results.filter(ok => !ok).length;
      if (failedCount === 0) {
        wx.showToast({ title: '设置成功', icon: 'success', duration: 2000 });
        wx.setStorageSync('hasMinimumSchedule', true);
        setTimeout(() => {
          wx.redirectTo({ url: '/pages/my-schedule/my-schedule' });
        }, 1500);
        return;
      }

      wx.showModal({
        title: '部分时段未保存',
        content: `有 ${failedCount} 个时段保存失败（可能已被占用或网络超时），请重试或重新选择时段。`,
        confirmText: '重试',
        cancelText: '重新选择',
        success: (res) => {
          if (res.confirm) {
            this.confirmSchedule();
            return;
          }
          this.setData({ selectedSlots: [] });
          this.refreshSlotCells();
          this.updateCanConfirm();
          this.loadDisabledSlotsForDate(this.data.selectedDate);
        }
      });
    });
  },

  skipSetting() {
    wx.showModal({
      title: '提示',
      content: '未设置保底排班时，患者可能约不到您的号源，确定稍后设置吗？',
      success: (res) => {
        if (res.confirm) {
          wx.redirectTo({ url: '/pages/my-schedule/my-schedule' });
        }
      }
    });
  }
});
