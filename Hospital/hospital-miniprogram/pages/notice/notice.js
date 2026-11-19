const app = getApp();
const pager = require('../../utils/pager');

Page({
  data: {
    hospitalId: '',
    hospitalName: '',
    articles: [],
    pager: { status: 'idle', hasMore: true, total: 0, error: '', loading: false }
  },

  onLoad(options) {
    let hospitalId = options.hospitalId || '';
    let hospitalName = options.hospitalName ? decodeURIComponent(options.hospitalName) : '';
    if (!hospitalId) {
      const selected = wx.getStorageSync('selectedHospital');
      if (selected && selected.hospitalId) {
        hospitalId = selected.hospitalId;
        hospitalName = hospitalName || selected.hospitalName || '';
      }
    }
    this.setData({ hospitalId, hospitalName });
    this.createPager();
    if (hospitalId) {
      this.pager.reset();
    }
  },

  onUnload() {
    if (this.pager) this.pager.destroy();
  },

  onReachBottom() {
    if (this.pager) this.pager.next();
  },

  createPager() {
    this.pager = pager.create({
      size: 20,
      scope: this,
      fetch: (page, size) => {
        if (!this.data.hospitalId) {
          return Promise.resolve({ data: [], total: 0, hasMore: false });
        }
        const url = app.globalData.baseUrl + '/api/hospital-articles/page?hospitalId=' +
          encodeURIComponent(this.data.hospitalId) + '&page=' + page + '&size=' + size;
        return new Promise((resolve, reject) => {
          wx.request({
            url,
            method: 'GET',
            success: res => {
              if (res.statusCode >= 200 && res.statusCode < 300) {
                resolve(res.data || {});
              } else {
                reject(new Error('HTTP ' + res.statusCode));
              }
            },
            fail: reject
          });
        });
      },
      onReset: () => {
        this.setData({ articles: [] });
      },
      onData: state => {
        const articles = (state.list || []).map(item => Object.assign({}, item, {
          coverFull: item.coverUrl ? app.globalData.baseUrl + item.coverUrl : '',
          categoryText: item.category || '院内动态',
          dateText: String(item.publishedAt || item.createTime || '').replace('T', ' ').slice(0, 10),
          brief: item.summary || String(item.content || '').replace(/\s+/g, ' ').slice(0, 90)
        }));
        this.setData({ articles, pager: state.pager });
      }
    });
  },

  retryLoad() {
    if (this.pager) this.pager.retry();
  },

  openArticle(e) {
    const id = e.currentTarget.dataset.id;
    if (!id) return;
    wx.navigateTo({ url: '/pages/article-detail/article-detail?id=' + id });
  },

  goHospitalList() {
    wx.navigateTo({ url: '/pages/hospital-list/hospital-list' });
  },

  goHelp() {
    wx.navigateTo({ url: '/pages/help/help' });
  }
});
