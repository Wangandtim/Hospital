const app = getApp();

function resolveMedia(url) {
  const text = String(url || '');
  if (!text) return '';
  if (text.indexOf('http://') === 0 || text.indexOf('https://') === 0 || text.indexOf('wxfile://') === 0) {
    return text;
  }
  return app.globalData.baseUrl + (text.charAt(0) === '/' ? text : '/' + text);
}

function parseList(data) {
  if (Array.isArray(data)) return data;
  if (data && Array.isArray(data.data)) return data.data;
  if (data && Array.isArray(data.list)) return data.list;
  return [];
}

Page({
  data: {
    doctorId: '',
    doctorName: '',
    loading: true,
    saving: false,
    photoUrl: '',
    photoDisplay: '',
    form: {
      introduction: '',
      expertiseDesc: '',
      workExperience: '',
      education: '',
      academicAppointments: '',
      qualification: '',
      consultationFee: ''
    },
    // 页面不展示，但 PUT 会覆盖全部字段，保存时必须原样回传
    awards: '',
    certificates: [],
    certFormVisible: false,
    certEditing: false,
    certSaving: false,
    certForm: {
      certificateId: '',
      title: '',
      issuer: '',
      issuedDate: '',
      imageUrl: ''
    },
    certImageDisplay: ''
  },

  onLoad(options) {
    const doctorId = options.doctorId || '';
    if (!doctorId) {
      wx.showToast({ title: '缺少医生信息', icon: 'none' });
      return;
    }
    this.setData({ doctorId });
    this.loadDetail();
    this.loadCertificates();
  },

  authHeader() {
    const token = wx.getStorageSync('token') || wx.getStorageSync('authToken');
    return token ? { 'X-Auth-Token': token } : {};
  },

  loadDetail() {
    wx.showLoading({ title: '加载中...' });
    wx.request({
      url: app.globalData.baseUrl + '/api/doctors/' + this.data.doctorId + '/detail',
      method: 'GET',
      header: this.authHeader(),
      success: (res) => {
        wx.hideLoading();
        if (res.statusCode === 401 || res.statusCode === 403 || (res.data && res.data.success === false)) {
          wx.showToast({ title: (res.data && res.data.message) || '无权限查看', icon: 'none' });
          setTimeout(() => wx.navigateBack(), 1200);
          return;
        }
        const data = res.data || {};
        const photoUrl = data.photoUrl || '';
        this.setData({
          loading: false,
          photoUrl,
          photoDisplay: resolveMedia(photoUrl),
          awards: data.awards || '',
          form: {
            introduction: data.introduction || '',
            expertiseDesc: data.expertiseDesc || '',
            workExperience: data.workExperience || '',
            education: data.education || '',
            academicAppointments: data.academicAppointments || '',
            qualification: data.qualification || '',
            consultationFee: data.consultationFee === null || data.consultationFee === undefined
              ? ''
              : String(data.consultationFee)
          }
        });
      },
      fail: () => {
        wx.hideLoading();
        this.setData({ loading: false });
        wx.showToast({ title: '加载失败，请稍后重试', icon: 'none' });
      }
    });
  },

  loadCertificates() {
    wx.request({
      url: app.globalData.baseUrl + '/api/doctors/' + this.data.doctorId + '/certificates',
      method: 'GET',
      header: this.authHeader(),
      success: (res) => {
        const list = parseList(res.data).map(item => ({
          ...item,
          imageUrlFull: resolveMedia(item.imageUrl)
        }));
        this.setData({ certificates: list });
      }
    });
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ ['form.' + field]: e.detail.value });
  },

  choosePhoto() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sizeType: ['compressed'],
      success: (res) => {
        const file = res.tempFiles && res.tempFiles[0];
        if (file) this.uploadImage(file.tempFilePath, 'photo');
      }
    });
  },

  chooseCertImage() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sizeType: ['compressed'],
      success: (res) => {
        const file = res.tempFiles && res.tempFiles[0];
        if (file) this.uploadImage(file.tempFilePath, 'cert');
      }
    });
  },

  uploadImage(tempFilePath, target) {
    wx.showLoading({ title: '上传中...' });
    wx.uploadFile({
      url: app.globalData.baseUrl + '/api/upload',
      filePath: tempFilePath,
      name: 'file',
      header: this.authHeader(),
      success: (res) => {
        wx.hideLoading();
        let body = {};
        try {
          body = JSON.parse(res.data || '{}');
        } catch (e) {
          body = {};
        }
        if (!body.url) {
          wx.showToast({ title: body.message || '上传失败', icon: 'none' });
          return;
        }
        if (target === 'photo') {
          this.setData({ photoUrl: body.url, photoDisplay: resolveMedia(body.url) });
        } else {
          this.setData({
            certForm: { ...this.data.certForm, imageUrl: body.url },
            certImageDisplay: resolveMedia(body.url)
          });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '上传失败，请重试', icon: 'none' });
      }
    });
  },

  removePhoto() {
    this.setData({ photoUrl: '', photoDisplay: '' });
  },

  openCertForm() {
    this.setData({
      certFormVisible: true,
      certEditing: false,
      certForm: { certificateId: '', title: '', issuer: '', issuedDate: '', imageUrl: '' },
      certImageDisplay: ''
    });
  },

  closeCertForm() {
    this.setData({ certFormVisible: false, certSaving: false });
  },

  editCert(e) {
    const item = this.data.certificates[e.currentTarget.dataset.index];
    if (!item) return;
    this.setData({
      certFormVisible: true,
      certEditing: true,
      certForm: {
        certificateId: item.certificateId,
        title: item.title || '',
        issuer: item.issuer || '',
        issuedDate: item.issuedDate || '',
        imageUrl: item.imageUrl || ''
      },
      certImageDisplay: resolveMedia(item.imageUrl)
    });
  },

  onCertInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ ['certForm.' + field]: e.detail.value });
  },

  onCertDateChange(e) {
    this.setData({ 'certForm.issuedDate': e.detail.value });
  },

  saveCert() {
    if (this.data.certSaving) return;
    const form = this.data.certForm;
    if (!String(form.title || '').trim()) {
      wx.showToast({ title: '请填写证书名称', icon: 'none' });
      return;
    }
    const editing = this.data.certEditing && form.certificateId;
    const existing = editing
      ? this.data.certificates.find(c => c.certificateId === form.certificateId)
      : null;
    const payload = {
      title: String(form.title).trim(),
      issuer: form.issuer || '',
      issuedDate: form.issuedDate || null,
      imageUrl: form.imageUrl || '',
      sortOrder: existing && existing.sortOrder !== undefined && existing.sortOrder !== null
        ? Number(existing.sortOrder)
        : this.data.certificates.length + 1
    };
    this.setData({ certSaving: true });
    wx.request({
      url: app.globalData.baseUrl + '/api/doctors/' + this.data.doctorId + '/certificates' +
        (editing ? '/' + form.certificateId : ''),
      method: editing ? 'PUT' : 'POST',
      header: Object.assign({ 'Content-Type': 'application/json' }, this.authHeader()),
      data: payload,
      success: (res) => {
        this.setData({ certSaving: false });
        if (res.data && res.data.success === false) {
          wx.showToast({ title: res.data.message || '保存失败', icon: 'none' });
          return;
        }
        wx.showToast({ title: '已保存', icon: 'success' });
        this.setData({ certFormVisible: false });
        this.loadCertificates();
      },
      fail: () => {
        this.setData({ certSaving: false });
        wx.showToast({ title: '网络异常，请重试', icon: 'none' });
      }
    });
  },

  deleteCert(e) {
    const item = this.data.certificates[e.currentTarget.dataset.index];
    if (!item) return;
    wx.showModal({
      title: '删除证书',
      content: '删除后不可恢复',
      confirmColor: '#A64842',
      success: (res) => {
        if (!res.confirm) return;
        wx.request({
          url: app.globalData.baseUrl + '/api/doctors/' + this.data.doctorId + '/certificates/' + item.certificateId,
          method: 'DELETE',
          header: this.authHeader(),
          success: () => {
            wx.showToast({ title: '已删除', icon: 'success' });
            this.loadCertificates();
          }
        });
      }
    });
  },

  moveCert(e) {
    const index = Number(e.currentTarget.dataset.index);
    const delta = Number(e.currentTarget.dataset.delta);
    const list = this.data.certificates.slice();
    const target = index + delta;
    if (target < 0 || target >= list.length) return;
    const moved = list.splice(index, 1)[0];
    list.splice(target, 0, moved);
    const withOrder = list.map((item, i) => ({ ...item, sortOrder: i + 1 }));
    this.setData({ certificates: withOrder });
    wx.request({
      url: app.globalData.baseUrl + '/api/doctors/' + this.data.doctorId + '/certificates/sort',
      method: 'PUT',
      header: Object.assign({ 'Content-Type': 'application/json' }, this.authHeader()),
      data: { certificateIds: withOrder.map(item => item.certificateId) },
      success: () => {
        wx.showToast({ title: '排序已保存', icon: 'none' });
      },
      fail: () => {
        wx.showToast({ title: '排序保存失败', icon: 'none' });
        this.loadCertificates();
      }
    });
  },

  previewCert(e) {
    const index = Number(e.currentTarget.dataset.index);
    const item = this.data.certificates[index];
    if (!item || !item.imageUrlFull) return;
    const urls = this.data.certificates.map(c => c.imageUrlFull).filter(Boolean);
    wx.previewImage({ current: item.imageUrlFull, urls });
  },

  submit() {
    if (this.data.saving) {
      return;
    }
    const form = this.data.form;
    const fee = String(form.consultationFee || '').trim();
    if (fee && !/^\d+$/.test(fee)) {
      wx.showToast({ title: '挂号费请填写整数', icon: 'none' });
      return;
    }

    const payload = {
      introduction: form.introduction,
      expertiseDesc: form.expertiseDesc,
      workExperience: form.workExperience,
      education: form.education,
      academicAppointments: form.academicAppointments,
      qualification: form.qualification,
      consultationFee: fee,
      awards: this.data.awards,
      photoUrl: this.data.photoUrl
    };

    this.setData({ saving: true });
    wx.showLoading({ title: '保存中...' });
    wx.request({
      url: app.globalData.baseUrl + '/api/doctors/' + this.data.doctorId + '/detail',
      method: 'PUT',
      header: Object.assign({ 'Content-Type': 'application/json' }, this.authHeader()),
      data: payload,
      success: (res) => {
        wx.hideLoading();
        this.setData({ saving: false });
        if (res.data && res.data.success) {
          wx.showToast({ title: '已保存', icon: 'success' });
          setTimeout(() => wx.navigateBack(), 800);
        } else {
          wx.showToast({ title: (res.data && res.data.message) || '保存失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.hideLoading();
        this.setData({ saving: false });
        wx.showToast({ title: '网络异常，请稍后重试', icon: 'none' });
      }
    });
  },

  goBack() {
    wx.navigateBack();
  }
});
