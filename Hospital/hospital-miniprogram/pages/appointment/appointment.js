const app = getApp();
const elderly = require('../../utils/elderly');

const questionnaireConfig = require('../../utils/questionnaire-config');

Page({
  data: {
    step: 1,
    departments: [],
    doctors: [],
    selectedDept: null,
    selectedDoctor: null,
    selectedDate: '',
    selectedTime: '',
    today: '',
    hospitalId: '',
    hospitalName: '',
    timeSlots: [],
    disabledTimeSlots: [],
    normalTimeSlots: ['08:00-09:00', '09:00-10:00', '10:00-11:00', '11:00-12:00', '14:00-15:00', '15:00-16:00', '16:00-17:00', '17:00-18:00'],
    currentQuestionnaire: null,
    questionnaireAnswers: {},
    skipQuestionnaire: false,
    questionPages: [],
    questionPageIndex: 0,
    questionPageCount: 0,
    pagedQuestions: [],
    currentHospital: null,
    paymentInfo: {},
    selectedPayment: 'wechat',
    currentAppointment: null,
    elderly: false
  },

  // 统一用本地时间格式化日期，避免 toISOString 走 UTC 导致凌晨 0-8 点日期少一天
  formatLocalDate(date) {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  },

  onLoad(options) {
    this.setData({ elderly: elderly.isElderly() });
    this.setData({ today: this.formatLocalDate(new Date()) });

    if (options.doctorId) {
      this.loadDoctorAndDept(options.doctorId);
    } else if (options.hospitalId) {
      this.setData({ hospitalId: options.hospitalId });
      this.loadHospital(options.hospitalId);
    } else {
      this.loadHospital('H001');
    }
  },

  loadHospital(hospitalId) {
    wx.request({
      url: app.globalData.baseUrl + '/api/hospitals/' + hospitalId,
      method: 'GET',
      success: (res) => {
        if (res.data) {
          this.setData({
            currentHospital: res.data,
            hospitalName: res.data.hospitalName,
            hospitalId: hospitalId
          });
          this.loadDepartments(hospitalId);
        }
      },
      fail: () => {
        console.error('获取医院信息失败');
      }
    });
  },

  loadDepartments(hospitalId) {
    wx.request({
      url: app.globalData.baseUrl + '/api/departments/hospital/' + hospitalId + '?bookable=true',
      method: 'GET',
      success: (res) => {
        if (res.data && Array.isArray(res.data)) {
          this.setData({ departments: res.data });
        }
      },
      fail: () => {
        this.setData({ departments: [] });
      }
    });
  },

  loadDoctorAndDept(doctorId) {
    wx.showLoading({ title: '加载中...' });
    
    wx.request({
      url: app.globalData.baseUrl + '/api/doctors/' + doctorId,
      method: 'GET',
      success: (res) => {
        if (res.data) {
          const doctor = res.data;
          this.setData({
            selectedDoctor: doctor,
            hospitalId: doctor.hospitalId,
            step: 2
          });
          
          this.loadHospital(doctor.hospitalId);
          this.loadDoctorsByDept(doctor.deptId);
          // 科室名称加载完成后再按名称匹配问卷，避免不同医院科室编号不一致
          this.loadDepartment(doctor.deptId, true);
          
          // 立即检查该医生的排班情况
          this.checkDoctorSchedule(doctor);
        } else {
          wx.hideLoading();
        }
      },
      fail: () => {
        wx.hideLoading();
      }
    });
  },
  
  checkDoctorSchedule(doctor) {
    const today = this.formatLocalDate(new Date());
    
    let completedCount = 0;
    const totalRequests = 2;
    
    const checkComplete = () => {
      completedCount++;
      if (completedCount === totalRequests) {
        wx.hideLoading();
      }
    };
    
    // 检查今日排班
    wx.request({
      url: app.globalData.baseUrl + '/api/schedules/doctor/' + doctor.doctorId + '/date/' + today,
      method: 'GET',
      success: (res) => {
        let hasSchedule = false;
        if (res.data && res.data.success && Array.isArray(res.data.data)) {
          hasSchedule = res.data.data.length > 0;
        } else if (res.data && Array.isArray(res.data)) {
          hasSchedule = res.data.length > 0;
        }
        
        this.setData({ 
          doctorHasSchedule: hasSchedule,
          ['scheduleMap.' + doctor.doctorId]: hasSchedule,
          selectedDate: hasSchedule ? today : ''
        });
        
        if (hasSchedule) {
          this.loadTimeSlotsForDate(today);
        }
        
        checkComplete();
      },
      fail: () => {
        this.setData({ doctorHasSchedule: false });
        checkComplete();
      }
    });
    
    // 获取有排班的日期
    wx.request({
      url: app.globalData.baseUrl + '/api/schedules/doctor/' + doctor.doctorId + '/dates',
      method: 'GET',
      success: (res) => {
        let scheduleDates = [];
        if (res.data && res.data.success && Array.isArray(res.data.data)) {
          scheduleDates = res.data.data;
        }
        this.setData({ scheduleDates });
        checkComplete();
      },
      fail: checkComplete
    });
  },

  loadDepartment(deptId, useForQuestionnaire) {
    wx.request({
      url: app.globalData.baseUrl + '/api/departments/' + deptId,
      method: 'GET',
      success: (res) => {
        if (res.data) {
          this.setData({ selectedDept: res.data });
          if (useForQuestionnaire) {
            this.loadQuestionnaire(res.data.deptId, res.data.deptName);
          }
        }
      }
    });
  },

  loadDoctorsByDept(deptId) {
    if (!this.data.hospitalId) return;
    
    wx.request({
      url: app.globalData.baseUrl + '/api/doctors/dept/' + deptId + '/hospital/' + this.data.hospitalId,
      method: 'GET',
      success: (res) => {
        if (res.data && Array.isArray(res.data)) {
          this.setData({ doctors: res.data });
          this.preloadAllDoctorsSchedule(res.data);
        }
      },
      fail: () => {
        this.setData({ doctors: [], scheduleMap: {} });
      }
    });
  },

  preloadAllDoctorsSchedule(doctors) {
    if (!doctors || doctors.length === 0) {
      this.setData({ scheduleMap: {} });
      return;
    }

    const today = this.formatLocalDate(new Date());
    const scheduleMap = {};
    doctors.forEach(doctor => {
      scheduleMap[doctor.doctorId] = false;
    });

    const hospitalId = this.data.hospitalId;
    if (!hospitalId) {
      this.setData({ scheduleMap });
      return;
    }

    // 一次拉取全院当日排班，替代逐个医生请求
    wx.request({
      url: app.globalData.baseUrl + '/api/schedules/hospital/' + hospitalId + '/date/' + today,
      method: 'GET',
      success: (res) => {
        const list = res.data && res.data.success
          ? (res.data.data || [])
          : (Array.isArray(res.data) ? res.data : []);
        list.forEach(item => {
          if (item && item.doctorId && item.doctorId in scheduleMap) {
            scheduleMap[item.doctorId] = true;
          }
        });
        this.setData({ scheduleMap });
      },
      fail: () => {
        this.setData({ scheduleMap });
      }
    });
  },

  loadQuestionnaire(deptId, deptName) {
    const questionnaire = questionnaireConfig.getConfig(deptId, deptName);
    const questionPages = this.buildQuestionPages(questionnaire.questions || []);
    this.setData({
      currentQuestionnaire: questionnaire,
      questionnaireAnswers: {},
      skipQuestionnaire: false,
      questionPages,
      questionPageIndex: 0,
      questionPageCount: questionPages.length,
      pagedQuestions: questionPages[0] || []
    });
  },

  // 按题目拆分成单题一页，确保问卷卡片不超出手机屏幕高度
  buildQuestionPages(questions) {
    const pages = questions.map(question => [question]);
    return pages.length > 0 ? pages : [[]];
  },

  prevQuestionPage() {
    const index = this.data.questionPageIndex;
    if (index <= 0) return;
    const nextIndex = index - 1;
    this.setData({
      questionPageIndex: nextIndex,
      pagedQuestions: this.data.questionPages[nextIndex] || []
    });
  },

  nextQuestionPage() {
    const index = this.data.questionPageIndex;
    if (index < this.data.questionPageCount - 1) {
      const nextIndex = index + 1;
      this.setData({
        questionPageIndex: nextIndex,
        pagedQuestions: this.data.questionPages[nextIndex] || []
      });
    } else {
      this.setData({ step: 4 });
    }
  },

  // 点击进度条中“问卷”之后的步骤，可直接跳过问卷进入时间选择
  onProgressStepTap(e) {
    const targetStep = Number(e.currentTarget.dataset.step);
    if (this.data.step !== 3 || !targetStep || targetStep <= 3) return;
    this.setData({ skipQuestionnaire: true, step: 4 });
    wx.showToast({ title: '已跳过问卷', icon: 'none' });
  },

  selectDept(e) {
    const dept = e.currentTarget.dataset.dept;
    this.setData({ selectedDept: dept, selectedDoctor: null, timeSlots: this.data.normalTimeSlots });
    this.loadDoctorsByDept(dept.deptId);
    this.loadQuestionnaire(dept.deptId, dept.deptName);
  },

  selectDoctor(e) {
    const doctor = e.currentTarget.dataset.doctor;
    const today = this.formatLocalDate(new Date());
    this.setData({ 
      selectedDoctor: doctor, 
      timeSlots: [], 
      disabledTimeSlots: [], 
      selectedDate: today, 
      selectedTime: '', 
      doctorHasSchedule: null 
    });
    
    wx.showLoading({ title: '查询排班中...' });
    
    let completedCount = 0;
    const totalRequests = 2;
    
    const checkComplete = () => {
      completedCount++;
      if (completedCount === totalRequests) {
        wx.hideLoading();
      }
    };
    
    // 请求今日排班
    wx.request({
      url: app.globalData.baseUrl + '/api/schedules/doctor/' + doctor.doctorId + '/date/' + today,
      method: 'GET',
      success: (res) => {
        console.log('[选择医生] 今日排班响应:', res);
        let hasSchedule = false;
        
        if (res.data && res.data.success && Array.isArray(res.data.data)) {
          hasSchedule = res.data.data.length > 0;
        } else if (res.data && Array.isArray(res.data)) {
          hasSchedule = res.data.length > 0;
        }
        
        this.setData({ 
          doctorHasSchedule: hasSchedule,
          ['scheduleMap.' + doctor.doctorId]: hasSchedule
        });
        
        console.log('[选择医生] 医生', doctor.doctorId, '今日排班:', hasSchedule);
        
        if (hasSchedule) {
          this.loadTimeSlotsForDate(today);
        }
        
        checkComplete();
      },
      fail: (err) => {
        console.error('[选择医生] 今日排班请求失败:', err);
        this.setData({ doctorHasSchedule: false, ['scheduleMap.' + doctor.doctorId]: false });
        checkComplete();
      }
    });
    
    // 请求有排班的所有日期
    wx.request({
      url: app.globalData.baseUrl + '/api/schedules/doctor/' + doctor.doctorId + '/dates',
      method: 'GET',
      success: (res) => {
        console.log('[选择医生] 排班日期响应:', res);
        let scheduleDates = [];
        
        if (res.data && res.data.success && Array.isArray(res.data.data)) {
          scheduleDates = res.data.data;
        }
        
        this.setData({ scheduleDates });
        console.log('[选择医生] 有排班的日期:', scheduleDates);
        
        checkComplete();
      },
      fail: (err) => {
        console.error('[选择医生] 排班日期请求失败:', err);
        checkComplete();
      }
    });
  },

  checkTodaySchedule(doctorId) {
    const today = this.formatLocalDate(new Date());
    
    wx.request({
      url: app.globalData.baseUrl + '/api/schedules/doctor/' + doctorId + '/date/' + today,
      method: 'GET',
      success: (res) => {
        console.log('[排班检查] 响应:', res);
        let hasSchedule = false;
        
        if (res.data && res.data.success && Array.isArray(res.data.data)) {
          hasSchedule = res.data.data.length > 0;
        } else if (res.data && Array.isArray(res.data)) {
          hasSchedule = res.data.length > 0;
        }
        
        console.log('[排班检查] 医生', doctorId, '今日排班:', hasSchedule);
        
        this.setData({
          doctorHasSchedule: hasSchedule,
          ['scheduleMap.' + doctorId]: hasSchedule
        });
      },
      fail: (err) => {
        console.error('[排班检查] 请求失败:', err);
        this.setData({
          doctorHasSchedule: false,
          ['scheduleMap.' + doctorId]: false
        });
      }
    });
  },

  loadDoctorSchedule(doctorId) {
    if (!doctorId) return;
    
    wx.showLoading({ title: '加载排班中...' });
    
    const today = this.formatLocalDate(new Date());
    
    wx.request({
      url: app.globalData.baseUrl + '/api/schedules/doctor/' + doctorId + '/date/' + today,
      method: 'GET',
      success: (res) => {
        wx.hideLoading();
        
        let hasSchedule = false;
        let totalSlots = 0;
        
        if (res.data && res.data.success && Array.isArray(res.data.data)) {
          totalSlots = res.data.data.length;
          hasSchedule = totalSlots > 0;
        } else if (res.data && Array.isArray(res.data)) {
          totalSlots = res.data.length;
          hasSchedule = totalSlots > 0;
        }
        
        this.setData({ 
          doctorHasSchedule: hasSchedule,
          scheduleDates: hasSchedule ? [today] : []
        });
        
        if (hasSchedule) {
          this.loadTimeSlotsForDate(today);
        } else {
          wx.showToast({ 
            title: '该医生暂未排班', 
            icon: 'none',
            duration: 2000
          });
        }
      },
      fail: () => {
        wx.hideLoading();
        this.setData({ doctorHasSchedule: false, scheduleDates: [] });
        wx.showToast({ 
          title: '该医生暂未排班', 
          icon: 'none',
          duration: 2000
        });
      }
    });
  },

  onDateChange(e) {
    const selectedDate = e.detail.value;
    console.log('[日期选择] 选择日期:', selectedDate);
    console.log('[日期选择] scheduleDates:', this.data.scheduleDates);
    
    this.setData({ selectedDate, selectedTime: '', timeSlots: [], disabledTimeSlots: [] });
    this.loadTimeSlotsForDate(selectedDate);
  },

  splitTimeSlot(timeSlot) {
    const slots = [];
    const [startStr, endStr] = timeSlot.split('-');
    
    if (!startStr || !endStr) {
      slots.push(timeSlot);
      return slots;
    }
    
    const [startHour, startMin] = startStr.split(':').map(Number);
    const [endHour, endMin] = endStr.split(':').map(Number);
    
    let currentHour = startHour;
    let currentMin = startMin;
    
    while (true) {
      const nextMin = currentMin + 20;
      let nextHour = currentHour;
      let nextM = nextMin;
      
      if (nextMin >= 60) {
        nextHour = currentHour + 1;
        nextM = nextMin - 60;
      }
      
      if (nextHour > endHour || (nextHour === endHour && nextM > endMin)) {
        if (currentHour < endHour || (currentHour === endHour && currentMin < endMin)) {
          const slot = `${String(currentHour).padStart(2, '0')}:${String(currentMin).padStart(2, '0')}-${String(endHour).padStart(2, '0')}:${String(endMin).padStart(2, '0')}`;
          slots.push(slot);
        }
        break;
      }
      
      const slot = `${String(currentHour).padStart(2, '0')}:${String(currentMin).padStart(2, '0')}-${String(nextHour).padStart(2, '0')}:${String(nextM).padStart(2, '0')}`;
      slots.push(slot);
      
      currentHour = nextHour;
      currentMin = nextM;
    }
    
    return slots;
  },

  // 号源可预约：状态正常且该段未约满（每段默认接待 2 人）
  isSlotBookable(schedule) {
    if (!schedule) return false;
    if (schedule.status !== 'available' && schedule.status !== 1) return false;
    const max = Number(schedule.maxPatients || 0);
    const booked = Number(schedule.bookedCount || 0);
    return max <= 0 || booked < max;
  },

  loadTimeSlotsForDate(selectedDate) {
    if (!this.data.selectedDoctor || !selectedDate) {
      console.log('[加载时段] 缺少医生或日期');
      return;
    }
    
    console.log('[加载时段] 医生:', this.data.selectedDoctor.doctorId);
    console.log('[加载时段] 日期:', selectedDate);
    
    const todayStr = this.formatLocalDate(new Date());
    
    wx.request({
      url: app.globalData.baseUrl + '/api/schedules/doctor/' + this.data.selectedDoctor.doctorId + '/date/' + selectedDate,
      method: 'GET',
      success: (res) => {
        console.log('[加载时段] 响应:', res);
        
        let timeSlots = [];
        const disabledTimeSlots = [];
        
        let rawSlots = [];
        if (res.data && res.data.success && Array.isArray(res.data.data)) {
          rawSlots = res.data.data.filter(s => this.isSlotBookable(s));
        } else if (res.data && Array.isArray(res.data)) {
          rawSlots = res.data.filter(s => this.isSlotBookable(s));
        }
        
        console.log('[加载时段] 原始时段:', rawSlots);
        
        timeSlots = rawSlots.map(schedule => schedule.timeSlot).sort();
        
        console.log('[加载时段] 处理后时段:', timeSlots);
        
        if (selectedDate === todayStr) {
          const now = new Date();
          const currentHour = now.getHours();
          const currentMinute = now.getMinutes();
          timeSlots.forEach(time => {
            const hour = parseInt(time.split('-')[0].split(':')[0]);
            const minute = parseInt(time.split('-')[0].split(':')[1]) || 0;
            if (hour < currentHour || (hour === currentHour && currentMinute > minute)) {
              disabledTimeSlots.push(time);
            }
          });
        }
        
        this.setData({ timeSlots, disabledTimeSlots });
        
        if (timeSlots.length === 0) {
          wx.showModal({
            title: '该日期暂无排班',
            content: '该医生当天没有可预约的时段，请换一个日期。',
            confirmText: '换日期',
            cancelText: '取消',
            success: (modalRes) => {
              if (modalRes.confirm) {
                this.setData({ selectedDate: '', selectedTime: '', timeSlots: [] });
              }
            }
          });
        }
      },
      fail: (err) => {
        console.error('[加载时段] 请求失败:', err);
        this.setData({ timeSlots: [], disabledTimeSlots: [] });
        wx.showModal({
          title: '网络超时',
          content: '排班时段加载失败，请检查网络后重试。',
          confirmText: '重试',
          cancelText: '取消',
          success: (modalRes) => {
            if (modalRes.confirm) {
              this.loadTimeSlotsForDate(selectedDate);
            }
          }
        });
      }
    });
  },

  selectTime(e) {
    const time = e.currentTarget.dataset.time;
    if (this.data.disabledTimeSlots.includes(time)) {
      wx.showToast({ title: '该时间已过', icon: 'none' });
      return;
    }

    // 重复预约统一由服务端校验，避免本地缓存过期导致误判为“预约冲突”
    this.setData({ selectedTime: time });
  },

  nextStep() {
    const currentStep = this.data.step;
    if (currentStep === 1 && this.data.selectedDept) {
      this.setData({ step: 2 });
    } else if (currentStep === 2 && this.data.selectedDoctor) {
      this.setData({ step: this.data.currentQuestionnaire ? 3 : 4 });
    } else if (currentStep === 3) {
      // 问卷全部为选填，直接进入时间选择
      this.setData({ step: 4 });
    } else if (currentStep === 4) {
      this.setData({ step: 5 });
    }
  },

  prevStep() {
    const currentStep = this.data.step;
    if (currentStep === 2) {
      this.setData({ step: 1 });
    } else if (currentStep === 3) {
      this.setData({ step: 2 });
    } else if (currentStep === 4) {
      this.setData({ step: this.data.currentQuestionnaire ? 3 : 2 });
    } else if (currentStep === 5) {
      this.setData({ step: 4 });
    } else if (currentStep === 6) {
      this.setData({ step: 5 });
    }
  },

  skipQuestionnaire() {
    this.setData({ skipQuestionnaire: true });
    this.nextStep();
  },

  submitAppointment() {
    if (this.submitting) {
      return;
    }

    if (!this.data.selectedDoctor || !this.data.selectedDate || !this.data.selectedTime) {
      wx.showModal({
        title: '预约信息不完整',
        content: '请先选择医生、日期和就诊时段。',
        showCancel: false,
        success: () => this.setData({ step: 2 })
      });
      return;
    }

    const hasCompleteInfo = wx.getStorageSync('hasCompleteInfo');
    if (!hasCompleteInfo) {
      wx.showModal({
        title: '提示',
        content: '请先完善个人信息',
        showCancel: false,
        success: () => {
          wx.redirectTo({ url: `/pages/patient-info/patient-info?fromUrl=${encodeURIComponent('/pages/appointment/appointment?doctorId=' + this.data.selectedDoctor.doctorId)}` });
        }
      });
      return;
    }

    const patientId = wx.getStorageSync('patientId');
    if (!patientId) {
      wx.showModal({
        title: '需要登录',
        content: '登录后才能提交预约',
        confirmText: '去登录',
        success: (res) => {
          if (res.confirm) wx.navigateTo({ url: '/pages/login/login' });
        }
      });
      return;
    }

    this.submitting = true;
    wx.showLoading({ title: '提交中...' });

    const appointmentData = {
      patientId: patientId,
      doctorId: this.data.selectedDoctor.doctorId,
      appointmentDate: this.data.selectedDate,
      timeSlot: this.data.selectedTime,
      patientName: wx.getStorageSync('patientName'),
      patientPhone: wx.getStorageSync('patientPhone') || '',
      questionnaireData: this.data.skipQuestionnaire ? null : JSON.stringify(this.data.questionnaireAnswers)
    };

    wx.request({
      url: app.globalData.baseUrl + '/api/appointments',
      method: 'POST',
      header: { 'content-type': 'application/json' },
      data: appointmentData,
      success: (res) => {
        wx.hideLoading();
        this.submitting = false;
        if (res.data && res.data.appointmentId) {
          this.setData({ currentAppointment: res.data });
          this.preparePayment(res.data.appointmentId);
          return;
        }

        const message = (res.data && res.data.message) || '预约未成功，请稍后重试';
        const isSlotConflict = /约满|占用|冲突|不可预约|不存在|已过/.test(message);
        wx.showModal({
          title: isSlotConflict ? '该时段不可预约' : '预约未成功',
          content: message,
          confirmText: isSlotConflict ? '重新选择' : '重试',
          cancelText: '取消',
          success: (modalRes) => {
            if (!modalRes.confirm) return;
            if (isSlotConflict) {
              this.setData({ step: 2, selectedTime: '', paymentInfo: null });
              this.loadTimeSlotsForDate(this.data.selectedDate);
            } else {
              this.submitAppointment();
            }
          }
        });
      },
      fail: (err) => {
        wx.hideLoading();
        this.submitting = false;
        console.error('预约失败:', err);
        wx.showModal({
          title: '网络超时',
          content: '预约未提交成功，请检查网络后重试。',
          confirmText: '重试',
          cancelText: '取消',
          success: (modalRes) => {
            if (modalRes.confirm) {
              this.submitAppointment();
            }
          }
        });
      }
    });
  },

  preparePayment(appointmentId) {
    wx.request({
      url: app.globalData.baseUrl + '/api/payment/prepare/' + appointmentId,
      method: 'GET',
      success: (res) => {
        if (res.data && !res.data.error) {
          this.setData({ paymentInfo: res.data, step: 6 });
        } else {
          wx.showModal({
            title: '支付信息加载失败',
            content: '预约已创建，但支付信息获取失败。可重试，或稍后在“我的预约”中继续支付。',
            confirmText: '重试',
            cancelText: '稍后',
            success: (modalRes) => {
              if (modalRes.confirm) {
                this.preparePayment(appointmentId);
              }
            }
          });
        }
      },
      fail: () => {
        wx.showModal({
          title: '网络超时',
          content: '预约已创建，但支付信息加载失败。请检查网络后重试。',
          confirmText: '重试',
          cancelText: '稍后',
          success: (modalRes) => {
            if (modalRes.confirm) {
              this.preparePayment(appointmentId);
            }
          }
        });
      }
    });
  },

  doPayment() {
    if (!this.data.selectedPayment) {
      wx.showToast({ title: '请选择支付方式', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '支付中...' });

    wx.request({
      url: app.globalData.baseUrl + '/api/payment/pay',
      method: 'POST',
      header: { 'content-type': 'application/json' },
      data: {
        appointmentId: this.data.currentAppointment.appointmentId,
        paymentMethod: this.data.selectedPayment,
        amount: this.data.paymentInfo.fee || (this.data.selectedDoctor.consultationFee || 30)
      },
      success: (res) => {
        wx.hideLoading();
        if (res.data && res.data.success) {
          const appointments = wx.getStorageSync('appointments') || [];
          appointments.unshift({
            appointmentId: this.data.currentAppointment.appointmentId,
            doctorName: this.data.selectedDoctor.name,
            deptName: this.data.selectedDept?.deptName,
            hospitalName: this.data.hospitalName,
            appointmentDate: this.data.selectedDate,
            timeSlot: this.data.selectedTime,
            status: 'pending',
            createTime: new Date().toISOString()
          });
          wx.setStorageSync('appointments', appointments);
          this.setData({ step: 7 });
        } else if (res.statusCode === 401) {
          wx.showToast({ title: '登录已失效，请重新登录', icon: 'none' });
        } else {
          wx.showToast({ title: (res.data && res.data.message) || '支付失败，请稍后再试', icon: 'none' });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '网络异常，支付未完成', icon: 'none' });
      }
    });
  },

  // 问卷：单选
  onRadioChange(e) {
    const qid = e.currentTarget.dataset.questionid;
    if (!qid) return;
    const answers = Object.assign({}, this.data.questionnaireAnswers);
    answers[qid] = e.detail.value;
    this.setData({ questionnaireAnswers: answers });
  },

  // 问卷：多选
  onCheckboxChange(e) {
    const qid = e.currentTarget.dataset.questionid;
    if (!qid) return;
    const answers = Object.assign({}, this.data.questionnaireAnswers);
    answers[qid] = e.detail.value;
    this.setData({ questionnaireAnswers: answers });
  },

  // 问卷：文本补充
  onTextareaInput(e) {
    const qid = e.currentTarget.dataset.questionid;
    if (!qid) return;
    const answers = Object.assign({}, this.data.questionnaireAnswers);
    answers[qid] = e.detail.value;
    this.setData({ questionnaireAnswers: answers });
  },

  // 选择支付方式
  selectPayment(e) {
    const method = e.currentTarget.dataset.method;
    if (!method) return;
    this.setData({ selectedPayment: method });
  },

  // 成功后查看预约
  viewAppointment() {
    wx.switchTab({ url: '/pages/my-appointments/my-appointments' });
  },

  goBack() {
    wx.switchTab({ url: '/pages/index/index' });
  }
});
