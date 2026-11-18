const app = getApp()
const scheduleService = require('../../utils/scheduleService.js')

Page({
  data: {
    currentYear: 2024,
    currentMonth: 1,
    selectedDate: '',
    calendarData: [],
    selectedScheduleList: [],
    doctorId: '',
    expandedSchedule: null,
    
    showModal: false,
    isEditing: false,
    editingId: null,
    
    durationOptions: ['15分钟', '20分钟', '30分钟', '45分钟', '60分钟'],
    durationIndex: 1,
    patientOptions: ['1', '2', '3', '4', '5'],
    patientIndex: 1,
    submitting: false,
    
    statusOptions: ['可预约', '已关闭'],
    statusIndex: 0,
    
    formData: {
      date: '',
      startTime: '',
      endTime: '',
      duration: 20,
      maxPatients: 2,
      status: 1
    },
    
    scheduleDates: {}
  },

  onLoad: function (options) {
    const now = new Date()
    const currentDate = this.formatDate(now)
    
    const doctorId = wx.getStorageSync('doctorId') || 'DOC001'
    
    this.setData({
      currentYear: now.getFullYear(),
      currentMonth: now.getMonth() + 1,
      selectedDate: currentDate,
      'formData.date': currentDate,
      doctorId: doctorId
    })
    
    this.generateCalendar()
    this.loadScheduleDates()
    this.loadScheduleList()
  },

  formatDate: function (date) {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  },

  generateCalendar: function () {
    const year = this.data.currentYear
    const month = this.data.currentMonth
    
    const firstDay = new Date(year, month - 1, 1)
    const lastDay = new Date(year, month, 0)
    
    const daysInMonth = lastDay.getDate()
    const startWeekDay = firstDay.getDay()
    
    const calendarData = []
    
    for (let i = 0; i < startWeekDay; i++) {
      calendarData.push({ day: '', dateStr: '', hasSchedule: false })
    }
    
    for (let day = 1; day <= daysInMonth; day++) {
      const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
      const hasSchedule = this.data.scheduleDates[dateStr] || false
      calendarData.push({ day, dateStr, hasSchedule })
    }
    
    this.setData({ calendarData })
  },

  loadScheduleDates: function () {
    const doctorId = this.data.doctorId
    
    scheduleService.getScheduleDates(doctorId, (err, dates) => {
      if (err) {
        console.error('加载排班日期失败:', err)
        return
      }
      
      const scheduleDates = {}
      dates.forEach(date => {
        scheduleDates[date] = true
      })
      this.setData({ scheduleDates })
      this.generateCalendar()
    })
  },

  loadScheduleList: function () {
    const doctorId = this.data.doctorId
    const date = this.data.selectedDate
    
    scheduleService.getSchedulesByDate(doctorId, date, (err, schedules) => {
      if (err) {
        console.error('加载排班列表失败:', err)
        this.setData({ selectedScheduleList: [] })
        return
      }
      
      const expandedSchedules = []
      const scheduleArray = schedules || []
      
      for (let itemIndex = 0; itemIndex < scheduleArray.length; itemIndex++) {
        const item = scheduleArray[itemIndex]
        const timeSlot = item.timeSlot || ''
        const timeSlots = []
        
        if (timeSlot) {
          const [start, end] = timeSlot.split('-')
          if (start && end) {
            const startHour = parseInt(start.split(':')[0]) || 0
            const startMin = parseInt(start.split(':')[1]) || 0
            const endHour = parseInt(end.split(':')[0]) || 0
            const endMin = parseInt(end.split(':')[1]) || 0
            
            let currentHour = startHour
            let currentMin = startMin
            
            while (currentHour < endHour || (currentHour === endHour && currentMin < endMin)) {
              const nextMin = currentMin + 20
              let nextHour = currentHour
              let nextM = nextMin
              
              if (nextMin >= 60) {
                nextHour = currentHour + 1
                nextM = nextMin - 60
              }
              
              const startStr = `${String(currentHour).padStart(2, '0')}:${String(currentMin).padStart(2, '0')}`
              const endStr = `${String(nextHour).padStart(2, '0')}:${String(nextM).padStart(2, '0')}`
              
              timeSlots.push(`${startStr}~${endStr}`)
              
              currentHour = nextHour
              currentMin = nextM
            }
          }
        }
        
        for (let index = 0; index < timeSlots.length; index++) {
          const slot = timeSlots[index]
          expandedSchedules.push({
            ...item,
            id: `${item.scheduleId}-${index}`,
            displayTimeSlot: slot,
            originalScheduleId: item.scheduleId,
            patients: []
          })
        }
      }
      
      this.setData({ selectedScheduleList: expandedSchedules })
      
      // 加载每个排班的预约患者
      this.loadSchedulePatients(expandedSchedules)
    })
  },

  loadSchedulePatients: function (schedules) {
    const scheduleMap = {}
    schedules.forEach(s => {
      if (!scheduleMap[s.scheduleId]) {
        scheduleMap[s.scheduleId] = []
      }
    })
    
    let completedCount = 0
    const totalSchedules = Object.keys(scheduleMap).length
    
    if (totalSchedules === 0) {
      return
    }
    
    Object.keys(scheduleMap).forEach(scheduleId => {
      wx.request({
        url: app.globalData.baseUrl + '/api/appointments/schedule/' + scheduleId,
        method: 'GET',
        success: (res) => {
          completedCount++
          const patients = res.data || []
          scheduleMap[scheduleId] = patients
          
          if (completedCount === totalSchedules) {
            this.updateSchedulesWithPatients(scheduleMap)
          }
        },
        fail: () => {
          completedCount++
          scheduleMap[scheduleId] = []
          
          if (completedCount === totalSchedules) {
            this.updateSchedulesWithPatients(scheduleMap)
          }
        }
      })
    })
  },

  updateSchedulesWithPatients: function (scheduleMap) {
    const updatedSchedules = this.data.selectedScheduleList.map(s => ({
      ...s,
      patients: scheduleMap[s.scheduleId] || []
    }))
    
    this.setData({ selectedScheduleList: updatedSchedules })
  },

  prevMonth: function () {
    let year = this.data.currentYear
    let month = this.data.currentMonth
    
    if (month === 1) {
      year--
      month = 12
    } else {
      month--
    }
    
    this.setData({ currentYear: year, currentMonth: month })
    this.generateCalendar()
  },

  nextMonth: function () {
    let year = this.data.currentYear
    let month = this.data.currentMonth
    
    if (month === 12) {
      year++
      month = 1
    } else {
      month++
    }
    
    this.setData({ currentYear: year, currentMonth: month })
    this.generateCalendar()
  },

  selectDate: function (e) {
    const dateStr = e.currentTarget?.dataset?.date
    
    if (!dateStr) {
      return
    }
    
    const today = this.formatDate(new Date())
    
    if (dateStr < today) {
      wx.showToast({ title: '只能选择未来日期', icon: 'none' })
      return
    }
    
    this.setData({ selectedDate: dateStr })
    this.loadScheduleList()
  },

  getDateClass: function (item) {
    const classes = []
    if (!item.day) {
      classes.push('empty')
      return classes.join(' ')
    }
    
    const today = this.formatDate(new Date())
    if (item.dateStr === today) {
      classes.push('today')
    }
    
    if (item.dateStr === this.data.selectedDate) {
      classes.push('selected')
    }
    
    if (item.dateStr < today) {
      classes.push('past')
    }
    
    if (item.hasSchedule) {
      classes.push('has-schedule')
    }
    
    return classes.join(' ')
  },

  openAddModal: function () {
    this.setData({
      showModal: true,
      isEditing: false,
      editingId: null,
      'formData.date': this.data.selectedDate,
      'formData.startTime': '09:00',
      'formData.endTime': '11:00',
      'formData.duration': 20,
      'formData.maxPatients': 2,
      'formData.status': 1,
      durationIndex: 1,
      statusIndex: 0
    })
  },

  editSchedule: function (e) {
    const item = e.currentTarget.dataset.item
    const timeSlot = item.timeSlot?.split('-') || []
    const duration = this.getSlotMinutes(item.timeSlot) || 20
    const durationIndex = this.getDurationIndex(duration)
    
    this.setData({
      showModal: true,
      isEditing: true,
      editingId: item.scheduleId,
      'formData.date': item.workDate || this.data.selectedDate,
      'formData.startTime': timeSlot[0] || '09:00',
      'formData.endTime': timeSlot[1] || '11:00',
      'formData.duration': duration,
      'formData.maxPatients': item.maxPatients || 2,
      'formData.status': item.status || 1,
      durationIndex: durationIndex,
      patientIndex: this.getPatientIndex(item.maxPatients),
      statusIndex: item.status === 1 ? 0 : 1
    })
  },

  getDurationIndex: function (minutes) {
    const options = [15, 20, 30, 45, 60]
    const index = options.indexOf(minutes)
    return index === -1 ? 1 : index
  },

  getPatientIndex: function (maxPatients) {
    const index = this.data.patientOptions.indexOf(String(maxPatients))
    return index === -1 ? 1 : index
  },

  getSlotMinutes: function (timeSlot) {
    if (!timeSlot || timeSlot.indexOf('-') === -1) return 0
    const parts = timeSlot.split('-')
    return this.toMinutes(parts[1]) - this.toMinutes(parts[0])
  },

  toMinutes: function (timeStr) {
    const parts = String(timeStr || '').trim().split(':')
    if (parts.length < 2) return 0
    return parseInt(parts[0]) * 60 + parseInt(parts[1])
  },

  formatMinutes: function (minutes) {
    const hour = Math.floor(minutes / 60)
    const minute = minutes % 60
    return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`
  },

  closeModal: function () {
    this.setData({ showModal: false })
  },

  stopPropagation: function () {},

  togglePatientList: function (e) {
    const scheduleId = e.currentTarget.dataset.id
    const currentExpanded = this.data.expandedSchedule
    
    this.setData({
      expandedSchedule: currentExpanded === scheduleId ? null : scheduleId
    })
  },

  onDateChange: function (e) {
    this.setData({ 'formData.date': e.detail.value })
  },

  onStartTimeChange: function (e) {
    this.setData({ 'formData.startTime': e.detail.value })
  },

  onEndTimeChange: function (e) {
    this.setData({ 'formData.endTime': e.detail.value })
  },

  onDurationChange: function (e) {
    const index = e.detail.value
    const options = [15, 20, 30, 45, 60]
    this.setData({
      durationIndex: index,
      'formData.duration': options[index]
    })
  },

  onStatusChange: function (e) {
    const index = e.detail.value
    this.setData({
      statusIndex: index,
      'formData.status': index === 0 ? 1 : 0
    })
  },

  onPatientChange: function (e) {
    const index = e.detail.value
    this.setData({
      patientIndex: index,
      'formData.maxPatients': parseInt(this.data.patientOptions[index])
    })
  },

  createScheduleRequest: function (requestData) {
    return new Promise(resolve => {
      scheduleService.createSchedule(requestData, (err) => {
        resolve({ ok: !err, err: err || '' })
      })
    })
  },

  saveSchedule: function () {
    if (this.data.submitting) {
      return
    }

    const data = this.data.formData
    
    if (!data.date || !data.startTime || !data.endTime) {
      wx.showModal({ title: '信息不完整', content: '请选择日期并填写开始、结束时间。', showCancel: false })
      return
    }
    
    const startMinutes = this.toMinutes(data.startTime)
    const endMinutes = this.toMinutes(data.endTime)
    
    if (endMinutes <= startMinutes) {
      wx.showModal({ title: '时间选择有误', content: '结束时间必须晚于开始时间，请重新选择。', showCancel: false })
      return
    }
    
    const totalMinutes = endMinutes - startMinutes
    if (totalMinutes < 20) {
      wx.showModal({ title: '时间太短', content: '每次排班至少 20 分钟，请调整时间范围。', showCancel: false })
      return
    }
    
    if (totalMinutes % 20 !== 0) {
      wx.showModal({ title: '时间选择有误', content: '排班时长需为 20 分钟的整数倍，请调整结束时间。', showCancel: false })
      return
    }
    
    if (!this.data.isEditing && totalMinutes < 120) {
      wx.showModal({ title: '排班时长不足', content: '每位医生当天排班至少 2 小时，请延长结束时间或分批添加。', showCancel: false })
      return
    }

    const segmentDuration = parseInt(data.duration) || 20
    if (!this.data.isEditing && totalMinutes % segmentDuration !== 0) {
      wx.showModal({
        title: '时长无法拆分',
        content: `当前总时长无法按 ${segmentDuration} 分钟整除，请调整结束时间或更换每段时长。`,
        showCancel: false
      })
      return
    }
    
    const hospitalId = wx.getStorageSync('hospitalId')
    if (!hospitalId) {
      wx.showModal({
        title: '登录状态失效',
        content: '登录信息已丢失，请重新登录后再设置排班。',
        showCancel: false,
        success: () => wx.reLaunch({ url: '/pages/login/login' })
      })
      return
    }

    const maxPatients = parseInt(data.maxPatients) || 2
    
    if (this.data.isEditing) {
      this.setData({ submitting: true })
      const requestData = {
        doctorId: this.data.doctorId,
        hospitalId: hospitalId,
        workDate: data.date,
        timeSlot: `${data.startTime}-${data.endTime}`,
        maxPatients: maxPatients,
        status: data.status
      }
      
      scheduleService.updateSchedule(this.data.editingId, requestData, (err) => {
        this.setData({ submitting: false })
        if (err) {
          this.showSaveError(err, () => this.saveSchedule())
          return
        }
        wx.showToast({ title: '修改成功', icon: 'success' })
        this.closeModal()
        this.loadScheduleDates()
        this.loadScheduleList()
      })
      return
    }

    const requests = []
    for (let offset = 0; offset < totalMinutes; offset += segmentDuration) {
      const segmentStart = startMinutes + offset
      const segmentEnd = segmentStart + segmentDuration
      requests.push({
        doctorId: this.data.doctorId,
        hospitalId: hospitalId,
        workDate: data.date,
        timeSlot: `${this.formatMinutes(segmentStart)}-${this.formatMinutes(segmentEnd)}`,
        maxPatients: maxPatients,
        status: data.status
      })
    }
    
    this.setData({ submitting: true })
    wx.showLoading({ title: '保存中...' })
    Promise.all(requests.map(requestData => this.createScheduleRequest(requestData))).then(results => {
      wx.hideLoading()
      this.setData({ submitting: false })
      const failed = results.filter(item => !item.ok)
      if (failed.length === 0) {
        wx.showToast({ title: '添加成功', icon: 'success' })
        this.closeModal()
        this.setData({ selectedDate: data.date })
        this.loadScheduleDates()
        this.loadScheduleList()
        return
      }

      const networkFailed = failed.some(item => !item.err || item.err.indexOf('网络') >= 0)
      wx.showModal({
        title: networkFailed ? '网络超时' : '部分时段未保存',
        content: networkFailed
          ? `有 ${failed.length} 个时段未保存，可能是网络超时。已保存的时段仍然有效，可重试未完成的时段。`
          : `有 ${failed.length} 个时段保存失败（可能已被占用），请调整时间后重试。`,
        confirmText: '重试',
        cancelText: '稍后处理',
        success: (res) => {
          this.loadScheduleList()
          if (res.confirm) {
            this.saveSchedule()
          }
        }
      })
    })
  },

  showSaveError: function (message, retry) {
    const isNetwork = !message || message.indexOf('网络') >= 0 || message.indexOf('超时') >= 0
    wx.showModal({
      title: isNetwork ? '网络超时' : '保存失败',
      content: isNetwork ? '网络请求超时，排班未保存。请检查网络后重试。' : message,
      confirmText: '重试',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm && retry) {
          retry()
        }
      }
    })
  },

  deleteSchedule: function (e) {
    const item = e.currentTarget.dataset.item
    const scheduleId = item.scheduleId
    const bookedCount = item.bookedCount || 0
    
    if (bookedCount > 0) {
      wx.showModal({
        title: '预约提醒',
        content: `该时段已有 ${bookedCount} 位患者预约，请先与患者协调后再取消排班。确定要继续取消吗？`,
        confirmColor: '#ff4d4f',
        success: (res) => {
          if (res.confirm) {
            this.doDeleteSchedule(scheduleId)
          }
        }
      })
    } else {
      wx.showModal({
        title: '确认删除',
        content: '删除后将无法恢复，确定要删除吗？',
        success: (res) => {
          if (res.confirm) {
            this.doDeleteSchedule(scheduleId)
          }
        }
      })
    }
  },
  
  doDeleteSchedule: function (scheduleId) {
    scheduleService.deleteSchedule(scheduleId, (err, success) => {
      if (err) {
        wx.showToast({ title: err, icon: 'none' })
        return
      }
      wx.showToast({ title: '删除成功', icon: 'success' })
      this.loadScheduleDates()
      this.loadScheduleList()
    })
  }
})