const app = getApp();
const elderly = require('../../utils/elderly');

Page({
  data: {
    currentTab: 0,
    appointments: [],
    elderly: false,
    loading: false,
    showScheduleCancelModal: false,
    cancelledScheduleInfo: null,
    // 分页相关
    currentPage: 0,
    pageSize: 10,
    total: 0,
    totalPages: 0,
    hasMore: true,
    loadingMore: false
  },

  onShow() {
    this.setData({ elderly: elderly.isElderly() });
    this.loadAppointments(true);
    this.checkCancelledSchedules();
  },

  switchTab(e) {
    const index = parseInt(e.currentTarget.dataset.index);
    this.setData({ 
      currentTab: index,
      currentPage: 0,
      appointments: [],
      hasMore: true
    });
    this.loadAppointments(true);
  },

  loadAppointments(reset = false) {
    const page = reset ? 0 : this.data.currentPage;
    this.setData({ loading: reset, loadingMore: !reset });
    
    const patientId = wx.getStorageSync('patientId');
    
    if (!patientId) {
      this.setData({ appointments: [], loading: false, loadingMore: false });
      return;
    }

    wx.request({
      url: app.globalData.baseUrl + '/api/appointments/my/' + patientId + '/page',
      method: 'GET',
      data: {
        page: page,
        size: this.data.pageSize
      },
      success: (res) => {
        let result = res.data || {};
        let data = result.data || [];
        
        data = data.map(a => {
          let status = 'pending';
          if (a.status === 'paid' || a.status === 'pending' || a.status === 1) {
            status = 'pending';
          } else if (a.status === 'completed' || a.status === 2) {
            status = 'completed';
          } else if (a.status === 'cancelled' || a.status === 0) {
            status = 'cancelled';
          } else if (a.status === 'inprogress' || a.status === 3) {
            status = 'inprogress';
          }
          
          return { 
            ...a, 
            status: status
          };
        });
        
        if (this.data.currentTab > 0) {
          const statusMap = { 1: 'pending', 2: 'completed', 3: 'cancelled' };
          data = data.filter(a => a.status === statusMap[this.data.currentTab]);
        }
        
        let newAppointments = reset ? data : [...this.data.appointments, ...data];
        
        this.setData({ 
          appointments: newAppointments,
          currentPage: result.currentPage || page,
          total: result.total || 0,
          totalPages: result.totalPages || 0,
          hasMore: newAppointments.length < (result.total || newAppointments.length),
          loading: false,
          loadingMore: false
        });
      },
      fail: () => {
        let appointments = wx.getStorageSync('appointments') || [];
        
        appointments = appointments.map(a => {
          if (a.status === 'paid' || a.status === 'pending' || a.status === 1) {
            return { ...a, status: 'pending' };
          } else if (a.status === 'completed' || a.status === 2) {
            return { ...a, status: 'completed' };
          } else if (a.status === 'cancelled' || a.status === 0) {
            return { ...a, status: 'cancelled' };
          } else if (a.status === 'inprogress' || a.status === 3) {
            return { ...a, status: 'inprogress' };
          }
          return { ...a, status: 'pending' };
        });
        
        if (this.data.currentTab > 0) {
          const statusMap = { 1: 'pending', 2: 'completed', 3: 'cancelled' };
          appointments = appointments.filter(a => a.status === statusMap[this.data.currentTab]);
        }
        
        this.setData({ 
          appointments: appointments,
          loading: false,
          loadingMore: false
        });
      }
    });
  },

  loadMore() {
    if (this.data.loadingMore || !this.data.hasMore) return;
    
    this.setData({ currentPage: this.data.currentPage + 1 });
    this.loadAppointments(false);
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
              this.loadAppointments(true);
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

  checkCancelledSchedules() {
    const patientId = wx.getStorageSync('patientId');
    if (!patientId) return;

    const pendingAppointments = this.data.appointments.filter(a => a.status === 'pending');
    
    if (pendingAppointments.length === 0) return;

    pendingAppointments.forEach(appointment => {
      if (appointment.scheduleId) {
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
          fail: () => {
            console.log('检查排班状态失败');
          }
        });
      }
    });
  },

  closeScheduleCancelModal() {
    this.setData({ showScheduleCancelModal: false, cancelledScheduleInfo: null });
  },

  stopPropagation() {},

  reschedule() {
    const info = this.data.cancelledScheduleInfo;
    if (info?.appointmentId) {
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
            wx.navigateTo({
              url: '/pages/appointment/appointment?doctorId=' + doctorId
            });
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
