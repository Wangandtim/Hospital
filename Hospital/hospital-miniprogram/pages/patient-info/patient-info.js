const app = getApp();
const placeholder = require('../../utils/placeholder');

Page({
  data: {
    patient: {
      name: '',
      gender: '',
      age: '',
      phone: '',
      medicalHistory: ''
    },
    initial: '就',
    verified: true,
    fromUrl: '',
    mode: 'edit',
    isCreate: false,
    familyId: '',
    canDelete: false
  },

  onLoad(options) {
    const mode = options.mode || 'edit';
    const isCreate = mode === 'create';
    const familyId = !isCreate && options.id && options.id !== 'SELF' ? options.id : '';
    this.setData({
      fromUrl: options.fromUrl || '',
      mode: isCreate ? 'edit' : mode,
      isCreate,
      familyId,
      canDelete: !!familyId
    });

    if (isCreate) {
      return;
    }

    const patientId = wx.getStorageSync('patientId');
    const patientPhone = wx.getStorageSync('patientPhone');
    const patientName = wx.getStorageSync('patientName');

    this.setData({
      'patient.phone': patientPhone || '',
      'patient.name': patientName || ''
    });

    if (this.data.familyId) {
      this.loadFamilyPatient(this.data.familyId);
    } else if (patientId) {
      this.loadPatientInfo(patientId);
    }
  },

  loadFamilyPatient(familyId) {
    const token = wx.getStorageSync('authToken');
    const patientId = wx.getStorageSync('patientId') || '';
    let url = app.globalData.baseUrl + '/api/patients/page?page=0&size=20';
    if (patientId) {
      url += '&patientId=' + encodeURIComponent(patientId);
    }

    wx.request({
      url,
      method: 'GET',
      header: token ? { 'X-Auth-Token': token } : {},
      success: (res) => {
        const list = (res.data && res.data.data) || [];
        const hit = list.find(item => item.patientId === familyId);
        if (hit) {
          this.setData({
            patient: {
              name: hit.name || '',
              gender: this.normalizeGender(hit.gender),
              age: String(hit.age || ''),
              phone: hit.phone || '',
              medicalHistory: hit.medicalHistory || ''
            },
            initial: String(hit.name || '就').slice(0, 1)
          });
        } else {
          this.loadFamilyFromPlaceholder(familyId);
        }
      },
      fail: () => this.loadFamilyFromPlaceholder(familyId)
    });
  },

  loadFamilyFromPlaceholder(familyId) {
    const patientInfo = wx.getStorageSync('patientInfo') || null;
    const primary = Object.assign({}, patientInfo || {}, { patientName: wx.getStorageSync('patientName') || (patientInfo && patientInfo.patientName) || '' });
    const list = placeholder.familyPatients(primary);
    const hit = list.find(item => item.patientId === familyId);
    if (!hit) return;
    this.setData({
      patient: {
        name: hit.name || '',
        gender: hit.gender === '女' ? 'F' : 'M',
        age: String(hit.age || ''),
        phone: (patientInfo && patientInfo.phone) || '',
        medicalHistory: hit.medicalHistory || ''
      },
      initial: String(hit.name || '就').slice(0, 1)
    });
  },

  loadPatientInfo(patientId) {
    const token = wx.getStorageSync('authToken');
    wx.request({
      url: app.globalData.baseUrl + '/api/patients/' + patientId,
      method: 'GET',
      header: token ? { 'X-Auth-Token': token } : {},
      success: (res) => {
        if (res.data) {
          const patient = res.data;
          this.setData({
            patient: {
              name: patient.name || '',
              gender: this.normalizeGender(patient.gender),
              age: patient.age || '',
              phone: patient.phone || '',
              medicalHistory: patient.medicalHistory || ''
            },
            initial: String(patient.name || '就').slice(0, 1)
          });
        }
      }
    });
  },

  normalizeGender(value) {
    if (value === 1 || value === 'M' || value === '男') return 'M';
    if (value === 2 || value === 'F' || value === '女') return 'F';
    return '';
  },

  onNameInput(e) {
    const value = e.detail.value;
    this.setData({ 'patient.name': value, initial: String(value || '就').slice(0, 1) });
  },

  onAgeInput(e) {
    this.setData({ 'patient.age': e.detail.value });
  },

  onGenderChange(e) {
    const gender = e.currentTarget.dataset.value;
    this.setData({ 'patient.gender': gender });
  },

  onMedicalHistoryInput(e) {
    this.setData({ 'patient.medicalHistory': e.detail.value });
  },

  submitForm() {
    const { patient } = this.data;

    if (!patient.name || !patient.name.trim()) {
      wx.showToast({ title: '请输入姓名', icon: 'none' });
      return;
    }
    if (!patient.gender) {
      wx.showToast({ title: '请选择性别', icon: 'none' });
      return;
    }
    if (!patient.age || parseInt(patient.age) <= 0) {
      wx.showToast({ title: '请输入有效的年龄', icon: 'none' });
      return;
    }

    if (this.data.isCreate || this.data.familyId) {
      this.saveFamilyPatient();
      return;
    }

    wx.showLoading({ title: '保存中...' });

    const patientId = wx.getStorageSync('patientId');
    const token = wx.getStorageSync('authToken');

    wx.request({
      url: app.globalData.baseUrl + '/api/patients/' + patientId,
      method: 'PUT',
      header: token ? { 'X-Auth-Token': token } : {},
      data: {
        name: patient.name,
        gender: patient.gender,
        age: parseInt(patient.age),
        phone: patient.phone,
        medicalHistory: patient.medicalHistory
      },
      success: (res) => {
        wx.hideLoading();
        if (res.statusCode === 200) {
          wx.setStorageSync('patientName', this.data.patient.name);
          wx.setStorageSync('hasCompleteInfo', true);
          app.globalData.hasPatientInfo = true;
          wx.showToast({ title: '保存成功', icon: 'success' });
          this.afterSave();
        } else {
          wx.showToast({ title: '保存失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '网络错误', icon: 'none' });
      }
    });
  },

  saveFamilyPatient() {
    const { patient, familyId, isCreate } = this.data;
    const owner = wx.getStorageSync('patientId') || '';
    const token = wx.getStorageSync('authToken');
    const query = owner ? '?ownerPatientId=' + encodeURIComponent(owner) : '';
    const url = app.globalData.baseUrl + '/api/patients/family' +
      (isCreate ? '' : '/' + encodeURIComponent(familyId)) + query;

    wx.showLoading({ title: '保存中...' });

    wx.request({
      url,
      method: isCreate ? 'POST' : 'PUT',
      header: Object.assign({ 'Content-Type': 'application/json' }, token ? { 'X-Auth-Token': token } : {}),
      data: {
        name: patient.name.trim(),
        gender: patient.gender === 'F' ? '女' : '男',
        age: parseInt(patient.age)
      },
      success: (res) => {
        wx.hideLoading();
        const body = res.data || {};
        if (res.statusCode >= 200 && res.statusCode < 300 && body.success !== false) {
          wx.showToast({ title: isCreate ? '已添加' : '已保存', icon: 'success' });
          this.afterSave();
        } else {
          wx.showToast({ title: body.message || '家庭成员不存在或不属于当前账号', icon: 'none' });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '网络错误', icon: 'none' });
      }
    });
  },

  deletePatient() {
    const familyId = this.data.familyId;
    if (!familyId) return;

    wx.showModal({
      title: '删除就诊人',
      content: '删除后该成员将不可用于挂号，需重新添加。',
      confirmText: '删除',
      confirmColor: '#A64842',
      success: (res) => {
        if (!res.confirm) return;
        this.doDeleteFamily(familyId);
      }
    });
  },

  doDeleteFamily(familyId) {
    const owner = wx.getStorageSync('patientId') || '';
    const token = wx.getStorageSync('authToken');
    const query = owner ? '?ownerPatientId=' + encodeURIComponent(owner) : '';

    wx.showLoading({ title: '删除中...' });
    wx.request({
      url: app.globalData.baseUrl + '/api/patients/family/' + encodeURIComponent(familyId) + query,
      method: 'DELETE',
      header: token ? { 'X-Auth-Token': token } : {},
      success: (res) => {
        wx.hideLoading();
        const body = res.data || {};
        if (res.statusCode >= 200 && res.statusCode < 300 && body.success !== false) {
          wx.showToast({ title: '已删除', icon: 'success' });
          setTimeout(() => wx.navigateBack(), 1200);
        } else {
          wx.showToast({ title: body.message || '家庭成员不存在或不属于当前账号', icon: 'none' });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '网络错误', icon: 'none' });
      }
    });
  },

  afterSave() {
    setTimeout(() => {
      if (this.data.fromUrl) {
        wx.redirectTo({ url: decodeURIComponent(this.data.fromUrl) });
      } else {
        wx.navigateBack();
      }
    }, 1200);
  },

  skip() {
    if (this.data.isCreate) {
      wx.navigateBack();
      return;
    }
    if (this.data.fromUrl) {
      wx.redirectTo({ url: decodeURIComponent(this.data.fromUrl) });
    } else {
      wx.navigateBack();
    }
  },

  goBack() {
    wx.navigateBack();
  },

  goToEdit() {
    wx.redirectTo({ url: '/pages/patient-info/patient-info?mode=edit&id=' + this.data.familyId });
  }
});
