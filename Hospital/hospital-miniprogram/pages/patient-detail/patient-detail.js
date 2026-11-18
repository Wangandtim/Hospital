const questionnaireConfig = require('../../utils/questionnaire-config');
const app = getApp();


Page({
  data: {
    patient: null,
    appointment: null,
    questionnaireData: null,
    parsedQuestionnaire: null,
    deptId: '',
    deptName: '',
    isLoading: true
  },

  onLoad(options) {
    const patientId = options.patientId;
    const appointmentId = options.appointmentId;
    const deptId = options.deptId;

    this.setData({ deptId: deptId || '' });

    if (appointmentId) {
      this.loadAppointmentDetail(appointmentId);
    } else if (patientId) {
      this.loadPatientDetail(patientId);
    }
  },

  loadAppointmentDetail(appointmentId) {
    wx.showLoading({ title: '加载中...' });
    wx.request({
      url: app.globalData.baseUrl + '/api/appointments/' + appointmentId,
      method: 'GET',
      success: (res) => {
        wx.hideLoading();
        if (res.data) {
          const appointment = res.data;
          this.setData({
            appointment: appointment,
            patient: appointment.patient,
            questionnaireData: appointment.questionnaireData
          });

          if (appointment.doctor && appointment.doctor.deptId) {
            this.setData({
              deptId: appointment.doctor.deptId,
              deptName: appointment.doctor.department ? appointment.doctor.department.deptName : ''
            });
            this.loadDeptName(appointment.doctor.deptId);
          }

          if (appointment.questionnaireData) {
            this.parseQuestionnaire(appointment.questionnaireData);
          }
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '加载失败', icon: 'none' });
      }
    });
  },

  loadPatientDetail(patientId) {
    wx.showLoading({ title: '加载中...' });
    wx.request({
      url: app.globalData.baseUrl + '/api/patients/' + patientId,
      method: 'GET',
      success: (res) => {
        wx.hideLoading();
        if (res.data) {
          this.setData({ patient: res.data });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '加载失败', icon: 'none' });
      }
    });
  },

  loadDeptName(deptId) {
    wx.request({
      url: app.globalData.baseUrl + '/api/departments/' + deptId,
      method: 'GET',
      success: (res) => {
        if (res.data) {
          this.setData({ deptName: res.data.deptName });
        }
      }
    });
  },

  parseQuestionnaire(questionnaireData) {
    const parsed = questionnaireConfig.parse(questionnaireData, this.data.deptId, this.data.deptName);
    if (parsed) {
      this.setData({ parsedQuestionnaire: parsed });
    }
  },

  goBack() {
    wx.navigateBack();
  },

  markCompleted() {
    if (!this.data.appointment) return;

    wx.showModal({
      title: '确认完成',
      content: '确认该患者已完成就诊？',
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '处理中...' });
          wx.request({
            url: app.globalData.baseUrl + '/api/appointments/' + this.data.appointment.appointmentId + '/complete',
            method: 'POST',
            success: () => {
              wx.hideLoading();
              wx.showToast({ title: '已完成', icon: 'success' });
              setTimeout(() => {
                wx.navigateBack();
              }, 1500);
            },
            fail: () => {
              wx.hideLoading();
              wx.showToast({ title: '操作失败', icon: 'none' });
            }
          });
        }
      }
    });
  }
});
