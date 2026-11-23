const app = getApp();
const deptIcon = require('../../utils/dept-icon');

const PAGE_SIZE = 20;

Page({
  data: {
    hospitalId: '',
    hospitalName: '',
    departments: [],
    keyword: '',
    page: 0,
    totalPages: 0,
    total: 0,
    loading: true,
    error: ''
  },

  onLoad(options) {
    this._seq = 0;
    if (options.hospitalId) {
      this.setData({ hospitalId: options.hospitalId });
    }
    this.loadHospitalName();
    this.loadPage(0);
  },

  onShow() {
    // 编辑保存返回后刷新当前页
    if (this._loaded) {
      this.loadPage(this.data.page);
    }
    this._loaded = true;
  },

  onUnload() {
    this._seq += 1;
    if (this._keywordTimer) clearTimeout(this._keywordTimer);
  },

  loadHospitalName() {
    wx.request({
      url: app.globalData.baseUrl + '/api/hospitals/' + this.data.hospitalId,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.hospitalName) {
          this.setData({ hospitalName: res.data.hospitalName });
        }
      }
    });
  },

  loadPage(page) {
    const keyword = (this.data.keyword || '').trim();
    const seq = ++this._seq;
    let url = app.globalData.baseUrl + '/api/departments/hospital/' +
      encodeURIComponent(this.data.hospitalId) + '/page?page=' + page + '&size=' + PAGE_SIZE;
    if (keyword) {
      url += '&keyword=' + encodeURIComponent(keyword);
    }

    this.setData({ loading: true, error: '' });

    wx.request({
      url: url,
      method: 'GET',
      success: (res) => {
        if (seq !== this._seq) return;
        if (res.statusCode < 200 || res.statusCode >= 300) {
          this.setData({ loading: false, error: 'HTTP ' + res.statusCode });
          return;
        }
        const body = res.data || {};
        const content = body.content || body.data || [];
        const total = Number(body.total !== undefined ? body.total : content.length);
        const totalPages = Number(body.totalPages !== undefined ? body.totalPages : 1);
        const list = content.map(dept => ({
          ...dept,
          deptIcon: deptIcon.iconPath(dept.deptName)
        }));
        this.setData({
          departments: list,
          page: page,
          totalPages: totalPages,
          total: total,
          loading: false,
          error: ''
        });
      },
      fail: (err) => {
        if (seq !== this._seq) return;
        this.setData({
          loading: false,
          error: (err && err.errMsg) || '加载失败，请重试'
        });
      }
    });
  },

  onSearchInput(e) {
    this.setData({ keyword: e.detail.value });
    if (this._keywordTimer) clearTimeout(this._keywordTimer);
    this._keywordTimer = setTimeout(() => this.loadPage(0), 400);
  },

  clearKeyword() {
    if (this._keywordTimer) clearTimeout(this._keywordTimer);
    this.setData({ keyword: '' });
    this.loadPage(0);
  },

  prevPage() {
    if (this.data.page > 0) {
      this.loadPage(this.data.page - 1);
    }
  },

  nextPage() {
    if (this.data.page + 1 < this.data.totalPages) {
      this.loadPage(this.data.page + 1);
    }
  },

  retryLoad() {
    this.loadPage(this.data.page);
  },

  goToDeptDetail(e) {
    const deptId = e.currentTarget.dataset.deptid;
    if (!deptId) return;
    wx.navigateTo({
      url: `/pages/department-detail/department-detail?id=${deptId}`
    });
  },

  editDept(e) {
    const deptId = e.currentTarget.dataset.deptid;
    if (!deptId) return;
    wx.navigateTo({
      url: `/pages/dept-add/dept-add?deptId=${deptId}&hospitalId=${this.data.hospitalId}`
    });
  },

  goBack() {
    wx.navigateBack();
  }
});
