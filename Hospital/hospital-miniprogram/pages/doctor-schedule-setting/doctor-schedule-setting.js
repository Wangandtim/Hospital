const app = getApp();

Page({
  data: {
    doctorId: '',
    weeklySchedule: {},
    days: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
    selectedDay: null,
    showTimeSlotModal: false,
    slotDurations: ['15', '20', '30', '45', '60'],
    maxPatientOptions: ['1', '2', '3', '4', '5'],
    newTimeSlot: {
      startTime: '',
      endTime: '',
      slotDuration: 20,
      maxPatientsPerSlot: 2
    },
    slotDurationIndex: 1,
    maxPatientsIndex: 1
  },

  onLoad(options) {
    if (options.doctorId) {
      this.setData({ doctorId: options.doctorId });
      this.loadWeeklySchedule(options.doctorId);
    } else {
      const doctorId = wx.getStorageSync('doctorId');
      if (doctorId) {
        this.setData({ doctorId: doctorId });
        this.loadWeeklySchedule(doctorId);
      }
    }
  },

  getDayEnabled(day) {
    const dayKey = 'day' + day;
    return this.data.weeklySchedule[dayKey] && this.data.weeklySchedule[dayKey].enabled;
  },

  getTimeSlots(day) {
    const dayKey = 'day' + day;
    return this.data.weeklySchedule[dayKey] && this.data.weeklySchedule[dayKey].timeSlots ? this.data.weeklySchedule[dayKey].timeSlots : [];
  },

  loadWeeklySchedule(doctorId) {
    wx.showLoading({ title: '加载中...' });
    wx.request({
      url: app.globalData.baseUrl + `/api/doctor-schedule-settings/doctor/${doctorId}/weekly`,
      method: 'GET',
      success: (res) => {
        wx.hideLoading();
        if (res.data && res.data.success) {
          this.setData({ weeklySchedule: res.data.data });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '加载失败', icon: 'none' });
      }
    });
  },

  toggleDay(e) {
    const dayOfWeek = e.currentTarget.dataset.day;
    const dayInfo = this.data.weeklySchedule['day' + dayOfWeek];
    const enabled = !dayInfo.enabled;

    wx.request({
      url: app.globalData.baseUrl + `/api/doctor-schedule-settings/doctor/${this.data.doctorId}/toggle-day`,
      method: 'PUT',
      data: {
        dayOfWeek: dayOfWeek,
        enabled: enabled
      },
      success: (res) => {
        if (res.data && res.data.success) {
          dayInfo.enabled = enabled;
          this.setData({ weeklySchedule: { ...this.data.weeklySchedule } });
          wx.showToast({ title: enabled ? '已启用' : '已禁用', icon: 'success' });
        }
      }
    });
  },

  openTimeSlotModal(e) {
    this.setData({
      selectedDay: e.currentTarget.dataset.day,
      showTimeSlotModal: true,
      newTimeSlot: {
        startTime: '',
        endTime: '',
        slotDuration: 20,
        maxPatientsPerSlot: 2
      }
    });
  },

  closeTimeSlotModal() {
    this.setData({ showTimeSlotModal: false });
  },

  // 阻止弹层内部点击冒泡到遮罩
  stopPropagation() {},

  onStartTimeChange(e) {
    this.setData({ 'newTimeSlot.startTime': e.detail.value });
  },

  onEndTimeChange(e) {
    this.setData({ 'newTimeSlot.endTime': e.detail.value });
  },

  onSlotDurationChange(e) {
    const index = e.detail.value;
    this.setData({ 
      slotDurationIndex: index,
      'newTimeSlot.slotDuration': parseInt(this.data.slotDurations[index]) 
    });
  },

  onMaxPatientsChange(e) {
    const index = e.detail.value;
    this.setData({ 
      maxPatientsIndex: index,
      'newTimeSlot.maxPatientsPerSlot': parseInt(this.data.maxPatientOptions[index]) 
    });
  },

  saveTimeSlot() {
    const { startTime, endTime, slotDuration, maxPatientsPerSlot } = this.data.newTimeSlot;
    
    if (!startTime || !endTime) {
      wx.showToast({ title: '请填写时间', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '保存中...' });
    wx.request({
      url: app.globalData.baseUrl + `/api/doctor-schedule-settings/doctor/${this.data.doctorId}/add-time-slot`,
      method: 'POST',
      data: {
        dayOfWeek: this.data.selectedDay,
        startTime: startTime + ':00',
        endTime: endTime + ':00',
        slotDuration: slotDuration,
        maxPatientsPerSlot: maxPatientsPerSlot
      },
      success: (res) => {
        wx.hideLoading();
        if (res.data && res.data.success) {
          this.closeTimeSlotModal();
          this.loadWeeklySchedule(this.data.doctorId);
          wx.showToast({ title: '添加成功', icon: 'success' });
        } else {
          wx.showToast({ title: res.data?.message || '添加失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '添加失败', icon: 'none' });
      }
    });
  },

  deleteTimeSlot(e) {
    const settingId = e.currentTarget.dataset.id;
    
    wx.showModal({
      title: '确认删除',
      content: '确定要删除这个时间段吗？',
      success: (res) => {
        if (res.confirm) {
          wx.request({
            url: app.globalData.baseUrl + `/api/doctor-schedule-settings/${settingId}`,
            method: 'DELETE',
            success: (res) => {
              if (res.data && res.data.success) {
                this.loadWeeklySchedule(this.data.doctorId);
                wx.showToast({ title: '删除成功', icon: 'success' });
              }
            }
          });
        }
      }
    });
  },

  previewSchedule() {
    wx.showModal({
      title: '预览可预约时间',
      content: '患者将根据您设置的时间进行预约，每个时间段最多' + this.data.newTimeSlot.maxPatientsPerSlot + '名患者预约',
      showCancel: false
    });
  }
});