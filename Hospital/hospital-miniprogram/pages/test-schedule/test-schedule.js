const app = getApp()

Page({
  data: {
    testDates: [],
    form: {
      date: '',
      time: ''
    },
    resultText: ''
  },

  onLoad: function () {
    this.initTestDates()
    console.log('测试页面加载完成')
  },

  initTestDates: function () {
    const dates = []
    const now = new Date()
    for (let i = 0; i < 7; i++) {
      const date = new Date(now)
      date.setDate(date.getDate() + i)
      dates.push({
        day: date.getDate(),
        date: `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`,
        hasSchedule: i % 2 === 0
      })
    }
    this.setData({ testDates: dates })
  },

  onDateClick: function (e) {
    const date = e.currentTarget.dataset.date
    console.log('日期点击:', date)
    wx.showToast({
      title: `选择了: ${date}`,
      icon: 'none'
    })
  },

  onDateChange: function (e) {
    console.log('日期选择:', e.detail.value)
    this.setData({ 'form.date': e.detail.value })
  },

  onTimeChange: function (e) {
    console.log('时间选择:', e.detail.value)
    this.setData({ 'form.time': e.detail.value })
  },

  onSubmit: function () {
    console.log('提交按钮点击')
    const form = this.data.form
    
    if (!form.date || !form.time) {
      this.setData({ resultText: '请填写完整信息' })
      return
    }

    const apiUrl = app.getApiUrl ? app.getApiUrl() : app.globalData.baseUrl
    console.log('API地址:', apiUrl)
    
    wx.request({
      url: `${apiUrl}/api/schedules`,
      method: 'POST',
      data: {
        doctorId: 'DOC001',
        hospitalId: 'HOSP001',
        workDate: form.date,
        timeSlot: `${form.time}-12:00`,
        maxPatients: 2,
        status: 1
      },
      success: (res) => {
        console.log('API响应:', res)
        if (res.data && res.data.success) {
          this.setData({ resultText: '添加成功!' })
        } else {
          this.setData({ resultText: '添加失败: ' + (res.data?.message || '未知错误') })
        }
      },
      fail: (err) => {
        console.error('API请求失败:', err)
        this.setData({ resultText: '网络请求失败' })
      }
    })
  }
})