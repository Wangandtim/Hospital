const app = getApp();

Page({
  data: {
    hospitalId: '',
    baseUrl: app.globalData.baseUrl,
    articles: [],
    showForm: false,
    editingId: '',
    categories: ['通知', '文章', '推文'],
    categoryIndex: 0,
    uploading: false,
    noticeEnabled: false,
    noticeLoading: true,
    noticeSaving: false,
    form: {
      title: '',
      summary: '',
      content: '',
      coverUrl: '',
      category: '通知'
    }
  },

  onLoad(options) {
    const hospitalId = options.hospitalId || wx.getStorageSync('hospitalId') || app.globalData.hospitalId || '';
    this.setData({ hospitalId });
    this.loadArticles();
    this.loadNoticeSwitch();
  },

  onShow() {
    if (this.data.hospitalId) {
      this.loadArticles();
    }
  },

  loadArticles() {    if (!this.data.hospitalId) {
      wx.showToast({ title: '未获取到医院信息', icon: 'none' });
      return;
    }
    wx.showLoading({ title: '加载中...' });
    wx.request({
      url: app.globalData.baseUrl + '/api/hospital-articles/manage?hospitalId=' + this.data.hospitalId,
      method: 'GET',
      header: this.authHeader(),
      success: (res) => {
        wx.hideLoading();
        const list = Array.isArray(res.data) ? res.data : [];
        this.setData({
          articles: list.map(item => ({
            ...item,
            coverFull: item.coverUrl ? app.globalData.baseUrl + item.coverUrl : '',
            timeText: (item.createTime || '').replace('T', ' ').slice(0, 16)
          }))
        });
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '加载失败', icon: 'none' });
      }
    });
  },

  authHeader() {
    const token = wx.getStorageSync('token') || wx.getStorageSync('authToken');
    return token ? { 'X-Auth-Token': token } : {};
  },

  loadNoticeSwitch() {
    if (!this.data.hospitalId) {
      this.setData({ noticeLoading: false, noticeEnabled: false });
      return;
    }
    this.setData({ noticeLoading: true });
    wx.request({
      url: app.globalData.baseUrl + '/api/hospital-articles/notice-switch?hospitalId=' + encodeURIComponent(this.data.hospitalId),
      method: 'GET',
      header: this.authHeader(),
      success: (res) => {
        const body = res.data || {};
        if (res.statusCode >= 200 && res.statusCode < 300 && body.success !== false) {
          this.setData({ noticeEnabled: body.noticeEnabled === true, noticeLoading: false });
        } else {
          this.setData({ noticeEnabled: false, noticeLoading: false });
          wx.showToast({ title: body.message || '公告开关读取失败', icon: 'none' });
        }
      },
      fail: () => {
        // 读取失败时按默认关闭处理，页面仍可继续编辑公告
        this.setData({ noticeEnabled: false, noticeLoading: false });
        wx.showToast({ title: '公告开关读取失败', icon: 'none' });
      }
    });
  },

  onNoticeSwitchChange(e) {
    const enabled = !!e.detail.value;
    if (this.data.noticeSaving) {
      this.setData({ noticeEnabled: !enabled });
      return;
    }
    this.setData({ noticeEnabled: enabled, noticeSaving: true });
    wx.request({
      url: app.globalData.baseUrl + '/api/hospital-articles/notice-switch',
      method: 'PUT',
      header: Object.assign({ 'Content-Type': 'application/json' }, this.authHeader()),
      data: { hospitalId: this.data.hospitalId, enabled },
      success: (res) => {
        const body = res.data || {};
        if (res.statusCode >= 200 && res.statusCode < 300 && body.success !== false) {
          this.setData({
            noticeEnabled: body.noticeEnabled === true,
            noticeSaving: false
          });
          wx.showToast({ title: enabled ? '公告已开启' : '公告已关闭', icon: 'success' });
        } else {
          this.setData({ noticeEnabled: !enabled, noticeSaving: false });
          wx.showToast({ title: body.message || '开关更新失败', icon: 'none' });
        }
      },
      fail: () => {
        this.setData({ noticeEnabled: !enabled, noticeSaving: false });
        wx.showToast({ title: '网络异常，开关未保存', icon: 'none' });
      }
    });
  },

  openCreate() {
    this.setData({
      showForm: true,
      editingId: '',
      categoryIndex: 0,
      form: { title: '', summary: '', content: '', coverUrl: '', category: '通知' }
    });
  },

  openEdit(e) {
    const article = e.currentTarget.dataset.article;
    const idx = this.data.categories.indexOf(article.category);
    this.setData({
      showForm: true,
      editingId: article.articleId,
      categoryIndex: idx >= 0 ? idx : 1,
      form: {
        title: article.title || '',
        summary: article.summary || '',
        content: article.content || '',
        coverUrl: article.coverUrl || '',
        category: article.category || '文章'
      }
    });
  },

  closeForm() {
    this.setData({ showForm: false });
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [`form.${field}`]: e.detail.value });
  },

  onCategoryChange(e) {
    const index = Number(e.detail.value);
    this.setData({
      categoryIndex: index,
      'form.category': this.data.categories[index]
    });
  },

  chooseCover() {
    if (this.data.uploading) return;
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sizeType: ['compressed'],
      success: (res) => {
        const filePath = res.tempFiles[0].tempFilePath;
        const token = wx.getStorageSync('token') || wx.getStorageSync('authToken');
        this.setData({ uploading: true });
        wx.showLoading({ title: '上传中...' });
        wx.uploadFile({
          url: app.globalData.baseUrl + '/api/upload/image',
          filePath,
          name: 'file',
          header: token ? { 'X-Auth-Token': token } : {},
          success: (uploadRes) => {
            wx.hideLoading();
            this.setData({ uploading: false });
            try {
              const data = JSON.parse(uploadRes.data || '{}');
              if (data.success && data.url) {
                this.setData({ 'form.coverUrl': data.url });
                wx.showToast({ title: '封面上传成功', icon: 'success' });
              } else {
                wx.showToast({ title: data.message || '上传失败', icon: 'none' });
              }
            } catch (err) {
              wx.showToast({ title: '上传失败', icon: 'none' });
            }
          },
          fail: () => {
            wx.hideLoading();
            this.setData({ uploading: false });
            wx.showToast({ title: '上传失败', icon: 'none' });
          }
        });
      }
    });
  },

  submitForm() {
    const { form, editingId, hospitalId } = this.data;
    if (!form.title.trim()) {
      wx.showToast({ title: '请输入标题', icon: 'none' });
      return;
    }
    wx.showLoading({ title: '保存中...' });
    const url = editingId
      ? app.globalData.baseUrl + '/api/hospital-articles/' + editingId
      : app.globalData.baseUrl + '/api/hospital-articles';
    wx.request({
      url,
      method: editingId ? 'PUT' : 'POST',
      header: Object.assign({ 'Content-Type': 'application/json' }, this.authHeader()),
      data: {
        hospitalId,
        title: form.title.trim(),
        summary: form.summary.trim(),
        content: form.content,
        coverUrl: form.coverUrl,
        category: form.category,
        status: 1
      },
      success: (res) => {
        wx.hideLoading();
        if (res.data && res.data.success) {
          wx.showToast({ title: editingId ? '已更新' : '已发布', icon: 'success' });
          this.setData({ showForm: false });
          this.loadArticles();
        } else {
          wx.showToast({ title: (res.data && res.data.message) || '保存失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '网络异常', icon: 'none' });
      }
    });
  },

  toggleStatus(e) {
    const article = e.currentTarget.dataset.article;
    const nextStatus = article.status === 1 ? 0 : 1;
    wx.request({
      url: app.globalData.baseUrl + '/api/hospital-articles/' + article.articleId,
      method: 'PUT',
      header: Object.assign({ 'Content-Type': 'application/json' }, this.authHeader()),
      data: { status: nextStatus },
      success: (res) => {
        if (res.data && res.data.success) {
          wx.showToast({ title: nextStatus === 1 ? '已发布' : '已下架', icon: 'success' });
          this.loadArticles();
        } else {
          wx.showToast({ title: (res.data && res.data.message) || '操作失败', icon: 'none' });
        }
      }
    });
  },

  removeArticle(e) {
    const article = e.currentTarget.dataset.article;
    wx.showModal({
      title: '确认删除',
      content: '删除后患者端将不再展示「' + article.title + '」，是否继续？',
      success: (res) => {
        if (!res.confirm) return;
        wx.request({
          url: app.globalData.baseUrl + '/api/hospital-articles/' + article.articleId,
          method: 'DELETE',
          header: this.authHeader(),
          success: (res2) => {
            if (res2.data && res2.data.success) {
              wx.showToast({ title: '已删除', icon: 'success' });
              this.loadArticles();
            } else {
              wx.showToast({ title: (res2.data && res2.data.message) || '删除失败', icon: 'none' });
            }
          }
        });
      }
    });
  },

  goBack() {
    wx.navigateBack();
  }
});
