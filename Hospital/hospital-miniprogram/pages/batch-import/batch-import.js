const app = getApp();
const deptIcon = require('../../utils/dept-icon');

const DOCTOR_FIELDS = 9;
const ADMIN_FIELDS = 5;

Page({
  data: {
    mode: 'doctors',
    isSuper: false,
    hospitalId: '',
    hospitalName: '',
    rawText: '',
    previewRows: [],
    previewCount: 0,
    importing: false,
    result: null,
    errorItems: []
  },

  onLoad(options) {
    const adminRole = wx.getStorageSync('adminRole');
    const isSuper = adminRole === 'SUPER_ADMIN';
    // 总管理员只导入医院管理员；医生导入仅限院管
    const mode = isSuper ? 'admins' : 'doctors';
    this.setData({
      isSuper: isSuper,
      mode: mode,
      hospitalId: wx.getStorageSync('hospitalId') || '',
      hospitalName: wx.getStorageSync('hospitalName') || ''
    });
    this.rows = [];
  },

  onInput(e) {
    this.setData({ rawText: e.detail.value });
  },

  copyTemplate() {
    const template = this.data.mode === 'doctors'
      ? '登录账号,姓名,性别,职称,科室,手机号,擅长领域,挂号费,密码\nzhangyi01,张医生,男,主任医师,内科,13800000001,心血管内科,30,123456'
      : '登录账号,姓名,手机号,密码,医院\nhospitaladmin01,李管理员,13800000002,123456,西安市第一医院';
    wx.setClipboardData({
      data: template,
      success: () => wx.showToast({ title: '模板已复制', icon: 'success' })
    });
  },

  parseRows() {
    const text = String(this.data.rawText || '').trim();
    if (!text) {
      wx.showToast({ title: '请先粘贴数据', icon: 'none' });
      return null;
    }

    const expected = this.data.mode === 'doctors' ? DOCTOR_FIELDS : ADMIN_FIELDS;
    const lines = text.split(/\r?\n/).map(line => line.trim()).filter(line => line.length > 0);
    let start = 0;
    if (/登录账号|账号|username/i.test(lines[0])) {
      start = 1;
    }

    if (lines.length - start > 500) {
      wx.showToast({ title: '单次最多 500 条', icon: 'none' });
      return null;
    }

    const rows = [];
    for (let i = start; i < lines.length; i++) {
      const cells = lines[i].split(/[,，\t]/).map(cell => cell.trim());
      if (cells.length === 1 && cells[0] === '') {
        continue;
      }
      if (cells.length > expected) {
        wx.showToast({ title: '第 ' + (i - start + 1) + ' 行列数过多', icon: 'none' });
        return null;
      }
      if (this.data.mode === 'doctors') {
        rows.push({
          username: cells[0] || '',
          name: cells[1] || '',
          gender: cells[2] || '',
          title: cells[3] || '',
          dept: cells[4] || '',
          phone: cells[5] || '',
          expertise: cells[6] || '',
          fee: cells[7] || '',
          password: cells[8] || ''
        });
      } else {
        rows.push({
          username: cells[0] || '',
          name: cells[1] || '',
          phone: cells[2] || '',
          password: cells[3] || '',
          hospital: cells[4] || ''
        });
      }
    }

    if (rows.length === 0) {
      wx.showToast({ title: '没有解析到有效数据', icon: 'none' });
      return null;
    }

    this.rows = rows;
    this.setData({
      previewRows: rows.slice(0, 5).map(row => ({
        ...row,
        deptIcon: deptIcon.iconPath(row.dept || '')
      })),
      previewCount: rows.length,
      result: null,
      errorItems: []
    });
    return rows;
  },

  preview() {
    this.parseRows();
  },

  submit() {
    if (this.data.importing) return;

    let rows = this.rows;
    if (!rows || rows.length === 0) {
      rows = this.parseRows();
      if (!rows) return;
    }

    if (this.data.mode === 'doctors' && !this.data.hospitalId) {
      wx.showToast({ title: '未获取到医院信息，请重新登录', icon: 'none' });
      return;
    }

    const url = app.globalData.baseUrl + (this.data.mode === 'doctors'
      ? '/api/batch-import/doctors'
      : '/api/batch-import/admins');

    const payload = { rows: rows };
    if (this.data.mode === 'doctors') {
      payload.hospitalId = this.data.hospitalId;
    }

    this.setData({ importing: true });
    wx.showLoading({ title: '导入中...' });

    wx.request({
      url: url,
      method: 'POST',
      header: { 'content-type': 'application/json' },
      data: payload,
      success: (res) => {
        wx.hideLoading();
        const data = res.data || {};
        if (res.statusCode === 401) {
          this.setData({ importing: false });
          wx.showToast({ title: '登录已失效，请重新登录', icon: 'none' });
          return;
        }
        if (!data.success) {
          this.setData({ importing: false, result: null });
          wx.showToast({ title: data.message || '导入失败', icon: 'none' });
          return;
        }

        this.setData({
          importing: false,
          result: data,
          errorItems: data.errors || []
        });
        wx.showToast({
          title: '成功导入 ' + (data.successCount || 0) + ' 条',
          icon: data.failCount ? 'none' : 'success'
        });

        if ((data.failCount || 0) === 0) {
          this.setData({ rawText: '', previewRows: [], previewCount: 0 });
          this.rows = [];
        }
      },
      fail: () => {
        wx.hideLoading();
        this.setData({ importing: false });
        wx.showToast({ title: '网络异常，请稍后重试', icon: 'none' });
      }
    });
  }
});
