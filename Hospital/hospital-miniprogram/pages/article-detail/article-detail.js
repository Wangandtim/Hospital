const app = getApp();
const placeholder = require('../../utils/placeholder');

Page({
  data: {
    article: null,
    coverFull: '',
    dateText: '',
    paragraphs: [],
    extra: null
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
          dateText: String(article.publishedAt || article.createTime || '').replace('T', ' ').slice(0, 16),
          paragraphs: content,
          extra: placeholder.articleExtra(article)
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
  },

  goNoticeList() {
    wx.navigateTo({ url: '/pages/notice/notice?hospitalId=' + (this.data.article && this.data.article.hospitalId || '') });
  },

  goMyAppointments() {
    wx.switchTab({ url: '/pages/my-appointments/my-appointments' });
  }
});
