const app = getApp();
const placeholder = require('../../utils/placeholder');
const pager = require('../../utils/pager');
const deptIcon = require('../../utils/dept-icon');

const TYPE_TEXT = {
  hospital: '医院',
  department: '科室',
  doctor: '医生',
  article: '公告'
};

Page({
  data: {
    keyword: '',
    activeType: 'all',
    types: [
      { key: 'all', label: '全部', count: null },
      { key: 'hospital', label: '医院', count: null },
      { key: 'department', label: '科室', count: null },
      { key: 'doctor', label: '医生', count: null },
      { key: 'article', label: '公告', count: null }
    ],
    counts: { hospital: 0, department: 0, doctor: 0, article: 0 },
    countAll: 0,
    hotKeywords: placeholder.hotKeywords,
    history: [],
    results: [],
    hasSearched: false,
    pager: { status: 'idle', hasMore: true, total: 0, error: '', loading: false }
  },

  onLoad(options) {
    this.setData({ history: placeholder.getSearchHistory() });
    this.createPager();

    if (options.keyword) {
      const keyword = decodeURIComponent(options.keyword);
      this.setData({ keyword });
      this.doSearch();
    }
  },

  onUnload() {
    if (this.pager) {
      this.pager.destroy();
    }
  },

  onReachBottom() {
    if (this.pager && this.data.hasSearched) {
      this.pager.next();
    }
  },

  createPager() {
    this.pager = pager.create({
      size: 20,
      scope: this,
      fetch: (page, size) => {
        const keyword = this.data.keyword.trim();
        if (!keyword) {
          return Promise.resolve({ data: [], total: 0, hasMore: false });
        }
        let url = app.globalData.baseUrl + '/api/search?keyword=' + encodeURIComponent(keyword) +
          '&page=' + page + '&size=' + size + '&type=' + this.data.activeType;
        const hospitalId = wx.getStorageSync('selectedHospital');
        if (hospitalId && hospitalId.hospitalId) {
          url += '&hospitalId=' + encodeURIComponent(hospitalId.hospitalId);
        }
        return new Promise((resolve, reject) => {
          wx.request({
            url: url,
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
        }).then(res => {
          this.applyCounts(res && res.counts);
          return res;
        });
      },
      onReset: () => {
        this.setData({ results: [] });
      },
      onData: state => {
        const results = (state.list || []).map(item => Object.assign({}, item, {
          typeText: TYPE_TEXT[item.type] || '结果',
          key: item.type + '-' + item.id,
          deptIcon: item.type === 'department' ? deptIcon.iconPath(item.title) : ''
        }));
        this.setData({ results, pager: state.pager });
      }
    });
  },

  applyCounts(counts) {
    if (!counts) return;
    const next = {
      hospital: Number(counts.hospital) || 0,
      department: Number(counts.department) || 0,
      doctor: Number(counts.doctor) || 0,
      article: Number(counts.article) || 0
    };
    const countAll = next.hospital + next.department + next.doctor + next.article;
    const types = this.data.types.map(item => Object.assign({}, item, {
      count: item.key === 'all' ? countAll : next[item.key]
    }));
    this.setData({ counts: next, countAll, types });
  },

  onInput(e) {
    this.setData({ keyword: e.detail.value });
  },

  clearKeyword() {
    const types = this.data.types.map(item => Object.assign({}, item, { count: null }));
    this.setData({ keyword: '', hasSearched: false, results: [], countAll: 0, types });
  },

  onSearch() {
    this.doSearch();
  },

  doSearch() {
    const keyword = this.data.keyword.trim();
    if (!keyword) {
      wx.showToast({ title: '请输入搜索关键词', icon: 'none' });
      return;
    }
    const history = placeholder.saveSearchHistory(keyword);
    this.setData({ history, hasSearched: true, activeType: 'all' });
    this.pager.reset();
  },

  useKeyword(e) {
    const keyword = e.currentTarget.dataset.keyword;
    this.setData({ keyword });
    this.doSearch();
  },

  clearHistory() {
    const history = placeholder.clearSearchHistory();
    this.setData({ history });
    wx.showToast({ title: '已清空搜索历史', icon: 'none' });
  },

  switchType(e) {
    const key = e.currentTarget.dataset.key;
    if (key === this.data.activeType) return;
    this.setData({ activeType: key });
    if (this.data.hasSearched) {
      this.pager.reset();
    }
  },

  retryLoad() {
    if (this.pager) {
      this.pager.retry();
    }
  },

  openResult(e) {
    const item = e.currentTarget.dataset.item;
    if (!item) return;
    if (item.type === 'hospital') {
      wx.navigateTo({ url: '/pages/department/department?hospitalId=' + item.id });
    } else if (item.type === 'department') {
      wx.navigateTo({ url: '/pages/department-detail/department-detail?id=' + item.id });
    } else if (item.type === 'doctor') {
      wx.navigateTo({ url: '/pages/doctor-detail/doctor-detail?id=' + item.id });
    } else if (item.type === 'article') {
      wx.navigateTo({ url: '/pages/article-detail/article-detail?id=' + item.id });
    }
  },

  goTriage() {
    wx.navigateTo({ url: '/pages/triage/triage' });
  }
});
