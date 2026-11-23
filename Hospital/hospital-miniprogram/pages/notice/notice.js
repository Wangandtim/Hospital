const app = getApp();
const pager = require('../../utils/pager');

Page({
  data: {
    hospitalId: '',
    hospitalName: '',
    keyword: '',
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
    if (this._keywordTimer) clearTimeout(this._keywordTimer);
    if (this.pager) this.pager.destroy();
  },

  onReachBottom() {
    if (this.pager) this.pager.next();
  },

  fetchPage(page, size) {
    if (!this.data.hospitalId) {
      return Promise.resolve({ data: [], total: 0, hasMore: false });
    }
    const keyword = (this.data.keyword || '').trim();
    const url = app.globalData.baseUrl + '/api/hospital-articles/page?hospitalId=' +
      encodeURIComponent(this.data.hospitalId) + '&page=' + page + '&size=' + size +
      (keyword ? '&keyword=' + encodeURIComponent(keyword) : '');
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

  decorateArticles(list) {
    return (list || []).map(item => Object.assign({}, item, {
      coverFull: item.coverUrl ? app.globalData.baseUrl + item.coverUrl : '',
      categoryText: item.category || '院内动态',
      dateText: String(item.publishedAt || item.createTime || '').replace('T', ' ').slice(0, 10),
      brief: item.summary || String(item.content || '').replace(/\s+/g, ' ').slice(0, 90)
    }));
  },

  createPager() {
    this.pager = pager.create({
      size: 20,
      scope: this,
      fetch: (page, size) => this.fetchPage(page, size),
      onReset: () => {
        this.setData({ articles: [] });
      },
      onData: state => {
        this.setData({
          articles: this.decorateArticles(state.list),
          pager: state.pager
        });
      }
    });
  },

  onKeywordInput(e) {
    this.setData({ keyword: e.detail.value });
    if (this._keywordTimer) clearTimeout(this._keywordTimer);
    this._keywordTimer = setTimeout(() => this.pager.reset(), 400);
  },

  clearKeyword() {
    if (this._keywordTimer) clearTimeout(this._keywordTimer);
    this.setData({ keyword: '' });
    this.pager.reset();
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
