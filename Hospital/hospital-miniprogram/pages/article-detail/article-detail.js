const app = getApp();

Page({
  data: {
    article: null,
    coverFull: '',
    dateText: '',
    paragraphs: []
  },

  onLoad(options) {
    if (!options.id) {
      wx.showToast({ title: '内容不存在', icon: 'none' });
      return;
    }
    wx.showLoading({ title: '加载中...' });
    wx.request({
      url: app.globalData.baseUrl + '/api/hospital-articles/' + options.id,
      method: 'GET',
      success: (res) => {
        wx.hideLoading();
        const article = res.data || {};
        const content = (article.content || '').split('\n').map(t => t.trim()).filter(Boolean);
        this.setData({
          article,
          coverFull: article.coverUrl ? app.globalData.baseUrl + article.coverUrl : '',
          dateText: (article.createTime || '').replace('T', ' ').slice(0, 16),
          paragraphs: content
        });
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '加载失败', icon: 'none' });
      }
    });
  },

  goBack() {
    wx.navigateBack();
  }
});
