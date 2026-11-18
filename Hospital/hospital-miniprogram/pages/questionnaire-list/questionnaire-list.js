const app = getApp();
const questionnaireConfig = require('../../utils/questionnaire-config');

Page({
  data: {
    questionnaires: [],
    showModal: false,
    currentPatientName: '',
    currentAppointmentDate: '',
    currentTimeSlot: '',
    currentAnswers: []
  },

  onLoad() {
    this.loadQuestionnaires();
  },

  loadQuestionnaires() {
    const doctorUsername = wx.getStorageSync('doctorUsername');
    
    wx.showLoading({ title: '加载中...' });

    wx.request({
      url: `${app.globalData.baseUrl}/api/appointments/doctor/by-username/${doctorUsername}`,
      method: 'GET',
      success: (res) => {
        wx.hideLoading();
        
        if (res.statusCode === 200) {
          const questionnaires = res.data
            .filter(appt => appt.questionnaireData)
            .map(appt => {
              let answersArray = [];
              const parsedQuestionnaire = questionnaireConfig.parse(
                appt.questionnaireData,
                appt.deptId,
                appt.deptName || (appt.doctor && appt.doctor.deptName)
              );
              if (parsedQuestionnaire) {
                answersArray = parsedQuestionnaire.answers;
              }
              
              return {
                appointmentId: appt.appointmentId,
                patientName: appt.patient ? appt.patient.name : '未知患者',
                appointmentDate: appt.appointmentDate,
                timeSlot: appt.timeSlot,
                answers: answersArray
              };
            });

          this.setData({ questionnaires });
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


  viewQuestionnaire(e) {
    const index = e.currentTarget.dataset.index;
    const questionnaire = this.data.questionnaires[index];
    this.setData({
      showModal: true,
      currentPatientName: questionnaire.patientName || '未知患者',
      currentAppointmentDate: questionnaire.appointmentDate,
      currentTimeSlot: questionnaire.timeSlot,
      currentAnswers: questionnaire.answers
    });
  },

  closeModal() {
    this.setData({
      showModal: false,
      currentPatientName: '',
      currentAppointmentDate: '',
      currentTimeSlot: '',
      currentAnswers: []
    });
  },

  goBack() {
    wx.navigateBack();
  },

  noop() {
    // 空函数，用于阻止事件冒泡
  }
});
