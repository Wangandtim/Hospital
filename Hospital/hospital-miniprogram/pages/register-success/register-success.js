const app = getApp()

Page({
  data: {
    activationKey: '',
    status: 'pending'
  },

  onKeyInput: function (e) {
    this.setData({ activationKey: e.detail.value })
  },

  activateAccount: function () {
    const key = this.data.activationKey
    
    if (!key) {
      wx.showToast({ title: '请输入激活密钥', icon: 'none' })
      return
    }

    wx.showLoading({ title: '激活中...' })

    wx.request({
      url: `${app.globalData.baseUrl}/api/doctors/activate`,
      method: 'POST',
      data: {
        activationKey: key
      },
      success: (res) => {
        wx.hideLoading()
        if (res.data && res.data.success) {
          wx.showToast({ title: '激活成功', icon: 'success' })
          setTimeout(() => {
            wx.redirectTo({ url: '/pages/login/login' })
          }, 2000)
        } else {
          wx.showToast({ title: res.data?.message || '激活失败', icon: 'none' })
        }
      },
      fail: () => {
        wx.hideLoading()
        wx.showToast({ title: '网络异常', icon: 'none' })
      }
    })
  }
})