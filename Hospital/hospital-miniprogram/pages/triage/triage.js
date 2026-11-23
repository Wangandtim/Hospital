const app = getApp();
const http = require('../../utils/request');
const elderlyMode = require('../../utils/elderly');
const deptIcon = require('../../utils/dept-icon');

const COMMON_AILS = [
  { label: '咳嗽发热', text: '咳嗽三天了，还有点发热' },
  { label: '头晕', text: '头晕，睡不好' },
  { label: '胃不舒服', text: '胃不舒服，有点反酸' },
  { label: '腰腿疼', text: '腰疼，腿也疼' },
  { label: '拉肚子', text: '拉肚子两天了' },
  { label: '睡不着', text: '最近老是失眠' },
  { label: '心慌', text: '心慌，有点胸闷' },
  { label: '尿频尿急', text: '尿频尿急' }
];

const QUICK_CHIPS = [
  '咳嗽', '发热', '头痛', '胃痛', '腹泻', '腰疼', '皮疹', '失眠'
];

// 可恢复的分诊会话状态：已评估/待补资料/锁号失败/已确认
const RESUMABLE_STATUSES = ['ASSESSED', 'NEED_PROFILE', 'FAILED', 'CONFIRMED'];
const SESSION_STORAGE_KEY = 'triageSession';
const SESSION_RESUME_TTL_MS = 2 * 60 * 60 * 1000;

Page({
  data: {
    elderly: false,
    voiceMode: false,
    mode: 'STANDARD',
    subject: 'SELF',
    step: 'input',
    inputText: '',
    chips: QUICK_CHIPS,
    commonAils: COMMON_AILS,
    loading: false,
    recording: false,
    asrLoading: false,
    asrEnabled: null,
    heardText: '',
    showHeard: false,

    triageId: '',
    status: '',
    tier: '',
    tierClass: 'tier-medium',
    copyTitle: '',
    copySubtitle: '',
    primaryText: '',
    secondaryText: '',
    tertiaryText: '',
    lowHint: '',
    deptName: '',
    expertLabel: '',
    expertReason: '',
    recommender: null,
    alternatives: [],
    notices: [],
    evidence: [],
    followUps: [],
    clarifyQuestions: [],
    clarifyAnswers: {},
    clarifyHint: '',
    showEvidence: false,
    bookingAllowed: false,
    bookingBlockedReason: '',

    missingFields: [],
    profileAnswers: {},

    lock: null,
    lockExpired: false,
    countdownText: '10:00',
    paying: false,
    appointment: null,
    baseUrl: app.globalData.baseUrl
  },

  onLoad(options) {
    const isElderly = elderlyMode.isElderly();
    const voice = options.voice === '1' || options.mode === 'ELDER';
    this.setData({
      elderly: isElderly || voice,
      voiceMode: voice,
      mode: voice ? 'ELDER' : 'STANDARD'
    });
    this.recorder = wx.getRecorderManager();
    this.recorder.onStop((res) => {
      this.setData({ recording: false });
      if (!this.data.voiceMode) {
        return;
      }
      if (res && res.tempFilePath) {
        this.uploadRecord(res.tempFilePath);
      } else {
        wx.showToast({ title: '点一下下面的不舒服', icon: 'none', duration: 2200 });
      }
    });
    this.recorder.onError(() => {
      this.setData({ recording: false, asrLoading: false });
      wx.showToast({ title: '录音不可用，请点下面的不舒服', icon: 'none', duration: 2200 });
    });
    this.prepareLocation();
    this.checkResume();
  },

  // ==================== 刷新恢复 ====================

  saveSession(res) {
    if (!res || !res.triageId || RESUMABLE_STATUSES.indexOf(res.sessionStatus) < 0) {
      this.clearSession();
      return;
    }
    try {
      wx.setStorageSync(SESSION_STORAGE_KEY, { id: res.triageId, ts: Date.now() });
    } catch (e) {
      // 存储不可用时只保留当前页面状态
    }
  },

  clearSession() {
    try {
      wx.removeStorageSync(SESSION_STORAGE_KEY);
    } catch (e) {
      // 忽略存储异常
    }
  },

  checkResume() {
    let stored = null;
    try {
      stored = wx.getStorageSync(SESSION_STORAGE_KEY);
    } catch (e) {
      stored = null;
    }
    if (!stored || !stored.id) {
      return;
    }
    if (!stored.ts || Date.now() - stored.ts > SESSION_RESUME_TTL_MS) {
      this.clearSession();
      return;
    }
    wx.showModal({
      title: '继续上次分诊？',
      content: '你还有一次没完成的分诊，可以接着上次的结果继续。',
      confirmText: '继续',
      cancelText: '重新开始',
      success: (res) => {
        if (res.confirm) {
          this.resumeTriage(stored.id);
        } else {
          this.clearSession();
        }
      }
    });
  },

  resumeTriage(triageId) {
    wx.showLoading({ title: '正在恢复…', mask: true });
    http.get('/api/triage/' + triageId)
      .then((res) => {
        wx.hideLoading();
        if (!res || res.success === false) {
          this.clearSession();
          return;
        }
        if (RESUMABLE_STATUSES.indexOf(res.sessionStatus) < 0) {
          this.clearSession();
          wx.showToast({ title: '上次分诊已结束，请重新描述', icon: 'none' });
          return;
        }
        this.applyResult(res);
      })
      .catch(() => {
        wx.hideLoading();
        this.clearSession();
      });
  },

  onShow() {
    if (this.refreshAfterProfile) {
      this.refreshAfterProfile = false;
      this.refreshTriage();
    }
  },

  refreshTriage() {
    if (!this.data.triageId) {
      return;
    }
    http.get('/api/triage/' + this.data.triageId)
      .then((res) => {
        this.applyResult(res);
      })
      .catch(() => { /* 保持当前页面状态 */ });
  },

  onUnload() {
    this.clearCountdown();
    if (this.recorder) {
      try {
        this.recorder.stop();
      } catch (e) { /* 未在录音 */ }
    }
  },

  prepareLocation() {
    wx.getLocation({
      type: 'gcj02',
      success: (res) => {
        this.latitude = res.latitude;
        this.longitude = res.longitude;
      },
      fail: () => {
        this.latitude = null;
        this.longitude = null;
        if (!this._locationTipShown) {
          this._locationTipShown = true;
          wx.showToast({ title: '未获取定位，将按默认城市推荐医院', icon: 'none', duration: 2500 });
        }
      }
    });
  },

  ensureLogin() {
    const token = wx.getStorageSync('authToken');
    if (token) {
      return true;
    }
    wx.showModal({
      title: '需要登录',
      content: '登录后才能使用智能分诊',
      confirmText: '去登录',
      success: (res) => {
        if (res.confirm) {
          wx.navigateTo({ url: '/pages/login/login' });
        }
      }
    });
    return false;
  },

  // ==================== 输入态 ====================

  onInput(e) {
    this.setData({ inputText: e.detail.value });
  },

  tapChip(e) {
    const text = e.currentTarget.dataset.text;
    const current = this.data.inputText || '';
    if (current.indexOf(text) >= 0) {
      return;
    }
    this.setData({ inputText: current + text });
  },

  tapAil(e) {
    if (this.data.loading) {
      return;
    }
    const text = e.currentTarget.dataset.text;
    this.setData({ inputText: text, showHeard: false });
    this.runAssess(text);
  },

  submitInput() {
    const text = (this.data.inputText || '').trim();
    if (!text) {
      wx.showToast({ title: '先说说哪里不舒服', icon: 'none' });
      return;
    }
    this.runAssess(text);
  },

  toggleSubject() {
    const next = this.data.subject === 'SELF' ? 'ELDER' : 'SELF';
    this.setData({ subject: next });
  },

  switchToText() {
    this.setData({ voiceMode: false, mode: 'STANDARD' });
  },

  // ==================== 录音 ====================

  startRecord() {
    if (!this.data.voiceMode || this.data.recording) {
      return;
    }
    this.setData({ recording: true, showHeard: false, heardText: '' });
    try {
      this.recorder.start({
        duration: 30000,
        format: 'mp3',
        sampleRate: 16000,
        numberOfChannels: 1,
        encodeBitRate: 48000
      });
    } catch (e) {
      this.setData({ recording: false });
    }
  },

  stopRecord() {
    if (!this.data.recording) {
      return;
    }
    this.setData({ recording: false });
    try {
      this.recorder.stop();
    } catch (e) { /* 已结束 */ }
  },

  uploadRecord(tempFilePath) {
    if (!this.ensureLogin()) {
      return;
    }
    if (this.data.asrEnabled === false) {
      wx.showToast({ title: '点一下下面的不舒服', icon: 'none', duration: 2200 });
      return;
    }
    const token = wx.getStorageSync('authToken');
    this.setData({ asrLoading: true });
    wx.uploadFile({
      url: app.globalData.baseUrl + '/api/voice/transcribe',
      filePath: tempFilePath,
      name: 'file',
      header: { 'X-Auth-Token': token },
      success: (res) => {
        this.setData({ asrLoading: false });
        let data = null;
        try {
          data = JSON.parse(res.data);
        } catch (e) {
          data = null;
        }
        if (!data) {
          wx.showToast({ title: '没听清，点一下下面的不舒服', icon: 'none', duration: 2200 });
          return;
        }
        if (data.enabled === false) {
          this.setData({ asrEnabled: false });
          wx.showToast({ title: '点一下下面的不舒服', icon: 'none', duration: 2200 });
          return;
        }
        this.setData({ asrEnabled: true });
        if (data.success === false || !data.text) {
          wx.showToast({ title: (data.message || '没听清，点一下下面的不舒服'), icon: 'none', duration: 2400 });
          return;
        }
        if (data.lowConfidence) {
          wx.showToast({ title: '没太听清，可以点下面的按钮', icon: 'none', duration: 2400 });
          return;
        }
        this.setData({ heardText: data.text, showHeard: true });
      },
      fail: () => {
        this.setData({ asrLoading: false });
        wx.showToast({ title: '没听清，点一下下面的不舒服', icon: 'none', duration: 2200 });
      }
    });
  },

  confirmHeard() {
    const text = this.data.heardText;
    this.setData({ showHeard: false });
    if (text) {
      this.setData({ inputText: text });
      this.runAssess(text);
    }
  },

  retryRecord() {
    this.setData({ showHeard: false, heardText: '' });
  },

  // ==================== 评估 ====================

  runAssess(text) {
    if (!this.ensureLogin()) {
      return;
    }
    if (this.data.loading) {
      return;
    }
    this.setData({ loading: true });
    wx.showLoading({ title: '正在判断…', mask: true });
    const payload = {
      symptoms: text,
      mode: this.data.mode,
      subject: this.data.subject
    };
    if (this.latitude !== undefined && this.latitude !== null) {
      payload.latitude = this.latitude;
      payload.longitude = this.longitude;
    }
    http.post('/api/triage/assess', payload)
      .then((res) => {
        wx.hideLoading();
        this.setData({ loading: false });
        this.applyResult(res);
      })
      .catch((err) => {
        wx.hideLoading();
        this.setData({ loading: false });
      });
  },

  applyResult(res) {
    if (!res || res.success === false) {
      wx.showToast({ title: (res && res.message) || '判断失败，请重试', icon: 'none' });
      return;
    }
    this.setData({ triageId: res.triageId, status: res.status });
    this.saveSession(res);
    this.clearCountdown();

    if (res.status === 'RED_FLAG') {
      this.clearSession();
      const reason = (res.redFlag && res.redFlag.reason) || '存在需要立即就医的症状';
      const emergency = res.emergency || {};
      let url = '/pages/triage-emergency/triage-emergency?reason=' + encodeURIComponent(reason);
      if (emergency.hospitalName) {
        url += '&hospitalName=' + encodeURIComponent(emergency.hospitalName);
      }
      if (emergency.hospitalPhone) {
        url += '&hospitalPhone=' + encodeURIComponent(emergency.hospitalPhone);
      }
      if (emergency.hospitalAddress) {
        url += '&hospitalAddress=' + encodeURIComponent(emergency.hospitalAddress);
      }
      if (emergency.phone) {
        url += '&emergencyPhone=' + encodeURIComponent(emergency.phone);
      }
      if (emergency.hospitalDistanceKm !== undefined && emergency.hospitalDistanceKm !== null) {
        url += '&hospitalDistanceKm=' + emergency.hospitalDistanceKm;
      }
      wx.navigateTo({ url: url });
      return;
    }

    if (res.missingFields && res.missingFields.length > 0) {
      this.setData({
        step: 'profile',
        missingFields: res.missingFields,
        profileAnswers: {},
        tertiaryText: '',
        copyTitle: (res.copy && res.copy.title) || '还差一点信息',
        copySubtitle: (res.copy && res.copy.subtitle) || ''
      });
      return;
    }

    const copy = res.copy || {};
    const tier = res.confidenceTier || '';
    const isLow = tier === 'LOW';
    const recommender = res.recommendation || null;
    const clarify = (res.clarify && res.clarify.questions && res.clarify.questions.length)
      ? res.clarify : null;

    this.setData({
      step: clarify ? 'clarify' : 'result',
      tier: tier,
      tierClass: tier === 'HIGH' ? 'tier-high' : (isLow ? 'tier-low' : 'tier-medium'),
      copyTitle: copy.title || '',
      copySubtitle: copy.subtitle || '',
      primaryText: copy.primary || (recommender ? '去付款' : '重新判断'),
      secondaryText: copy.secondary || '',
      tertiaryText: tier === 'MEDIUM' && !clarify ? '补充症状' : '',
      lowHint: copy.lowHint || '',
      deptName: (res.department && res.department.name) || '',
      expertLabel: res.expertLabel || '',
      expertReason: res.expertReason || '',
      recommender: recommender ? { ...recommender, deptIcon: deptIcon.iconPath(recommender.departmentName) } : null,
      alternatives: res.alternatives || [],
      notices: res.notices || [],
      evidence: res.evidence || [],
      followUps: res.followUpQuestions || [],
      clarifyQuestions: clarify ? clarify.questions : [],
      clarifyAnswers: {},
      clarifyHint: clarify ? clarify.hint : '',
      bookingAllowed: !!res.bookingAllowed,
      bookingBlockedReason: res.bookingBlockedReason || '',
      lock: null,
      lockExpired: false,
      showEvidence: false
    });
  },

  chooseClarify(e) {
    const key = e.currentTarget.dataset.key;
    const value = e.currentTarget.dataset.value;
    const answers = Object.assign({}, this.data.clarifyAnswers);
    answers[key] = value;
    this.setData({ clarifyAnswers: answers });
  },

  submitClarify() {
    const questions = this.data.clarifyQuestions || [];
    const answers = this.data.clarifyAnswers || {};
    const list = [];
    for (let i = 0; i < questions.length; i++) {
      const value = answers[questions[i].key];
      if (value) {
        list.push(value);
      }
    }
    if (list.length === 0) {
      wx.showToast({ title: '先点一个最接近的选项', icon: 'none' });
      return;
    }
    if (this.data.loading) {
      return;
    }
    this.setData({ loading: true });
    wx.showLoading({ title: '正在继续判断…', mask: true });
    http.post('/api/triage/' + this.data.triageId + '/clarify', { answers: list })
      .then((res) => {
        wx.hideLoading();
        this.setData({ loading: false });
        this.applyResult(res);
      })
      .catch(() => {
        wx.hideLoading();
        this.setData({ loading: false });
      });
  },

  // ==================== 补资料 ====================

  chooseProfile(e) {
    const key = e.currentTarget.dataset.key;
    const value = e.currentTarget.dataset.value;
    const answers = Object.assign({}, this.data.profileAnswers);
    answers[key] = value;
    this.setData({ profileAnswers: answers });
  },

  submitProfile() {
    const fields = this.data.missingFields || [];
    const answers = this.data.profileAnswers || {};
    for (let i = 0; i < fields.length; i++) {
      if (!answers[fields[i].key]) {
        wx.showToast({ title: '请选择' + fields[i].label, icon: 'none' });
        return;
      }
    }
    if (this.data.loading) {
      return;
    }
    this.setData({ loading: true });
    wx.showLoading({ title: '继续判断…', mask: true });
    const body = {};
    if (answers.age) {
      body.ageBand = answers.age;
    }
    if (answers.gender) {
      body.gender = answers.gender;
    }
    if (answers.pregnant) {
      body.pregnant = answers.pregnant;
    }
    http.post('/api/triage/' + this.data.triageId + '/profile', body)
      .then((res) => {
        wx.hideLoading();
        this.setData({ loading: false });
        this.applyResult(res);
      })
      .catch(() => {
        wx.hideLoading();
        this.setData({ loading: false });
      });
  },

  // ==================== 结果操作 ====================

  toggleEvidence() {
    this.setData({ showEvidence: !this.data.showEvidence });
  },

  primaryAction() {
    if (this.data.lockExpired) {
      this.doLock();
      return;
    }
    if (this.data.lock) {
      this.setData({ step: 'pay' });
      return;
    }
    if (this.data.primaryText === '重新判断') {
      this.backToInput();
      return;
    }
    if (this.data.tier === 'LOW') {
      this.backToInput();
      return;
    }
    if (!this.data.bookingAllowed) {
      const reason = this.data.bookingBlockedReason || '';
      if (reason.indexOf('姓名') >= 0 || reason.indexOf('手机号') >= 0 || reason.indexOf('就诊人信息') >= 0) {
        this.refreshAfterProfile = true;
        wx.navigateTo({ url: '/pages/patient-info/patient-info' });
        return;
      }
      wx.showToast({ title: reason || '当前不能预约', icon: 'none' });
      return;
    }
    if (this.data.tier === 'MEDIUM') {
      this.confirmThenLock();
      return;
    }
    this.doLock();
  },

  secondaryAction() {
    if (this.data.tier === 'LOW') {
      wx.navigateTo({ url: '/pages/contact/contact' });
      return;
    }
    if (this.data.lock) {
      return;
    }
    this.rejectDept();
  },

  tertiaryAction() {
    if (this.data.tier !== 'MEDIUM' || this.data.lock) {
      return;
    }
    this.clearCountdown();
    this.clearSession();
    this.setData({
      step: 'input',
      triageId: '',
      lock: null,
      lockExpired: false,
      status: '',
      tier: '',
      recommender: null,
      alternatives: [],
      notices: [],
      evidence: [],
      followUps: [],
      clarifyQuestions: [],
      clarifyAnswers: {},
      tertiaryText: ''
    });
  },

  backToInput() {
    this.clearCountdown();
    this.clearSession();
    this.setData({
      step: 'input',
      triageId: '',
      inputText: '',
      recommender: null,
      alternatives: [],
      notices: [],
      evidence: [],
      followUps: [],
      clarifyQuestions: [],
      clarifyAnswers: {},
      lock: null,
      lockExpired: false,
      status: '',
      tier: '',
      tertiaryText: ''
    });
  },

  rejectDept() {
    http.post('/api/triage/' + this.data.triageId + '/reject')
      .then(() => {
        wx.showToast({ title: '已记录，可以重新描述', icon: 'none' });
        this.backToInput();
      })
      .catch(() => { /* 统一提示 */ });
  },

  confirmThenLock() {
    wx.showLoading({ title: '确认中…', mask: true });
    http.post('/api/triage/' + this.data.triageId + '/confirm')
      .then((res) => {
        wx.hideLoading();
        if (res && res.success === false) {
          wx.showToast({ title: res.message || '确认失败', icon: 'none' });
          return;
        }
        this.doLock();
      })
      .catch(() => {
        wx.hideLoading();
      });
  },

  doLock() {
    if (this.data.loading) {
      return;
    }
    this.setData({ loading: true });
    wx.showLoading({ title: '正在为你保留号源…', mask: true });
    http.post('/api/triage/' + this.data.triageId + '/lock')
      .then((res) => {
        wx.hideLoading();
        this.setData({ loading: false });
        if (!res || res.success === false) {
          wx.showToast({ title: (res && res.message) || '号源保留失败，请重试', icon: 'none' });
          this.setData({ lockExpired: true, primaryText: '重新保留' });
          return;
        }
        this.setData({
          step: 'pay',
          lock: res,
          lockExpired: false,
          primaryText: '去付款'
        });
        this.startCountdown(res.ttlSeconds || 600);
      })
      .catch(() => {
        wx.hideLoading();
        this.setData({ loading: false });
      });
  },

  // ==================== 倒计时 ====================

  startCountdown(seconds) {
    this.clearCountdown();
    this.countdownEnd = Date.now() + seconds * 1000;
    this.countdownWarned = false;
    this.updateCountdown();
    this.timer = setInterval(() => this.updateCountdown(), 1000);
  },

  updateCountdown() {
    const remain = Math.max(0, this.countdownEnd - Date.now());
    const total = Math.floor(remain / 1000);
    const mm = String(Math.floor(total / 60)).padStart(2, '0');
    const ss = String(total % 60).padStart(2, '0');
    const patch = { countdownText: mm + ':' + ss };
    if (remain <= 60000 && !this.countdownWarned) {
      this.countdownWarned = true;
      wx.showToast({ title: '号源快到期了，确认支付就能保留', icon: 'none', duration: 2400 });
    }
    if (remain <= 0) {
      this.clearCountdown();
      patch.lockExpired = true;
      patch.primaryText = '重新保留';
      patch.lock = null;
      patch.step = 'result';
    }
    this.setData(patch);
  },

  clearCountdown() {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
  },

  // ==================== 支付 ====================

  payNow() {
    if (this.data.paying || !this.data.lock) {
      return;
    }
    if (this.data.lockExpired) {
      this.doLock();
      return;
    }
    const lock = this.data.lock;
    this.setData({ paying: true });
    wx.showLoading({ title: '正在提交…', mask: true });
    http.post('/api/triage/' + this.data.triageId + '/book')
      .then((bookRes) => {
        if (!bookRes || bookRes.success === false) {
          wx.hideLoading();
          this.setData({ paying: false });
          if (bookRes && bookRes.needRelock) {
            this.setData({ step: 'result', lock: null, lockExpired: true, primaryText: '重新保留' });
          }
          if (bookRes && bookRes.needChooseAnother) {
            this.setData({ step: 'result', lock: null, lockExpired: true, primaryText: '重新选择' });
          }
          wx.showToast({ title: (bookRes && bookRes.message) || '提交失败', icon: 'none' });
          return null;
        }
        return http.post('/api/payment/pay', {
          appointmentId: bookRes.appointmentId,
          paymentMethod: 'wechat'
        }).then((payRes) => ({ bookRes, payRes }));
      })
      .then((result) => {
        if (!result) {
          return;
        }
        wx.hideLoading();
        if (!result.payRes || result.payRes.success === false) {
          this.setData({ paying: false });
          wx.showToast({ title: (result.payRes && result.payRes.message) || '支付失败，可重试', icon: 'none' });
          return;
        }
        this.clearCountdown();
        this.clearSession();
        this.setData({
          paying: false,
          step: 'success',
          lock: null,
          appointment: {
            appointmentId: result.bookRes.appointmentId,
            hospitalName: lock.hospitalName,
            departmentName: lock.departmentName,
            doctorName: lock.doctorName,
            doctorTitle: lock.doctorTitle,
            dateText: lock.dateText,
            timeSlot: lock.timeSlot,
            fee: lock.fee
          }
        });
      })
      .catch(() => {
        wx.hideLoading();
        this.setData({ paying: false });
      });
  },

  payLater() {
    this.setData({ step: 'result' });
    wx.showToast({ title: '号源会为你保留 10 分钟', icon: 'none' });
  },

  relock() {
    this.doLock();
  },

  goMyAppointments() {
    wx.switchTab({ url: '/pages/my-appointments/my-appointments' });
  },

  goHome() {
    wx.switchTab({ url: '/pages/index/index' });
  }
});
