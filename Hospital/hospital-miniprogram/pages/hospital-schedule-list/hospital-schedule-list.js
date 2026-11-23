const app = getApp();
const deptIcon = require('../../utils/dept-icon');

const DEPT_SIZE = 8;
const SLOT_SIZE = 10;

const TITLE_OPTIONS = [
  { label: '全部', value: '' },
  { label: '主任医师', value: '主任医师' },
  { label: '副主任医师', value: '副主任医师' },
  { label: '主治医师', value: '主治医师' }
];

Page({
  data: {
    hospitalId: '',
    hospitalName: '',
    keyword: '',
    titles: TITLE_OPTIONS,
    titleIndex: 0,
    titleValue: '',
    pager: { status: 'idle', hasMore: false, total: 0, error: '', loading: false },
    deptPage: 0,
    totalPages: 0,
    total: 0,
    scheduleTotal: 0,
    scheduleGroups: [],
    currentYear: 2024,
    currentMonth: 1,
    selectedDate: '',
    calendarData: [],
    scheduleDates: {}
  },

  onLoad(options) {
    if (options && options.hospitalId) {
      this.setData({ hospitalId: options.hospitalId });
    } else {
      const storedHospitalId = wx.getStorageSync('hospitalId');
      this.setData({ hospitalId: storedHospitalId });
    }

    this._seq = 0;
    this._deptPage = 0;
    this._expanding = {};

    const now = new Date();
    this.setData({
      currentYear: now.getFullYear(),
      currentMonth: now.getMonth() + 1,
      selectedDate: this.formatDate(now)
    });

    this.loadHospitalName();
    this.loadScheduleDatesFromApi();
    this.loadPage(0);
  },

  onShow() {
    if (!this.data.scheduleGroups.length && !this.data.pager.loading) {
      this.loadPage(this.data.deptPage);
    }
  },

  onUnload() {
    this._seq += 1;
    if (this._keywordTimer) clearTimeout(this._keywordTimer);
  },

  formatDate(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
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

  loadScheduleDatesFromApi() {
    wx.request({
      // 只取有排班的日期用于日历标记，不拉取排班明细
      url: app.globalData.baseUrl + '/api/schedules/hospital/' + this.data.hospitalId + '/dates',
      method: 'GET',
      success: (res) => {
        const dates = (res.data && res.data.success && Array.isArray(res.data.data)) ? res.data.data : [];
        const scheduleDates = {};
        dates.forEach(date => {
          const dateStr = typeof date === 'string' ? date.substring(0, 10) : '';
          if (dateStr) {
            scheduleDates[dateStr] = true;
          }
        });
        this.setData({ scheduleDates });
        this.generateCalendar();
      },
      fail: () => {
        this.setData({ scheduleDates: {} });
        this.generateCalendar();
      }
    });
  },

  buildUrl(page, slotPage, deptId) {
    const keyword = (this.data.keyword || '').trim();
    const title = this.data.titleValue;
    let url = app.globalData.baseUrl + '/api/schedules/hospital/' + this.data.hospitalId +
      '/date/' + this.data.selectedDate + '/page?groupBy=dept&page=' + page +
      '&size=' + DEPT_SIZE + '&slotPage=' + slotPage + '&slotSize=' + SLOT_SIZE;
    if (deptId) url += '&deptId=' + encodeURIComponent(deptId);
    if (keyword) url += '&keyword=' + encodeURIComponent(keyword);
    if (title) url += '&title=' + encodeURIComponent(title);
    return url;
  },

  requestGroups(page, slotPage, deptId) {
    return new Promise((resolve, reject) => {
      wx.request({
        url: this.buildUrl(page, slotPage, deptId),
        method: 'GET',
        success: res => {
          if (res.statusCode >= 200 && res.statusCode < 300) {
            resolve(res.data || {});
          } else {
            reject(new Error('HTTP ' + res.statusCode));
          }
        },
        fail: reject
      });
    });
  },

  decorateGroup(group) {
    return {
      deptId: group.deptId || '',
      deptName: group.deptName || '其他科室',
      deptIcon: deptIcon.iconPath(group.deptName || '其他科室'),
      scheduleCount: Number(group.scheduleCount || 0),
      slotPage: Number(group.slotPage || 0),
      slotHasMore: Boolean(group.slotHasMore),
      slots: (group.slots || []).map(slot => Object.assign({}, slot, {
        timeText: slot.timeSlot || '',
        statusText: slot.statusText || (slot.remainingSlots > 0 ? '余 ' + slot.remainingSlots : '')
      }))
    };
  },

  loadPage(page) {
    const seq = ++this._seq;
    this.setData({
      pager: { status: 'loading', hasMore: false, total: 0, error: '', loading: true },
      deptPage: page
    });

    this.requestGroups(page, 0, '')
      .then(body => {
        if (seq !== this._seq) return;
        const groups = (body.groups || []).map(group => this.decorateGroup(group));
        const total = Number(body.total !== undefined ? body.total : groups.length);
        const totalPages = Number(body.totalPages !== undefined ? body.totalPages : 1);

        let status;
        if (!groups.length) {
          status = 'empty';
        } else {
          status = 'done';
        }

        this._deptPage = page;
        this.setData({
          scheduleGroups: groups,
          total: total,
          totalPages: totalPages,
          scheduleTotal: Number(body.scheduleTotal || 0),
          deptPage: page,
          pager: { status: status, hasMore: false, total: total, error: '', loading: false }
        });
      })
      .catch(error => {
        if (seq !== this._seq) return;
        this.setData({
          scheduleGroups: [],
          pager: {
            status: 'error',
            hasMore: false,
            total: 0,
            error: (error && (error.errMsg || error.message)) || '加载失败，请重试',
            loading: false
          }
        });
      });
  },

  expandSlots(e) {
    const deptId = e.currentTarget.dataset.deptId;
    const slotPage = Number(e.currentTarget.dataset.slotPage || 0);
    if (!deptId || this._expanding[deptId]) return;

    this._expanding[deptId] = true;
    const seq = this._seq;

    this.requestGroups(0, slotPage + 1, deptId)
      .then(body => {
        delete this._expanding[deptId];
        if (seq !== this._seq) return;
        const source = (body.groups || [])[0];
        if (!source) return;
        const incoming = this.decorateGroup(source);

        const scheduleGroups = this.data.scheduleGroups.map(group => {
          if (group.deptId !== deptId) return group;
          return Object.assign({}, group, {
            slots: group.slots.concat(incoming.slots),
            slotPage: incoming.slotPage,
            slotHasMore: incoming.slotHasMore
          });
        });
        this.setData({ scheduleGroups: scheduleGroups });
      })
      .catch(() => {
        delete this._expanding[deptId];
      });
  },

  prevPage() {
    if (this.data.deptPage > 0) {
      this.loadPage(this.data.deptPage - 1);
    }
  },

  nextPage() {
    if (this.data.deptPage + 1 < this.data.totalPages) {
      this.loadPage(this.data.deptPage + 1);
    }
  },

  retryLoad() {
    this.loadPage(this.data.deptPage);
  },

  onKeywordInput(e) {
    this.setData({ keyword: e.detail.value });
    if (this._keywordTimer) clearTimeout(this._keywordTimer);
    this._keywordTimer = setTimeout(() => this.loadPage(0), 400);
  },

  clearKeyword() {
    if (this._keywordTimer) clearTimeout(this._keywordTimer);
    this.setData({ keyword: '' });
    this.loadPage(0);
  },

  onTitleTap(e) {
    const index = Number(e.currentTarget.dataset.index);
    if (index === this.data.titleIndex) return;
    const option = TITLE_OPTIONS[index] || TITLE_OPTIONS[0];
    this.setData({ titleIndex: index, titleValue: option.value });
    this.loadPage(0);
  },

  generateCalendar() {
    const year = this.data.currentYear;
    const month = this.data.currentMonth;

    const firstDay = new Date(year, month - 1, 1);
    const lastDay = new Date(year, month, 0);
    const daysInMonth = lastDay.getDate();
    const startWeekDay = firstDay.getDay();
    const calendarData = [];

    for (let i = 0; i < startWeekDay; i++) {
      calendarData.push({ day: '', dateStr: '', hasSchedule: false, dateClass: 'empty' });
    }
    const today = this.formatDate(new Date());
    const selectedDate = this.data.selectedDate;

    for (let day = 1; day <= daysInMonth; day++) {
      const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
      const hasSchedule = this.data.scheduleDates[dateStr] || false;
      const classNames = [];
      if (dateStr === today) classNames.push('today');
      if (dateStr === selectedDate) classNames.push('selected');
      if (dateStr < today) classNames.push('past');
      if (hasSchedule) classNames.push('has-schedule');
      calendarData.push({ day, dateStr, hasSchedule, dateClass: classNames.join(' ') });
    }

    this.setData({ calendarData });
  },

  prevMonth() {
    let year = this.data.currentYear;
    let month = this.data.currentMonth;
    if (month === 1) {
      year--;
      month = 12;
    } else {
      month--;
    }
    this.setData({ currentYear: year, currentMonth: month });
    this.generateCalendar();
  },

  nextMonth() {
    let year = this.data.currentYear;
    let month = this.data.currentMonth;
    if (month === 12) {
      year++;
      month = 1;
    } else {
      month++;
    }
    this.setData({ currentYear: year, currentMonth: month });
    this.generateCalendar();
  },

  selectDate(e) {
    const dateStr = e.currentTarget && e.currentTarget.dataset ? e.currentTarget.dataset.date : '';
    if (!dateStr) return;

    const today = this.formatDate(new Date());
    if (dateStr < today) {
      wx.showToast({ title: '只能选择未来日期', icon: 'none' });
      return;
    }

    this.setData({ selectedDate: dateStr });
    this.generateCalendar();
    this.loadPage(0);
  },

  getDateClass(item) {
    const classes = [];
    if (!item.day) {
      classes.push('empty');
      return classes.join(' ');
    }
    const today = this.formatDate(new Date());
    if (item.dateStr === today) classes.push('today');
    if (item.dateStr === this.data.selectedDate) classes.push('selected');
    if (item.dateStr < today) classes.push('past');
    if (item.hasSchedule) classes.push('has-schedule');
    return classes.join(' ');
  },

  goBack() {
    wx.navigateBack({ delta: 1 });
  }
});
