const app = getApp();
const elderly = require('../../utils/elderly');
const placeholder = require('../../utils/placeholder');
const pager = require('../../utils/pager');

Page({
  data: {
    departments: [],
    currentHospital: null,
    hospitalId: null,
    elderly: false,
    pager: { status: 'idle', hasMore: true, total: 0, error: '', loading: false },
    isLoading: true
  },

  onLoad(options) {
    this.setData({
      elderly: elderly.isElderly(),
      hospitalId: options.hospitalId || null
    });

    if (!options.hospitalId) {
      wx.showToast({ title: '未指定医院', icon: 'none' });
      return;
    }

    this.createPager();
    this.loadHospital(options.hospitalId);
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
      fetch: (page, size) => new Promise((resolve, reject) => {
        wx.request({
          url: app.globalData.baseUrl + '/api/departments/hospital/' + this.data.hospitalId +
            '/page?bookable=true&page=' + page + '&size=' + size,
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
      }),
      onReset: () => {
        this.setData({ departments: [], isLoading: true });
      },
      onData: state => {
        let lastGroup = '';
        const list = (state.list || []).map(dept => {
          const extra = placeholder.departmentExtra(dept);
          const groupHeader = extra.group !== lastGroup;
          lastGroup = extra.group;
          return {
            ...dept,
            ...extra,
            initial: String(dept.deptName || '科').slice(0, 1),
            groupHeader: groupHeader
          };
        });
        this.setData({
          departments: list,
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

  viewDept(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/department-detail/department-detail?id=${id}`
    });
  },

  viewDoctorList(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/doctor/doctor?deptId=${id}&hospitalId=${this.data.hospitalId}`
    });
  },

  goHospitalSelect() {
    wx.navigateTo({ url: '/pages/hospital-list/hospital-list' });
  }
});
