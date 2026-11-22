const app = getApp();
const deptIcon = require('../../utils/dept-icon');

Page({
  data: {
    schedules: [],
    dateGroups: [],
    doctors: [],
    departments: [],
    hospitalList: [],
    hospitalId: '',
    selectedHospital: '',
    selectedHospitalName: '全部医院',
    selectedDept: '',
    selectedDeptName: '全部科室',
    selectedDoctor: '',
    selectedDoctorName: '全部医生',
    selectedDate: '',
    today: '',
    isSuperAdmin: false
  },

  onLoad(options) {
    const role = wx.getStorageSync('adminRole');
    const hospitalId = options?.hospitalId || wx.getStorageSync('currentHospitalId') || wx.getStorageSync('hospitalId');
    const today = new Date().toISOString().split('T')[0];
    
    this.setData({
      hospitalId: hospitalId,
      today: today,
      selectedDate: today,
      isSuperAdmin: role === 'SUPER_ADMIN',
      selectedHospital: hospitalId || ''
    });
    
    if (this.data.isSuperAdmin) {
      this.loadHospitals();
    } else if (hospitalId) {
      this.setData({ selectedHospitalName: wx.getStorageSync('currentHospitalName') || '当前医院' });
      this.loadDepartments(hospitalId);
    }
    this.loadDoctors();
    this.loadSchedules();
  },

  onShow() {
    this.loadSchedules();
  },

  loadHospitals() {
    wx.request({
      url: app.globalData.baseUrl + '/api/hospitals',
      method: 'GET',
      success: (res) => {
        if (res.data && Array.isArray(res.data)) {
          this.setData({ hospitalList: res.data });
        }
      }
    });
  },

  loadDepartments(hospitalId) {
    wx.request({
      url: app.globalData.baseUrl + '/api/departments/hospital/' + hospitalId,
      method: 'GET',
      success: (res) => {
        if (res.data && Array.isArray(res.data)) {
          this.setData({ departments: res.data });
        }
      }
    });
  },

  loadDoctors() {
    let url = app.globalData.baseUrl + '/api/doctors';
    if (this.data.selectedHospital && !this.data.isSuperAdmin) {
      url = app.globalData.baseUrl + '/api/doctors/hospital/' + this.data.selectedHospital;
    }

    wx.request({
      url: url,
      method: 'GET',
      success: (res) => {
        if (res.data && Array.isArray(res.data)) {
          this.setData({ doctors: res.data });
        }
      }
    });
  },

  loadSchedules() {
    const { selectedHospital, selectedDoctor, selectedDate } = this.data;

    if (!selectedHospital || !selectedDate) {
      this.setData({ dateGroups: [] });
      return;
    }

    // 只拉取所选日期，避免一次加载全院数千条排班导致页面卡顿
    const url = app.globalData.baseUrl + '/api/schedules/hospital/' + selectedHospital + '/date/' + selectedDate;

    wx.showLoading({ title: '加载中...' });

    wx.request({
      url: url,
      method: 'GET',
      success: (res) => {
        wx.hideLoading();
        const list = res.data && res.data.success
          ? (res.data.data || [])
          : (Array.isArray(res.data) ? res.data : []);

        const raw = selectedDoctor ? list.filter(s => s.doctorId === selectedDoctor) : list;

        const doctorMap = {};
        this.data.doctors.forEach(d => { doctorMap[d.doctorId] = d; });
        const deptMap = {};
        this.data.departments.forEach(d => { deptMap[d.deptId] = d; });

        const schedulesWithInfo = raw.map(schedule => {
          const doctor = doctorMap[schedule.doctorId];
          const dept = deptMap[doctor?.deptId || schedule.deptId];
          const deptName = schedule.deptName || (dept ? dept.deptName : '未知科室');
          return {
            ...schedule,
            // 服务端已返回医生/科室信息，缺失时再用本地医生列表兜底
            doctorName: schedule.doctorName || (doctor ? doctor.name : '未知医生'),
            doctorTitle: schedule.doctorTitle || (doctor ? doctor.title : ''),
            deptName: deptName,
            deptIcon: deptIcon.iconPath(deptName),
            remainingSlots: schedule.remainingSlots != null
              ? schedule.remainingSlots
              : schedule.maxPatients - schedule.bookedCount
          };
        });
        this.groupByDate(schedulesWithInfo);
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({
          title: '加载失败',
          icon: 'none'
        });
      }
    });
  },

  groupByDate(schedules) {
    const groups = {};
    
    schedules.forEach(schedule => {
      const date = schedule.workDate;
      if (!groups[date]) {
        const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
        const weekday = weekdays[new Date(date).getDay()];
        groups[date] = {
          date: date,
          weekday: weekday,
          schedules: []
        };
      }
      groups[date].schedules.push(schedule);
    });

    const sortedGroups = Object.values(groups).sort((a, b) => a.date.localeCompare(b.date));
    this.setData({ dateGroups: sortedGroups });
  },

  onHospitalChange(e) {
    const index = e.detail.value;
    const hospitalList = this.data.hospitalList;
    
    if (index >= 0 && hospitalList[index]) {
      const hospital = hospitalList[index];
      this.setData({ 
        selectedHospital: hospital.hospitalId,
        selectedHospitalName: hospital.hospitalName,
        selectedDept: '',
        selectedDeptName: '全部科室',
        selectedDoctor: '',
        selectedDoctorName: '全部医生'
      });
      this.loadDepartments(hospital.hospitalId);
    } else {
      this.setData({ 
        selectedHospital: '',
        selectedHospitalName: '全部医院',
        selectedDept: '',
        selectedDeptName: '全部科室',
        selectedDoctor: '',
        selectedDoctorName: '全部医生'
      });
    }
    this.loadDoctors();
    this.loadSchedules();
  },

  onDeptChange(e) {
    const index = e.detail.value;
    const departments = this.data.departments;
    
    if (index >= 0 && departments[index]) {
      const dept = departments[index];
      this.setData({ 
        selectedDept: dept.deptId,
        selectedDeptName: dept.deptName,
        selectedDoctor: '',
        selectedDoctorName: '全部医生'
      });
    } else {
      this.setData({ 
        selectedDept: '',
        selectedDeptName: '全部科室',
        selectedDoctor: '',
        selectedDoctorName: '全部医生'
      });
    }
    this.loadSchedules();
  },

  onDoctorChange(e) {
    const index = e.detail.value;
    const doctors = this.data.doctors;
    
    if (index >= 0 && doctors[index]) {
      this.setData({ 
        selectedDoctor: doctors[index].doctorId,
        selectedDoctorName: doctors[index].name
      });
    } else {
      this.setData({ 
        selectedDoctor: '',
        selectedDoctorName: '全部医生'
      });
    }
    this.loadSchedules();
  },

  onDateChange(e) {
    this.setData({ selectedDate: e.detail.value });
    this.loadSchedules();
  },

  selectToday() {
    const today = new Date().toISOString().split('T')[0];
    this.setData({ selectedDate: today });
    this.loadSchedules();
  },

  goBack() {
    wx.navigateBack();
  }
})