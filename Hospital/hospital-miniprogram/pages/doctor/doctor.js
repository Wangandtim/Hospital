const app = getApp();
const placeholder = require('../../utils/placeholder');
const pager = require('../../utils/pager');

function resolveMedia(url) {
  const text = String(url || '');
  if (!text) return '';
  if (text.indexOf('http://') === 0 || text.indexOf('https://') === 0 || text.indexOf('wxfile://') === 0) {
    return text;
  }
  return app.globalData.baseUrl + (text.charAt(0) === '/' ? text : '/' + text);
}

Page({
  data: {
    doctors: [],
    deptId: '',
    hospitalId: '',
    currentHospital: null,
    pager: { status: 'idle', hasMore: true, total: 0, error: '', loading: false },
    isLoading: true
  },

  onLoad(options) {
    const hospitalId = options.hospitalId || '';
    const deptId = options.deptId || '';
    this.setData({ hospitalId: hospitalId, deptId: deptId });

    if (!hospitalId && !deptId) {
      wx.showToast({ title: '未指定医院或科室', icon: 'none' });
      return;
    }

    this.createPager();
    if (hospitalId) {
      this.loadHospital(hospitalId);
    }
    this.reload();
  },

  onUnload() {
    if (this.pager) {
      this.pager.destroy();
    }
  },

  onReachBottom() {
    if (this.pager) {
      this.pager.next();
    }
  },

  createPager() {
    this.pager = pager.create({
      size: 20,
      scope: this,
      fetch: (page, size) => {
        const base = this.data.hospitalId
          ? '/api/doctors/hospital/' + this.data.hospitalId
          : '/api/doctors/dept/' + this.data.deptId;
        return new Promise((resolve, reject) => {
          wx.request({
            url: app.globalData.baseUrl + base + '/page?page=' + page + '&size=' + size,
            method: 'GET',
            success: res => {
              if (res.statusCode >= 200 && res.statusCode < 300) {
                resolve(res.data);
              } else {
                reject(new Error('HTTP ' + res.statusCode));
              }
            },
            fail: reject
          });
        });
      },
      onReset: () => {
        this.setData({ doctors: [], isLoading: true });
      },
      onData: state => {
        const doctors = (state.list || []).map(doctor => {
          const extra = placeholder.doctorExtra(doctor);
          const remaining = Number(doctor.todayRemaining);
          return {
            ...doctor,
            ...extra,
            avatarText: String(doctor.name || '医').slice(0, 1),
            photoUrlFull: resolveMedia(doctor.photoUrl),
            available: remaining === 0 ? false : true
          };
        });
        this.setData({
          doctors: doctors,
          pager: state.pager,
          isLoading: state.pager.status === 'loading'
        });
      }
    });
  },

  reload() {
    if (this.pager) {
      this.pager.reset();
    }
  },

  retryLoad() {
    if (this.pager) {
      this.pager.retry();
    }
  },

  loadHospital(hospitalId) {
    wx.request({
      url: app.globalData.baseUrl + '/api/hospitals/' + hospitalId,
      method: 'GET',
      success: (res) => {
        if (res.data) {
          this.setData({
            currentHospital: {
              ...res.data,
              ...placeholder.hospitalExtra(res.data)
            }
          });
        }
      }
    });
  },

  makeAppointment(e) {
    const doctorId = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/appointment/appointment?doctorId=${doctorId}&hospitalId=${this.data.hospitalId}`
    });
  },

  goBack() {
    wx.navigateBack();
  },

  viewDoctor(e) {
    const doctorId = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/doctor-detail/doctor-detail?id=${doctorId}`
    });
  }
});
