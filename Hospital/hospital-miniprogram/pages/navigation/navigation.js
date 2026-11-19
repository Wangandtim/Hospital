const app = getApp();
const placeholder = require('../../utils/placeholder');

// 接口演示数据带 MOCK- 前缀，展示前去掉
function cleanMockText(text) {
  return String(text || '').replace(/^MOCK-/, '');
}

Page({
  data: {
    hospitalId: '',
    nav: null,
    floors: [],
    activeIndex: 0,
    activeFloor: null,
    usingMock: false,
    loading: true
  },

  onLoad() {
    const selected = wx.getStorageSync('selectedHospital') || {};
    const hospitalId = selected.hospitalId || 'H001';
    this._selected = selected;
    this.setData({ hospitalId });
    this.loadNavigation(hospitalId);
  },

  loadNavigation(hospitalId) {
    wx.request({
      url: app.globalData.baseUrl + '/api/hospitals/' + hospitalId + '/navigation',
      method: 'GET',
      timeout: 5000,
      success: (res) => {
        const body = res.data || {};
        if (body.success && body.data && Array.isArray(body.data.floors) && body.data.floors.length > 0) {
          this.applyNavigation(body.data);
        } else {
          this.applyFallback();
        }
      },
      fail: () => this.applyFallback()
    });
  },

  applyNavigation(data) {
    const floors = data.floors || [];
    const route = data.route || {};
    const parking = data.parking || {};
    const nav = {
      hospitalName: data.hospitalName || (this._selected && this._selected.hospitalName) || '',
      address: data.address || '',
      guideDeskPhone: data.guideDeskPhone || '',
      route: {
        from: cleanMockText(route.from),
        to: cleanMockText(route.to),
        minutes: route.minutes || 0,
        steps: (route.steps || []).map(cleanMockText)
      },
      parking: {
        entrance: cleanMockText(parking.entrance),
        fee: cleanMockText(parking.fee)
      }
    };
    this.setData({
      nav,
      floors,
      activeIndex: 0,
      activeFloor: floors[0] || null,
      usingMock: Array.isArray(data._mock) && data._mock.length > 0,
      loading: false
    });
  },

  applyFallback() {
    const mock = placeholder.navigationMock(this._selected || {});
    const floors = mock.floors.map((floor, index) => ({
      floor: floor.key,
      name: floor.name,
      zones: index === 2
        ? mock.zones.map(zone => ({
            deptName: zone.name,
            area: '',
            room: '',
            waitArea: zone.name + ' 候诊区'
          }))
        : []
    }));
    const route = {
      from: '门诊 1 层大厅',
      to: mock.floors[2] ? mock.floors[2].name : '对应诊区',
      minutes: mock.minutes,
      steps: mock.steps.map(step => step.title + '：' + step.desc)
    };
    const parking = {
      entrance: mock.services[2] ? mock.services[2].name + '（' + mock.services[2].detail + '）' : '',
      fee: ''
    };
    this.setData({
      nav: {
        hospitalName: mock.hospitalName,
        address: '',
        guideDeskPhone: '029-8763 0000',
        route,
        parking
      },
      floors,
      activeIndex: 2,
      activeFloor: floors[2] || floors[0],
      usingMock: true,
      loading: false
    });
  },

  selectFloor(e) {
    const index = Number(e.currentTarget.dataset.index);
    this.setData({
      activeIndex: index,
      activeFloor: this.data.floors[index]
    });
  },

  callGuide() {
    const phone = (this.data.nav && this.data.nav.guideDeskPhone) || '';
    if (!phone) {
      wx.showToast({ title: '暂无导医台电话', icon: 'none' });
      return;
    }
    wx.makePhoneCall({ phoneNumber: String(phone).replace(/[^0-9]/g, ''), fail: () => {} });
  },

  goBack() {
    wx.navigateBack();
  },

  goHelp() {
    wx.navigateTo({ url: '/pages/help/help' });
  },

  goEmergency() {
    wx.navigateTo({ url: '/pages/triage-emergency/triage-emergency' });
  }
});
