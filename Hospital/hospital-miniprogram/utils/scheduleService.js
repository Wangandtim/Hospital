const app = getApp()

class ScheduleService {
  constructor() {
    this.apiUrl = app.globalData.baseUrl
  }

  getApiUrl() {
    if (app.getApiUrl) {
      return app.getApiUrl()
    }
    return this.apiUrl
  }

  getScheduleDates(doctorId, callback) {
    const url = `${this.getApiUrl()}/api/schedules/doctor/${doctorId}/dates`
    wx.request({
      url,
      method: 'GET',
      success: (res) => {
        if (callback) {
          if (res.data && res.data.success) {
            callback(null, res.data.data || [])
          } else {
            callback(res.data?.message || '获取失败', null)
          }
        }
      },
      fail: (err) => {
        if (callback) {
          callback(err, null)
        }
      }
    })
  }

  getSchedulesByDate(doctorId, date, callback) {
    const url = `${this.getApiUrl()}/api/schedules/doctor/${doctorId}/date/${date}`
    console.log('scheduleService.getSchedulesByDate:', url)
    
    wx.request({
      url,
      method: 'GET',
      success: (res) => {
        console.log('getSchedulesByDate 响应:', res)
        if (callback) {
          if (res.data && res.data.success) {
            callback(null, res.data.data || [])
          } else {
            callback(res.data?.message || '获取失败', null)
          }
        }
      },
      fail: (err) => {
        console.error('getSchedulesByDate 失败:', err)
        if (callback) {
          callback(err, null)
        }
      }
    })
  }

  createSchedule(data, callback) {
    const url = `${this.getApiUrl()}/api/schedules`
    wx.request({
      url,
      method: 'POST',
      data,
      header: { 'Content-Type': 'application/json' },
      success: (res) => {
        if (callback) {
          if (res.data && res.data.success) {
            callback(null, res.data.data)
          } else {
            callback(res.data?.message || '创建失败', null)
          }
        }
      },
      fail: (err) => {
        if (callback) {
          callback(err, null)
        }
      }
    })
  }

  updateSchedule(scheduleId, data, callback) {
    const url = `${this.getApiUrl()}/api/schedules/${scheduleId}`
    wx.request({
      url,
      method: 'PUT',
      data,
      header: { 'Content-Type': 'application/json' },
      success: (res) => {
        if (callback) {
          if (res.data && res.data.success) {
            callback(null, res.data.data)
          } else {
            callback(res.data?.message || '更新失败', null)
          }
        }
      },
      fail: (err) => {
        if (callback) {
          callback(err, null)
        }
      }
    })
  }

  deleteSchedule(scheduleId, callback) {
    const url = `${this.getApiUrl()}/api/schedules/${scheduleId}`
    wx.request({
      url,
      method: 'DELETE',
      success: (res) => {
        if (callback) {
          if (res.data && res.data.success) {
            callback(null, true)
          } else {
            callback(res.data?.message || '删除失败', null)
          }
        }
      },
      fail: (err) => {
        if (callback) {
          callback(err, null)
        }
      }
    })
  }
}

module.exports = new ScheduleService()