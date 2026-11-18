const app = getApp();

Page({
  data: {
    hospitalId: '',
    hospitalName: '',
    currentYear: 2024,
    currentMonth: 1,
    selectedDate: '',
    calendarData: [],
    scheduleDates: {},
    groupedSchedules: [],
    doctors: {},
    departments: {},
    expandedDoctors: {}
  },

  onLoad(options) {
    if (options && options.hospitalId) {
      this.setData({ hospitalId: options.hospitalId });
    } else {
      const storedHospitalId = wx.getStorageSync('hospitalId');
      this.setData({ hospitalId: storedHospitalId });
    }

    const now = new Date();
    const currentDate = this.formatDate(now);
    
    this.setData({
      currentYear: now.getFullYear(),
      currentMonth: now.getMonth() + 1,
      selectedDate: currentDate
    });

    this.loadHospitalName();
    this.loadScheduleDates();
    this.loadData();
  },

  formatDate(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  },

  loadHospitalName() {
    wx.request({
      url: app.globalData.baseUrl + '/api/hospitals/' + this.data.hospitalId,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.hospitalName) {
          this.setData({ hospitalName: res.data.hospitalName });
        }
      }
    });
  },

  loadScheduleDates() {
    wx.showLoading({ title: '加载中...' });
    
    let loadedCount = 0;
    const checkComplete = () => {
      loadedCount++;
      if (loadedCount >= 4) {
        wx.hideLoading();
        this.generateCalendar();
        this.groupSchedulesByDoctor();
      }
    };

    this.loadScheduleDatesFromApi(checkComplete);
    this.loadSchedules(checkComplete);
    this.loadDoctors(checkComplete);
    this.loadDepartments(checkComplete);
  },

  loadData() {
    this.loadScheduleDates();
  },

  loadScheduleDatesFromApi(callback) {
    wx.request({
      // 只取有排班的日期，用于日历标记；不拉取排班明细
      url: app.globalData.baseUrl + '/api/schedules/hospital/' + this.data.hospitalId + '/dates',
      method: 'GET',
      success: (res) => {
        const dates = (res.data && res.data.success && Array.isArray(res.data.data)) ? res.data.data : [];
        const scheduleDates = {};
        dates.forEach(date => {
          const dateStr = typeof date === 'string' ? date.substring(0, 10) : '';
          if (dateStr) {
            scheduleDates[dateStr] = true;
          }
        });
        this.setData({ scheduleDates });
      },
      fail: () => {
        this.setData({ scheduleDates: {} });
      },
      complete: callback
    });
  },

  loadSchedules(callback) {
    // 只拉取所选日期的排班，避免一次加载全院上万条排班
    const selectedDate = this.data.selectedDate;
    if (!selectedDate) {
      this._daySchedules = [];
      if (callback) callback();
      return;
    }

    wx.request({
      url: app.globalData.baseUrl + '/api/schedules/hospital/' + this.data.hospitalId + '/date/' + selectedDate,
      method: 'GET',
      success: (res) => {
        let schedules = [];
        if (res.data && res.data.success && Array.isArray(res.data.data)) {
          schedules = res.data.data;
        } else if (res.data && Array.isArray(res.data)) {
          schedules = res.data;
        }
        // 排班明细仅用于本地计算，不放进 data，避免上千条数据触发昂贵的 setData
        this._daySchedules = schedules;
      },
      fail: () => {
        this._daySchedules = [];
      },
      complete: callback
    });
  },

  loadDoctors(callback) {
    wx.request({
      url: app.globalData.baseUrl + '/api/doctors/hospital/' + this.data.hospitalId,
      method: 'GET',
      success: (res) => {
        const doctors = {};
        if (res.data && Array.isArray(res.data)) {
          res.data.forEach(doctor => {
            doctors[doctor.doctorId] = doctor;
          });
        }
        this.setData({ doctors });
      },
      fail: () => {
        this.setData({ doctors: {} });
      },
      complete: callback
    });
  },

  loadDepartments(callback) {
    wx.request({
      url: app.globalData.baseUrl + '/api/departments/hospital/' + this.data.hospitalId,
      method: 'GET',
      success: (res) => {
        const departments = {};
        if (res.data && Array.isArray(res.data)) {
          res.data.forEach(dept => {
            departments[dept.deptId] = dept;
          });
        }
        this.setData({ departments });
      },
      fail: () => {
        this.setData({ departments: {} });
      },
      complete: callback
    });
  },

  generateCalendar() {
    const year = this.data.currentYear;
    const month = this.data.currentMonth;
    
    const firstDay = new Date(year, month - 1, 1);
    const lastDay = new Date(year, month, 0);
    
    const daysInMonth = lastDay.getDate();
    const startWeekDay = firstDay.getDay();
    
    const calendarData = [];
    
    for (let i = 0; i < startWeekDay; i++) {
      calendarData.push({ day: '', dateStr: '', hasSchedule: false });
    }
    
    for (let day = 1; day <= daysInMonth; day++) {
      const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
      const hasSchedule = this.data.scheduleDates[dateStr] || false;
      calendarData.push({ day, dateStr, hasSchedule });
    }
    
    this.setData({ calendarData });
  },

  groupSchedulesByDoctor() {
    const schedules = this._daySchedules || [];
    const { doctors, departments } = this.data;
    const selectedDate = this.data.selectedDate;
    
    const filteredSchedules = schedules.filter(s => {
      const scheduleDate = s.workDate ? (typeof s.workDate === 'string' ? s.workDate : s.workDate.toString()) : '';
      return scheduleDate === selectedDate;
    });
    
    const grouped = {};
    
    filteredSchedules.forEach(schedule => {
      const doctorId = schedule.doctorId;
      if (!grouped[doctorId]) {
        const doctor = doctors[doctorId] || {};
        const dept = departments[doctor.deptId] || {};
        
        grouped[doctorId] = {
          doctorId: doctorId,
          doctorName: doctor.name || '未知医生',
          gender: doctor.gender || 'M',
          genderText: doctor.gender === 'F' ? '女' : '男',
          avatar: doctor.gender === 'F' ? '👩‍⚕️' : '👨‍⚕️',
          title: doctor.title || '未知职位',
          deptId: doctor.deptId || '',
          deptName: dept.deptName || '未知科室',
          schedules: []
        };
      }
      
      const timeSlot = schedule.timeSlot || '';
      if (timeSlot && timeSlot.includes('-')) {
        const expandedSlots = this.expandTimeSlot(schedule);
        grouped[doctorId].schedules.push(...expandedSlots);
      } else {
        grouped[doctorId].schedules.push(schedule);
      }
    });
    
    const groupedList = Object.values(grouped).map(item => ({
      ...item,
      schedules: item.schedules.sort((a, b) => (a.displayTimeSlot || a.timeSlot || '').localeCompare(b.displayTimeSlot || b.timeSlot || ''))
    }));
    
    this.setData({ groupedSchedules: groupedList });
  },

  expandTimeSlot(schedule) {
    const timeSlot = schedule.timeSlot || '';
    const [start, end] = timeSlot.split('-');
    
    if (!start || !end) {
      return [{ ...schedule, displayTimeSlot: timeSlot }];
    }
    
    const startHour = parseInt(start.split(':')[0]) || 0;
    const startMin = parseInt(start.split(':')[1]) || 0;
    const endHour = parseInt(end.split(':')[0]) || 0;
    const endMin = parseInt(end.split(':')[1]) || 0;
    
    const expandedSlots = [];
    let currentHour = startHour;
    let currentMin = startMin;
    
    while (currentHour < endHour || (currentHour === endHour && currentMin < endMin)) {
      const nextMin = currentMin + 20;
      let nextHour = currentHour;
      let nextM = nextMin;
      
      if (nextMin >= 60) {
        nextHour = currentHour + 1;
        nextM = nextMin - 60;
      }
      
      const startStr = `${String(currentHour).padStart(2, '0')}:${String(currentMin).padStart(2, '0')}`;
      const endStr = `${String(nextHour).padStart(2, '0')}:${String(nextM).padStart(2, '0')}`;
      
      expandedSlots.push({
        ...schedule,
        displayTimeSlot: `${startStr}-${endStr}`
      });
      
      currentHour = nextHour;
      currentMin = nextM;
    }
    
    return expandedSlots;
  },

  prevMonth() {
    let year = this.data.currentYear;
    let month = this.data.currentMonth;
    
    if (month === 1) {
      year--;
      month = 12;
    } else {
      month--;
    }
    
    this.setData({ currentYear: year, currentMonth: month });
    this.generateCalendar();
  },

  nextMonth() {
    let year = this.data.currentYear;
    let month = this.data.currentMonth;
    
    if (month === 12) {
      year++;
      month = 1;
    } else {
      month++;
    }
    
    this.setData({ currentYear: year, currentMonth: month });
    this.generateCalendar();
  },

  selectDate(e) {
    const dateStr = e.currentTarget?.dataset?.date;
    
    if (!dateStr) {
      return;
    }
    
    const today = this.formatDate(new Date());
    
    if (dateStr < today) {
      wx.showToast({ title: '只能选择未来日期', icon: 'none' });
      return;
    }
    
    this.setData({ selectedDate: dateStr, expandedDoctors: [] });
    this.loadSchedules(() => this.groupSchedulesByDoctor());
  },

  getDateClass(item) {
    const classes = [];
    if (!item.day) {
      classes.push('empty');
      return classes.join(' ');
    }
    
    const today = this.formatDate(new Date());
    if (item.dateStr === today) {
      classes.push('today');
    }
    
    if (item.dateStr === this.data.selectedDate) {
      classes.push('selected');
    }
    
    if (item.dateStr < today) {
      classes.push('past');
    }
    
    if (item.hasSchedule) {
      classes.push('has-schedule');
    }
    
    return classes.join(' ');
  },

  toggleDoctor(e) {
    const doctorId = e.currentTarget.dataset.doctorId;
    const expandedDoctors = { ...this.data.expandedDoctors };
    
    if (expandedDoctors[doctorId]) {
      delete expandedDoctors[doctorId];
    } else {
      expandedDoctors[doctorId] = true;
    }
    
    this.setData({ expandedDoctors });
  },

  goBack() {
    wx.navigateBack({ delta: 1 });
  },

  onShow() {
    if (this.data.groupedSchedules.length === 0) {
      this.loadData();
    }
  }
});