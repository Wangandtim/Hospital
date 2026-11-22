const pager = require('../../utils/pager');

function apiBase() {
  return getApp().globalData.baseUrl;
}

function genderText(value) {
  if (value === 1 || value === 'M' || value === '男') return '男';
  if (value === 2 || value === 'F' || value === '女') return '女';
  return '未知';
}

Page({
  data: {
    patients: [],
    total: 0,
    pager: { status: 'idle', hasMore: true, total: 0, error: '', loading: false }
  },

  onShow() {
    this.loadPatients();
  },

  onUnload() {
    if (this.pager) this.pager.destroy();
  },

  onReachBottom() {
    if (this.pager) this.pager.next();
  },

  loadPatients() {
    if (this.pager) {
      this.pager.destroy();
    }

    this.pager = pager.create({
      size: 20,
      scope: this,
      fetch: (page, size) => {
        const token = wx.getStorageSync('authToken');
        const patientId = wx.getStorageSync('patientId') || '';
        let url = apiBase() + '/api/patients/page?page=' + page + '&size=' + size;
        if (patientId) {
          url += '&patientId=' + encodeURIComponent(patientId);
        }
        return new Promise((resolve, reject) => {
          wx.request({
            url,
            method: 'GET',
            header: token ? { 'X-Auth-Token': token } : {},
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
          return res;
        });
      },
      onReset: () => {
        this.setData({ patients: [] });
      },
      onData: state => {
        const patients = (state.list || []).map(item => {
          const tags = [];
          if (item.isDefault) tags.push('默认就诊人');
          return Object.assign({}, item, {
            initial: String(item.name || '就').slice(0, 1),
            genderText: genderText(item.gender),
            idMasked: item.idCard || '',
            tags
          });
        });
        this.setData({
          patients,
          total: state.pager.total || 0,
          pager: state.pager
        });
      }
    });
    this.pager.reset();
  },

  retryLoad() {
    if (this.pager) this.pager.retry();
  },

  editPatient(e) {
    const id = e.currentTarget.dataset.id;
    const selfId = wx.getStorageSync('patientId');
    if (id === selfId) {
      wx.navigateTo({ url: '/pages/patient-info/patient-info?mode=edit' });
      return;
    }
    wx.navigateTo({ url: '/pages/patient-info/patient-info?mode=edit&id=' + (id || '') });
  },

  addPatient() {
    wx.navigateTo({ url: '/pages/patient-info/patient-info?mode=create' });
  },

  goHelp() {
    wx.navigateTo({ url: '/pages/help/help' });
  }
});
