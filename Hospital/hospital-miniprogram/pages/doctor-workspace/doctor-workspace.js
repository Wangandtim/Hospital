const app = getApp();
const questionnaireConfig = require('../../utils/questionnaire-config');
const deptIcon = require('../../utils/dept-icon');

Page({
  data: {
    doctorName: '',
    doctorTitle: '',
    doctorDept: '',
    doctorDeptIcon: deptIcon.iconPath(''),
    todayCount: 0,
    inProgressCount: 0,
    completedCount: 0,
    pendingCount: 0,
    questionnaireCount: 0,
    appointments: [],
    expandedIndex: null,
    refreshTimer: null,
    weeklyConfirmPending: false,
    weeklyConfirmLabel: '',
    weeklyConfirmSummary: '',
    weeklyConfirmDoing: false,
    emergencyModalVisible: false,
    emergencyReason: '',
    emergencyPassword: '',
    emergencySubmitting: false,
    emergencyNeedReport: false,
    emergencyReportModalVisible: false,
    emergencyReportScope: '',
    emergencyReportImpact: '',
    emergencyReportComplete: false,
    emergencyReportSubmitting: false,
    emergencyRequestId: '',
    emergencyCertIdPath: '',
    emergencyCertLicensePath: '',
    emergencyCertIdPreview: '',
    emergencyCertLicensePreview: '',
    emergencyCertReady: false,
    emergencyEligible: true,
    voucherModalVisible: false,
    voucherStage: 'input',
    voucherInput: '',
    voucherSubmitting: false,
    voucherResult: null
  },

  onLoad() {
    const role = wx.getStorageSync('userRole');
    if (role !== 'doctor') {
      wx.redirectTo({
        url: '/pages/login/login'
      });
      return;
    }
    
    // 数据统一在 onShow 中加载，避免首次进入时 onLoad + onShow 重复请求
  },

  loadWeeklyConfirm() {
    const doctorId = wx.getStorageSync('doctorId');
    if (!doctorId) return;

    wx.request({
      url: app.globalData.baseUrl + '/api/schedule-weekly-confirm/doctor/' + doctorId + '/pending',
      method: 'GET',
      success: (res) => {
        const data = res.data && res.data.success ? res.data.data : null;
        if (!data) return;
        const days = data.days || [];
        const totalSlots = days.reduce((sum, d) => sum + (d.slotCount || 0), 0);
        const activeDays = days.filter(d => (d.slotCount || 0) > 0).length;
        this.setData({
          weeklyConfirmPending: !!data.pending,
          weeklyConfirmLabel: data.weekLabel || '',
          weeklyConfirmSummary: totalSlots > 0
            ? ('共 ' + totalSlots + ' 个时段，覆盖 ' + activeDays + ' 天')
            : '上周暂无排班，建议先设置出诊时间'
        });
      }
    });
  },

  confirmWeeklyInherit() {
    if (this.data.weeklyConfirmDoing) return;
    const doctorId = wx.getStorageSync('doctorId');
    if (!doctorId) return;

    wx.showModal({
      title: '沿用上周排班',
      content: '将按上一周排班自动生成下周号源，确定继续吗？',
      success: (res) => {
        if (!res.confirm) return;
        this.setData({ weeklyConfirmDoing: true });
        wx.request({
          url: app.globalData.baseUrl + '/api/schedule-weekly-confirm/doctor/' + doctorId + '/confirm',
          method: 'POST',
          data: { action: 'inherit' },
          success: (r) => {
            this.setData({ weeklyConfirmDoing: false });
            if (r.data && r.data.success) {
              this.setData({ weeklyConfirmPending: false });
              wx.showToast({ title: r.data.data.message || '已确认', icon: 'success' });
              this.checkMinimumSchedule();
            } else {
              wx.showToast({ title: (r.data && r.data.message) || '操作失败', icon: 'none' });
            }
          },
          fail: () => {
            this.setData({ weeklyConfirmDoing: false });
            wx.showToast({ title: '网络异常，请稍后重试', icon: 'none' });
          }
        });
      }
    });
  },

  goToScheduleEdit() {
    const doctorId = wx.getStorageSync('doctorId');
    if (doctorId) {
      wx.request({
        url: app.globalData.baseUrl + '/api/schedule-weekly-confirm/doctor/' + doctorId + '/confirm',
        method: 'POST',
        data: { action: 'modify' }
      });
    }
    this.setData({ weeklyConfirmPending: false });
    wx.navigateTo({ url: '/pages/my-schedule/my-schedule' });
  },

  formatLocalDate(date) {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  },

  checkMinimumSchedule() {
    const doctorId = wx.getStorageSync('doctorId');
    
    if (!doctorId) return;

    // 一次拉取该医生全部排班，本地按未来 7 天逐日统计，避免按日期逐个请求造成串行卡顿
    wx.request({
      url: app.globalData.baseUrl + '/api/schedules/doctor/' + doctorId,
      method: 'GET',
      success: (res) => {
        let schedules = [];
        if (Array.isArray(res.data)) {
          schedules = res.data;
        } else if (res.data && Array.isArray(res.data.data)) {
          schedules = res.data.data;
        }

        const dates = [];
        for (let i = 0; i < 7; i++) {
          const date = new Date();
          date.setDate(date.getDate() + i);
          dates.push(this.formatLocalDate(date));
        }

        const minuteCounts = {};
        dates.forEach(date => { minuteCounts[date] = 0; });
        schedules.forEach(schedule => {
          const workDate = String(schedule.workDate || '').slice(0, 10);
          if (!(workDate in minuteCounts)) return;
          minuteCounts[workDate] += this.calculateDurationMinutes(schedule.timeSlot);
        });

        const scheduledDates = dates.filter(date => minuteCounts[date] > 0);
        if (scheduledDates.length === 0) {
          this.setData({
            scheduleWarning: true,
            scheduleWarningText: '未来 7 天尚未排班，请先设置出诊时间',
            scheduleWarningDate: dates[0],
            scheduleProgress: 0,
            scheduleRequired: 120
          });
          return;
        }

        const lowDates = dates.filter(date => minuteCounts[date] > 0 && minuteCounts[date] < 120);
        if (lowDates.length > 0) {
          const firstDate = lowDates[0];
          const label = `${parseInt(firstDate.slice(5, 7))}月${parseInt(firstDate.slice(8, 10))}日`;
          this.setData({
            scheduleWarning: true,
            scheduleWarningText: `${label}仅 ${minuteCounts[firstDate]} 分钟，需至少 120 分钟（2 小时）`,
            scheduleWarningDate: firstDate,
            scheduleProgress: minuteCounts[firstDate],
            scheduleRequired: 120
          });
          return;
        }

        this.setData({ scheduleWarning: false });
        wx.setStorageSync('hasMinimumSchedule', true);
      },
      fail: () => {
        this.setData({
          scheduleWarning: true,
          scheduleWarningText: '排班数据加载失败，请稍后重试',
          scheduleWarningDate: '',
          scheduleProgress: 0,
          scheduleRequired: 120
        });
      }
    });
  },

  calculateDurationMinutes(timeSlot) {
    if (!timeSlot || timeSlot.indexOf('-') === -1) return 0;
    const parts = timeSlot.split('-');
    const startParts = String(parts[0] || '').trim().split(':');
    const endParts = String(parts[1] || '').trim().split(':');
    if (startParts.length < 2 || endParts.length < 2) return 0;
    const start = parseInt(startParts[0]) * 60 + parseInt(startParts[1]);
    const end = parseInt(endParts[0]) * 60 + parseInt(endParts[1]);
    const minutes = end - start;
    return minutes > 0 ? minutes : 0;
  },


  scanVoucher() {
    wx.scanCode({
      scanType: ['qrCode'],
      success: (res) => {
        const voucher = String(res.result || '').trim();
        if (!voucher) {
          wx.showToast({ title: '未识别到凭证内容', icon: 'none' });
          return;
        }
        this.submitVoucher(voucher);
      },
      fail: (err) => {
        const msg = String((err && err.errMsg) || '');
        if (msg.indexOf('cancel') > -1) return;
        wx.showModal({
          title: '无法使用相机',
          content: '可能未允许相机权限。可点击「手动输入」继续完成报到。',
          confirmText: '手动输入',
          cancelText: '取消',
          success: (res) => {
            if (res.confirm) this.openManualVoucher();
          }
        });
      }
    });
  },

  openManualVoucher() {
    this.setData({
      voucherModalVisible: true,
      voucherStage: 'input',
      voucherInput: '',
      voucherResult: null
    });
  },

  closeVoucherModal() {
    this.setData({
      voucherModalVisible: false,
      voucherStage: 'input',
      voucherInput: '',
      voucherResult: null
    });
  },

  onVoucherInput(e) {
    this.setData({ voucherInput: e.detail.value });
  },

  submitManualVoucher() {
    const voucher = String(this.data.voucherInput || '').trim();
    if (!voucher) {
      wx.showToast({ title: '请输入就诊凭证', icon: 'none' });
      return;
    }
    this.submitVoucher(voucher);
  },

  voucherCodeText(code) {
    const map = {
      VOUCHER_INVALID_SIGNATURE: '凭证无效：签名校验失败，请让患者重新获取',
      VOUCHER_EXPIRED: '凭证已过期，请让患者重新获取',
      VOUCHER_USED: '该凭证已用于报到，不能重复使用',
      VOUCHER_WRONG_HOSPITAL: '该凭证不属于本院，无法报到',
      VOUCHER_STATUS_INVALID: '该预约当前状态不可报到'
    };
    return map[code] || '';
  },

  submitVoucher(voucher) {
    if (this.data.voucherSubmitting) return;
    const token = wx.getStorageSync('authToken');
    this.setData({
      voucherSubmitting: true,
      voucherModalVisible: true,
      voucherStage: 'result',
      voucherResult: null
    });

    wx.request({
      url: app.globalData.baseUrl + '/api/appointments/voucher/checkin',
      method: 'POST',
      header: token ? { 'X-Auth-Token': token } : {},
      data: { voucher: voucher },
      success: (res) => {
        const body = res.data || {};
        const success = body.success !== false && res.statusCode >= 200 && res.statusCode < 300;
        if (success) {
          this.setData({
            voucherSubmitting: false,
            voucherResult: {
              success: true,
              queueNo: body.queueNo || '',
              checkinTime: body.checkinTime || '',
              message: body.message || '报到成功'
            }
          });
          this.loadDoctorData();
          return;
        }
        const code = body.code || '';
        this.setData({
          voucherSubmitting: false,
          voucherResult: {
            success: false,
            code: code,
            message: this.voucherCodeText(code) || body.message || body.error || '报到失败，请核对凭证'
          }
        });
      },
      fail: () => {
        this.setData({
          voucherSubmitting: false,
          voucherResult: {
            success: false,
            message: '网络异常，请稍后重试'
          }
        });
      }
    });
  },

  onShow() {
    this.loadDoctorData();
    this.checkMinimumSchedule();
    this.loadWeeklyConfirm();
    this.loadEmergencyReminder();
    this.startRefreshTimer();
  },

  onHide() {
    this.stopRefreshTimer();
  },

  onUnload() {
    this.stopRefreshTimer();
  },

  // 启动定时刷新（每30秒刷新一次）
  startRefreshTimer() {
    this.stopRefreshTimer();
    this.data.refreshTimer = setInterval(() => {
      this.loadDoctorData();
    }, 30000);
  },

  // 停止定时刷新
  stopRefreshTimer() {
    if (this.data.refreshTimer) {
      clearInterval(this.data.refreshTimer);
      this.data.refreshTimer = null;
    }
  },

  loadDoctorData() {
    const doctorUsername = wx.getStorageSync('doctorUsername');
    const doctorName = wx.getStorageSync('doctorName') || '王建安';
    
    if (!doctorUsername) {
      wx.showToast({ title: '请先登录医生账号', icon: 'none' });
      setTimeout(() => {
        wx.redirectTo({ url: '/pages/login/login' });
      }, 1500);
      return;
    }
    
    this.setData({ doctorName });
    
    
    // 仅首次进入显示加载遮罩，30 秒定时刷新静默更新，避免反复闪遮罩
    const showLoading = !this._doctorDataLoaded;
    if (showLoading) {
      wx.showLoading({ title: '加载中...' });
    }
    
    wx.request({
      url: app.globalData.baseUrl + '/api/appointments/doctor/by-username/' + doctorUsername,
      method: 'GET',
      timeout: 15000,
      success: (res) => {
        this._doctorDataLoaded = true;
        if (showLoading) {
          wx.hideLoading();
        }
        
        
        if (res.statusCode === 200) {
          if (!res.data || !Array.isArray(res.data)) {
            wx.showToast({
              title: '数据格式错误',
              icon: 'none'
            });
            return;
          }
          
          const appointments = res.data.map(appt => {
            appt.hasQuestionnaire = !!appt.questionnaireData;
            appt.patientName = appt.patient ? appt.patient.name : '未知患者';
            
            if (appt.questionnaireData) {
              const parsedQuestionnaire = questionnaireConfig.parse(
                appt.questionnaireData,
                appt.deptId,
                appt.deptName || (appt.doctor && appt.doctor.deptName)
              );
              if (parsedQuestionnaire) {
                appt.parsedQuestionnaire = parsedQuestionnaire;
              } else {
                appt.hasQuestionnaire = false;
              }
            }
            return appt;
          });
          
          const doctorInfo = appointments.length ? appointments[0].doctor : null;
          if (doctorInfo) {
            const nextDept = doctorInfo.deptName || this.data.doctorDept;
            this.setData({
              doctorTitle: doctorInfo.title || this.data.doctorTitle,
              doctorDept: nextDept,
              doctorDeptIcon: deptIcon.iconPath(nextDept)
            });
          }

          const today = new Date().toISOString().split('T')[0];
          
          // 按状态和时间排序：待就诊（status=1）按日期升序排前面，其他状态放后面
          appointments.sort((a, b) => {
            // 待就诊的排前面
            if (a.status === 1 && b.status !== 1) return -1;
            if (a.status !== 1 && b.status === 1) return 1;
            
            // 同为待就诊，按日期和时间排序
            if (a.status === 1 && b.status === 1) {
              if (a.appointmentDate !== b.appointmentDate) {
                return a.appointmentDate.localeCompare(b.appointmentDate);
              }
              return a.timeSlot.localeCompare(b.timeSlot);
            }
            
            // 其他状态按日期降序（最新的在前）
            return b.appointmentDate.localeCompare(a.appointmentDate);
          });
          
          const todayCount = appointments.filter(a => a.appointmentDate === today && a.status === 1).length;
          const pendingCount = appointments.filter(a => a.status === 1).length;
          const completedCount = appointments.filter(a => a.status === 2).length;
          const inProgressCount = appointments.filter(a => a.status === 3).length;
          const questionnaireCount = appointments.filter(a => a.hasQuestionnaire).length;
          
          this.setData({
            todayCount: todayCount,
            inProgressCount: inProgressCount,
            completedCount: completedCount,
            pendingCount: pendingCount,
            questionnaireCount: questionnaireCount,
            appointments: appointments
          });
        } else {
          wx.showToast({
            title: res.data?.message || '加载数据失败',
            icon: 'none'
          });
        }
      },
      fail: (err) => {
        if (showLoading) {
          wx.hideLoading();
        }
        console.error('[医生工作台] 请求失败:', err);
        wx.showToast({
          title: '网络错误，请检查服务是否启动',
          icon: 'none',
          duration: 3000
        });
      }
    });
  },


  toggleQuestionnaire(e) {
    const index = e.currentTarget.dataset.index;
    this.setData({
      expandedIndex: this.data.expandedIndex === index ? null : index
    });
  },

  handleLogout() {
    wx.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          wx.removeStorageSync('token');
          wx.removeStorageSync('authToken');
          wx.removeStorageSync('openid');
          wx.removeStorageSync('userRole');
          wx.removeStorageSync('doctorId');
          wx.removeStorageSync('doctorName');
          app.globalData.token = null;
          app.globalData.openid = null;
          app.globalData.userRole = null;
          app.globalData.doctorId = null;
          app.globalData.doctorName = null;
          
          wx.redirectTo({
            url: '/pages/login/login'
          });
        }
      }
    });
  },

  goToMyPatients() {
    wx.navigateTo({
      url: '/pages/patient-list/patient-list'
    });
  },

  goToMinimumSchedule() {
    const date = this.data.scheduleWarningDate || '';
    wx.navigateTo({
      url: '/pages/doctor-minimum-schedule/doctor-minimum-schedule' + (date ? '?date=' + date : '')
    });
  },

  goToMySchedule() {
    wx.navigateTo({
      url: '/pages/my-schedule/my-schedule'
    });
  },

  goToQuestionnaires() {
    wx.navigateTo({
      url: '/pages/questionnaire-list/questionnaire-list'
    });
  },

  loadEmergencyReminder() {
    const token = app.globalData.token || wx.getStorageSync('token') || wx.getStorageSync('authToken');
    if (!token) return;
    wx.request({
      url: app.globalData.baseUrl + '/api/emergency/request/my',
      method: 'GET',
      success: (res) => {
        const data = res.data || {};
        if (!data.success) return;
        this.setData({
          emergencyNeedReport: !!data.reportRequired,
          emergencyRequestId: data.requestId || '',
          emergencyEligible: data.eligible !== false
        });
      }
    });
  },

  applyEmergencyAccess() {
    const token = app.globalData.token || wx.getStorageSync('token') || wx.getStorageSync('authToken');
    if (!token) {
      wx.showModal({
        title: '登录状态失效',
        content: '请重新登录后再使用应急通道。',
        showCancel: false
      });
      return;
    }

    wx.showLoading({ title: '查询中...' });
    wx.request({
      url: app.globalData.baseUrl + '/api/emergency/request/my',
      method: 'GET',
      success: (res) => {
        wx.hideLoading();
        const data = res.data || {};
        if (!data.success) {
          wx.showModal({
            title: '查询失败',
            content: data.message || '请稍后重试。',
            showCancel: false
          });
          return;
        }
        if (data.eligible === false) {
          wx.showModal({
            title: '仅限主任医师',
            content: data.eligibleMessage || '应急通道仅限主任医师及以上职称的注册医生使用。',
            showCancel: false
          });
          return;
        }

        if (data.status === 0 && data.token) {
          this.enterEmergencyDepartment(data);
          return;
        }
        if (data.needReport) {
          this.openEmergencyReport(data);
          return;
        }
        if (data.status === 2) {
          wx.showModal({
            title: '等待卫健委确认',
            content: '结束报告已提交，卫健委确认后将自动归档。',
            showCancel: false
          });
          return;
        }
        if (data.status === 3) {
          wx.showModal({
            title: '已确认归档',
            content: '本次应急事项已完成确认归档。',
            showCancel: false
          });
          return;
        }
        if (data.status === 4) {
          wx.showModal({
            title: '已被驳回',
            content: data.endReason || '本次应急事项被判定为异常，请联系卫健委。',
            showCancel: false
          });
          return;
        }
        if (data.status === 7) {
          wx.showModal({
            title: '确认已逾期',
            content: '卫健委未在规定时间内确认，本院后续自助申请已暂停，请联系卫健委处理。',
            showCancel: false
          });
          return;
        }
        this.openEmergencyForm(!!data.certReady);
      },
      fail: () => {
        wx.hideLoading();
        wx.showModal({
          title: '网络超时',
          content: '查询应急状态失败，请检查网络后重试。',
          confirmText: '重试',
          cancelText: '取消',
          success: (modalRes) => {
            if (modalRes.confirm) {
              this.applyEmergencyAccess();
            }
          }
        });
      }
    });
  },

  openEmergencyForm(certReady) {
    this.setData({
      emergencyModalVisible: true,
      emergencyReason: '',
      emergencyPassword: '',
      emergencyCertReady: !!certReady,
      emergencyCertIdPath: '',
      emergencyCertLicensePath: '',
      emergencyCertIdPreview: '',
      emergencyCertLicensePreview: ''
    });
  },

  uploadEmergencyCert(e) {
    const type = e.currentTarget.dataset.type;
    if (type !== 'idCard' && type !== 'license') return;

    const token = app.globalData.token || wx.getStorageSync('token') || wx.getStorageSync('authToken');
    if (!token) {
      wx.showModal({ title: '登录状态失效', content: '请重新登录后再上传证件。', showCancel: false });
      return;
    }

    wx.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (chooseRes) => {
        const filePath = chooseRes.tempFilePaths[0];
        wx.showLoading({ title: '上传中...' });
        wx.uploadFile({
          url: app.globalData.baseUrl + '/api/emergency/request/cert',
          filePath: filePath,
          name: 'file',
          header: { 'X-Auth-Token': token },
          formData: { type: type },
          success: (uploadRes) => {
            wx.hideLoading();
            let data = {};
            try { data = JSON.parse(uploadRes.data || '{}'); } catch (err) { data = {}; }
            if (data.success && data.path) {
              if (type === 'idCard') {
                this.setData({ emergencyCertIdPath: data.path, emergencyCertIdPreview: filePath });
              } else {
                this.setData({ emergencyCertLicensePath: data.path, emergencyCertLicensePreview: filePath });
              }
              wx.showToast({ title: '已上传', icon: 'success' });
            } else {
              wx.showModal({
                title: '上传失败',
                content: data.message || '请稍后重试。',
                showCancel: false
              });
            }
          },
          fail: () => {
            wx.hideLoading();
            wx.showModal({ title: '网络超时', content: '证件上传失败，请检查网络后重试。', showCancel: false });
          }
        });
      },
      fail: (err) => {
        const msg = String((err && err.errMsg) || '');
        if (msg.indexOf('cancel') > -1) return;
        wx.showModal({
          title: '无法访问相册或相机',
          content: '请点击「去设置」开启相册/相机权限后重试，或重新上传证件。',
          confirmText: '去设置',
          success: (res) => {
            if (res.confirm) wx.openSetting({});
          }
        });
      }
    });
  },

  openEmergencyReport(data) {
    const requestId = (data && data.requestId) || this.data.emergencyRequestId;
    if (!requestId) {
      wx.showToast({ title: '暂无可提交的报告', icon: 'none' });
      return;
    }
    this.setData({
      emergencyReportModalVisible: true,
      emergencyRequestId: requestId,
      emergencyReportScope: '',
      emergencyReportImpact: '',
      emergencyReportComplete: false
    });
    wx.showLoading({ title: '生成改动范围...' });
    wx.request({
      url: app.globalData.baseUrl + '/api/emergency/request/report-draft?requestId=' + encodeURIComponent(requestId),
      method: 'GET',
      success: (res) => {
        wx.hideLoading();
        const draft = res.data || {};
        if (draft.success) {
          this.setData({ emergencyReportScope: draft.autoScope || '' });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '改动范围生成失败，可手动填写', icon: 'none' });
      }
    });
  },

  openEmergencyReportFromBanner() {
    this.openEmergencyReport({ requestId: this.data.emergencyRequestId });
  },

  closeEmergencyReport() {
    if (this.data.emergencyReportSubmitting) return;
    this.setData({ emergencyReportModalVisible: false });
  },

  onReportScopeInput(e) {
    this.setData({ emergencyReportScope: e.detail.value });
  },

  onReportImpactInput(e) {
    this.setData({ emergencyReportImpact: e.detail.value });
  },

  onReportCompleteChange(e) {
    const values = e.detail.value || [];
    this.setData({ emergencyReportComplete: values.length > 0 });
  },

  submitEmergencyReport() {
    if (this.data.emergencyReportSubmitting) return;
    const impact = (this.data.emergencyReportImpact || '').trim();
    if (impact.length < 4) {
      wx.showModal({ title: '请填写影响预估', content: '影响预估至少 4 个字。', showCancel: false });
      return;
    }
    if (!this.data.emergencyReportComplete) {
      wx.showModal({ title: '请确认无未完成事项', content: '请勾选“确认已无未完成事项”后再提交。', showCancel: false });
      return;
    }

    this.setData({ emergencyReportSubmitting: true });
    wx.showLoading({ title: '提交中...' });
    wx.request({
      url: app.globalData.baseUrl + '/api/emergency/request/report',
      method: 'POST',
      header: { 'content-type': 'application/json' },
      data: {
        requestId: this.data.emergencyRequestId,
        scope: this.data.emergencyReportScope,
        impact: impact,
        complete: true
      },
      success: (res) => {
        wx.hideLoading();
        this.setData({ emergencyReportSubmitting: false });
        if (res.data && res.data.success) {
          this.setData({ emergencyReportModalVisible: false, emergencyNeedReport: false });
          wx.showModal({
            title: '报告已提交',
            content: '结束报告已提交，等待卫健委确认归档。',
            showCancel: false
          });
          return;
        }
        wx.showModal({
          title: '提交未成功',
          content: (res.data && res.data.message) || '请稍后重试。',
          showCancel: false
        });
      },
      fail: () => {
        wx.hideLoading();
        this.setData({ emergencyReportSubmitting: false });
        wx.showModal({ title: '网络超时', content: '报告提交失败，请检查网络后重试。', showCancel: false });
      }
    });
  },

  enterEmergencyDepartment(data) {
    wx.setStorageSync('emergencyToken', data.token);
    wx.setStorageSync('emergencyEntry', 'doctor');
    wx.setStorageSync('emergencyHospitalId', data.hospitalId);
    wx.setStorageSync('emergencyHospitalName', data.hospitalName || '');
    wx.setStorageSync('expireTime', data.expireTime);
    this.setData({
      emergencyModalVisible: false,
      emergencyPassword: '',
      emergencyNeedReport: false,
      emergencyCertIdPath: '',
      emergencyCertLicensePath: '',
      emergencyCertIdPreview: '',
      emergencyCertLicensePreview: ''
    });
    wx.showToast({ title: '已开通 2 小时权限', icon: 'success' });
    setTimeout(() => {
      wx.navigateTo({
        url: '/subpackages/admin/pages/emergency-department/emergency-department'
      });
    }, 1200);
  },

  closeEmergencyModal() {
    if (this.data.emergencySubmitting) return;
    this.setData({ emergencyModalVisible: false, emergencyPassword: '' });
  },

  onEmergencyReasonInput(e) {
    this.setData({ emergencyReason: e.detail.value });
  },

  onEmergencyPasswordInput(e) {
    this.setData({ emergencyPassword: e.detail.value });
  },

  noop() {
    // 阻止弹层滚动穿透
  },

  submitEmergencyRequest() {
    const reason = (this.data.emergencyReason || '').trim();
    const password = this.data.emergencyPassword || '';

    if (reason.length < 4) {
      wx.showModal({
        title: '请填写申请原因',
        content: '申请原因至少 4 个字，用于事后审计。',
        showCancel: false
      });
      return;
    }
    if (!password) {
      wx.showModal({
        title: '请输入密码',
        content: '为确认是本人操作，请重新输入医生账号密码。',
        showCancel: false
      });
      return;
    }
    if (!this.data.emergencyCertReady
        && (!this.data.emergencyCertIdPath || !this.data.emergencyCertLicensePath)) {
      wx.showModal({
        title: '请上传证件',
        content: '首次使用须上传身份证人像面和医师资格证。',
        showCancel: false
      });
      return;
    }

    this.requestEmergencyApproval(reason, password);
  },

  requestEmergencyApproval(reason, password) {
    if (this.data.emergencySubmitting) {
      return;
    }

    const token = app.globalData.token || wx.getStorageSync('token') || wx.getStorageSync('authToken');
    if (!token) {
      wx.showModal({
        title: '登录状态失效',
        content: '请重新登录后再申请应急权限。',
        showCancel: false
      });
      return;
    }

    this.setData({ emergencySubmitting: true });
    wx.showLoading({ title: '提交中...' });
    wx.request({
      url: app.globalData.baseUrl + '/api/emergency/request/submit',
      method: 'POST',
      header: {
        'content-type': 'application/json',
        'Authorization': 'Bearer ' + token
      },
      data: {
        reason: reason,
        password: password,
        certIdPath: this.data.emergencyCertIdPath,
        certLicensePath: this.data.emergencyCertLicensePath
      },
      success: (res) => {
        wx.hideLoading();
        this.setData({ emergencySubmitting: false });
        if (res.data && res.data.success) {
          this.enterEmergencyDepartment(res.data);
          return;
        }
        wx.showModal({
          title: '提交未成功',
          content: (res.data && res.data.message) || '请稍后重试。',
          showCancel: false
        });
      },
      fail: () => {
        wx.hideLoading();
        this.setData({ emergencySubmitting: false });
        wx.showModal({
          title: '网络超时',
          content: '申请未提交成功，请检查网络后重试。',
          confirmText: '重试',
          cancelText: '取消',
          success: (modalRes) => {
            if (modalRes.confirm) {
              this.requestEmergencyApproval(reason, password);
            }
          }
        });
      }
    });
  },

  goToProfile() {
    wx.navigateTo({ url: '/pages/doctor-profile/doctor-profile' });
  },

  goToPatientDetail(e) {
    const appointmentId = e.currentTarget.dataset.appointmentId;
    const deptId = e.currentTarget.dataset.deptId;
    
    wx.navigateTo({
      url: `/pages/patient-detail/patient-detail?appointmentId=${appointmentId}&deptId=${deptId || ''}`
    });
  },

  getStatusText(status) {
    if (status === 1) return '待就诊';
    if (status === 2) return '已完成';
    if (status === 3) return '进行中';
    if (status === 0) return '已取消';
    return '未知';
  },

  getStatusClass(status) {
    if (status === 1) return 'status-pending';
    if (status === 2) return 'status-completed';
    if (status === 3) return 'status-inprogress';
    return 'status-cancelled';
  }
})
