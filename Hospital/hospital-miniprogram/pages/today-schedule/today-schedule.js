const app = getApp();

Page({
  data: {
    currentDate: '',
    currentTimeSlot: '',
    scheduleList: [],
    showModal: false,
    scheduleAppointments: []
  },

  onLoad() {
    const today = new Date();
    const dateStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
    this.setData({ currentDate: dateStr });
    this.loadTodaySchedule();
  },

  loadTodaySchedule() {
    const doctorId = wx.getStorageSync('doctorId');
    const today = new Date().toISOString().split('T')[0];

    wx.showLoading({ title: '加载中...' });

    wx.request({
      url: `${app.globalData.baseUrl}/api/schedules/doctor/${doctorId}/date/${today}`,
      method: 'GET',
      success: (res) => {
        wx.hideLoading();
        if (res.statusCode === 200) {
          this.setData({ scheduleList: res.data });
        } else {
          wx.showToast({
            title: res.data.message || '加载失败',
            icon: 'none'
          });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({
          title: '网络错误',
          icon: 'none'
        });
      }
    });
  },

  getScheduleStatusText(item) {
    if (item.bookedCount >= item.maxPatients) {
      return '已满';
    }
    if (item.status === 0) {
      return '已取消';
    }
    return '可预约';
  },

  getScheduleStatusClass(item) {
    if (item.bookedCount >= item.maxPatients) {
      return 'status-full';
    }
    if (item.status === 0) {
      return 'status-cancelled';
    }
    return 'status-available';
  },

  getStatusText(status) {
    const statusMap = {
      0: '已取消',
      1: '待就诊',
      2: '已完成',
      3: '进行中'
    };
    return statusMap[status] || '未知';
  },

  getStatusClass(status) {
    const classMap = {
      0: 'status-cancelled',
      1: 'status-pending',
      2: 'status-completed',
      3: 'status-inprogress'
    };
    return classMap[status] || 'status-pending';
  },

  viewAppointments(e) {
    const scheduleId = e.currentTarget.dataset.scheduleId;
    const schedule = this.data.scheduleList.find(s => s.scheduleId === scheduleId);
    
    this.setData({
      showModal: true,
      currentTimeSlot: schedule ? schedule.timeSlot : ''
    });

    this.loadScheduleAppointments(scheduleId);
  },

  loadScheduleAppointments(scheduleId) {
    wx.request({
      url: `${app.globalData.baseUrl}/api/appointments/schedule/${scheduleId}`,
      method: 'GET',
      success: (res) => {
        if (res.statusCode === 200) {
          const appointments = res.data.map(appt => {
            const patient = appt.patient;
            return {
              appointmentId: appt.appointmentId,
              patientName: patient ? patient.name : '未知患者',
              patientPhone: patient ? patient.phone : '无联系方式',
              status: appt.status
            };
          });
          this.setData({ scheduleAppointments: appointments });
        } else {
          this.setData({ scheduleAppointments: [] });
        }
      },
      fail: () => {
        this.setData({ scheduleAppointments: [] });
      }
    });
  },

  closeModal() {
    this.setData({
      showModal: false,
      currentTimeSlot: '',
      scheduleAppointments: []
    });
  },

  goBack() {
    wx.navigateBack();
  },

  noop() {
    // 空函数，用于阻止事件冒泡
  }
});