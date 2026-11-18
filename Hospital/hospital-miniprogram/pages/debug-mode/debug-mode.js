const app = getApp()

Page({
  data: {
    debugMode: false,
    currentApiUrl: ''
  },

  onLoad: function () {
    this.loadSettings()
  },

  loadSettings: function () {
    const debugMode = wx.getStorageSync('debugMode') || false
    const apiUrl = app.getApiUrl()
    
    this.setData({
      debugMode: debugMode,
      currentApiUrl: apiUrl
    })
  },

  onDebugModeChange: function (e) {
    const enabled = e.detail.value
    app.setDebugMode(enabled)
    
    wx.showToast({
      title: enabled ? '已启用调试模式' : '已关闭调试模式',
      icon: 'success'
    })
    
    this.loadSettings()
  },

  goBack: function () {
    wx.navigateBack()
  }
})