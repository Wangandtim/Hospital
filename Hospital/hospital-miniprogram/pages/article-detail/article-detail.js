const app = getApp();
const elderly = require('../../utils/elderly');
const placeholder = require('../../utils/placeholder');

Page({
  data: {
    article: null,
    coverFull: '',
    dateText: '',
    paragraphs: [],
    extra: null,
    loading: true,
    error: '',
    elderly: false
  },

  onLoad(options) {
    this.articleId = options.id || '';
    this.setData({ elderly: elderly.isElderly() });

    if (!this.articleId) {
      this.setData({ loading: false, error: '内容不存在' });
      return;
    }
    this.loadArticle();
  },

  loadArticle() {
    this.setData({ loading: true, error: '' });
    wx.request({
      url: app.globalData.baseUrl + '/api/hospital-articles/' + this.articleId,
      method: 'GET',
      timeout: 10000,
      success: (res) => {
        const article = (res.data && (res.data.article || res.data)) || {};
        if (res.statusCode !== 200 || (!article.articleId && !article.title)) {
          this.setData({ loading: false, error: '公告内容不存在' });
          return;
        }
        const content = String(article.content || '').split('\n').map(t => t.trim()).filter(Boolean);
        this.setData({
          article,
          coverFull: article.coverUrl ? app.globalData.baseUrl + article.coverUrl : '',
          dateText: String(article.publishedAt || article.createTime || '').replace('T', ' ').slice(0, 16),
          paragraphs: content,
          extra: placeholder.articleExtra(article),
          loading: false
        });
      },
      fail: () => {
        this.setData({ loading: false, error: '公告加载失败' });
      }
    });
  },

  retryLoad() {
    if (this.articleId) {
      this.loadArticle();
    } else {
      wx.navigateBack();
    }
  },

  onCoverError() {
    this.setData({ coverFull: '' });
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
