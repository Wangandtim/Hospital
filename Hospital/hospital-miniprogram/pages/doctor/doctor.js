const app = getApp();
const placeholder = require('../../utils/placeholder');

const TITLE_OPTIONS = [
  { label: '全部', value: '' },
  { label: '主任医师', value: '主任医师' },
  { label: '副主任医师', value: '副主任医师' },
  { label: '主治医师', value: '主治医师' }
];

const DEPT_PAGE_SIZE = 8;
const DOCTOR_PAGE_SIZE = 6;

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
    groups: [],
    deptId: '',
    hospitalId: '',
    currentHospital: null,
    keyword: '',
    titles: TITLE_OPTIONS,
    titleIndex: 0,
    titleValue: '',
    pager: { status: 'idle', hasMore: true, total: 0, error: '', loading: false, page: 0 },
    isLoading: true
  },

  onLoad(options) {
    const hospitalId = options.hospitalId || '';
    const deptId = options.deptId || '';
    this._groupSeq = 0;
    this._deptPage = 0;
    this._loading = false;
    this._expanding = {};
    this.setData({ hospitalId: hospitalId, deptId: deptId });

    if (!hospitalId && !deptId) {
      wx.showToast({ title: '未指定医院或科室', icon: 'none' });
      return;
    }

    if (hospitalId) {
      this.loadHospital(hospitalId);
    }
    this.loadGroups(true);
  },

  onUnload() {
    this._groupSeq += 1;
    if (this._keywordTimer) clearTimeout(this._keywordTimer);
  },

  onReachBottom() {
    if (this.data.pager.hasMore && !this._loading) {
      this.loadGroups(false);
    }
  },

  decorateDoctors(list) {
    return (list || []).map(doctor => {
      const extra = placeholder.doctorExtra(doctor);
      const remaining = Number(doctor.todayRemaining);
      return Object.assign({}, doctor, extra, {
        avatarText: String(doctor.name || '医').slice(0, 1),
        photoUrlFull: resolveMedia(doctor.photoUrl),
        available: isNaN(remaining) ? true : remaining > 0
      });
    });
  },

  decorateGroup(group) {
    return {
      deptId: group.deptId || '',
      deptName: group.deptName || '其他科室',
      doctorTotal: Number(group.doctorTotal || (group.doctors || []).length || 0),
      doctorPage: Number(group.doctorPage || 0),
      doctorHasMore: Boolean(group.doctorHasMore),
      doctors: this.decorateDoctors(group.doctors)
    };
  },

  buildUrl(deptPage, doctorPage, deptId) {
    const keyword = this.data.keyword.trim();
    const title = this.data.titleValue;
    if (this.data.hospitalId) {
      let url = app.globalData.baseUrl + '/api/doctors/hospital/' + this.data.hospitalId +
        '/page?groupBy=dept&page=' + deptPage + '&size=' + DEPT_PAGE_SIZE +
        '&doctorPage=' + doctorPage + '&doctorSize=' + DOCTOR_PAGE_SIZE;
      if (deptId) url += '&deptId=' + encodeURIComponent(deptId);
      if (keyword) url += '&keyword=' + encodeURIComponent(keyword);
      if (title) url += '&title=' + encodeURIComponent(title);
      return url;
    }
    let url = app.globalData.baseUrl + '/api/doctors/dept/' + this.data.deptId +
      '/page?page=' + deptPage + '&size=' + DOCTOR_PAGE_SIZE;
    if (keyword) url += '&keyword=' + encodeURIComponent(keyword);
    if (title) url += '&title=' + encodeURIComponent(title);
    return url;
  },

  requestGroups(deptPage, doctorPage, deptId) {
    return new Promise((resolve, reject) => {
      wx.request({
        url: this.buildUrl(deptPage, doctorPage, deptId),
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

  loadGroups(reset) {
    if (this._loading) return;
    if (!reset && !this.data.pager.hasMore) return;

    if (reset) {
      this._groupSeq += 1;
      this._deptPage = 0;
      this.setData({
        groups: [],
        isLoading: true,
        pager: { status: 'loading', hasMore: true, total: 0, error: '', loading: true, page: 0 }
      });
    } else {
      this.setData({
        pager: Object.assign({}, this.data.pager, { status: 'loadingMore', loading: true })
      });
    }

    const seq = this._groupSeq;
    const deptPage = this._deptPage;
    this._loading = true;

    this.requestGroups(deptPage, 0, '')
      .then(body => {
        this._loading = false;
        if (seq !== this._groupSeq) return;

        const incoming = (body.groups || []).map(group => this.decorateGroup(group));
        if (!this.data.hospitalId) {
          // 科室平铺返回：合并成单个分组展示
          const flat = this.decorateDoctors(body.data || []);
          const deptName = (flat[0] && flat[0].deptName) || '科室医生';
          const first = {
            deptId: this.data.deptId,
            deptName: deptName,
            doctorTotal: Number(body.total || flat.length),
            doctorPage: deptPage,
            doctorHasMore: Boolean(body.hasMore),
            doctors: flat
          };
          incoming.push(first);
        }

        let groups;
        if (reset) {
          groups = incoming;
        } else {
          groups = this.data.groups.slice();
          incoming.forEach(group => {
            const index = groups.findIndex(item => item.deptId === group.deptId);
            if (index < 0) {
              groups.push(group);
            } else {
              groups[index] = Object.assign({}, groups[index], {
                doctors: this.mergeDoctors(groups[index].doctors, group.doctors),
                doctorHasMore: group.doctorHasMore,
                doctorPage: group.doctorPage
              });
            }
          });
        }

        let hasMore;
        if (this.data.hospitalId) {
          hasMore = body.hasMore !== undefined
            ? Boolean(body.hasMore)
            : (Number(body.page || deptPage) + 1 < Number(body.totalPages || 1));
        } else {
          hasMore = Boolean(body.hasMore);
          if (body.hasMore === undefined && body.totalPages !== undefined) {
            hasMore = deptPage + 1 < Number(body.totalPages);
          }
        }
        this._deptPage = deptPage + 1;

        let status;
        if (!groups.length) {
          status = 'empty';
        } else if (!hasMore) {
          status = 'done';
        } else {
          status = 'more';
        }

        this.setData({
          groups: groups,
          isLoading: false,
          pager: {
            status: status,
            hasMore: hasMore,
            total: Number(body.doctorTotal || body.total || 0),
            error: '',
            loading: false,
            page: deptPage
          }
        });
      })
      .catch(error => {
        this._loading = false;
        if (seq !== this._groupSeq) return;
        this.setData({
          isLoading: false,
          pager: {
            status: 'error',
            hasMore: true,
            total: this.data.pager.total || 0,
            error: (error && (error.errMsg || error.message)) || '加载失败，请重试',
            loading: false,
            page: deptPage
          }
        });
      });
  },

  mergeDoctors(existing, incoming) {
    const merged = (existing || []).slice();
    const ids = {};
    merged.forEach(item => { ids[item.doctorId] = true; });
    (incoming || []).forEach(item => {
      if (!ids[item.doctorId]) {
        ids[item.doctorId] = true;
        merged.push(item);
      }
    });
    return merged;
  },

  expandGroup(e) {
    const deptId = e.currentTarget.dataset.deptId;
    const doctorPage = Number(e.currentTarget.dataset.doctorPage || 0);
    if (!deptId || this._expanding[deptId]) return;
    this._expanding[deptId] = true;
    const seq = this._groupSeq;

    this.requestGroups(0, doctorPage + 1, deptId)
      .then(body => {
        delete this._expanding[deptId];
        if (seq !== this._groupSeq) return;
        const source = this.data.hospitalId
          ? (body.groups || []).find(group => group.deptId === deptId) || (body.groups || [])[0]
          : {
              deptId: deptId,
              doctors: body.data || [],
              doctorHasMore: Boolean(body.hasMore),
              doctorPage: doctorPage + 1
            };
        if (!source) return;

        const incoming = this.decorateGroup(source);
        const groups = this.data.groups.map(group => {
          if (group.deptId !== deptId) return group;
          return Object.assign({}, group, {
            doctors: this.mergeDoctors(group.doctors, incoming.doctors),
            doctorHasMore: this.data.hospitalId ? incoming.doctorHasMore : Boolean(source.doctorHasMore),
            doctorPage: Number(source.doctorPage === undefined ? doctorPage + 1 : source.doctorPage)
          });
        });
        this.setData({ groups: groups });
      })
      .catch(() => {
        delete this._expanding[deptId];
      });
  },

  onKeywordInput(e) {
    this.setData({ keyword: e.detail.value });
    if (this._keywordTimer) clearTimeout(this._keywordTimer);
    this._keywordTimer = setTimeout(() => this.loadGroups(true), 400);
  },

  clearKeyword() {
    if (this._keywordTimer) clearTimeout(this._keywordTimer);
    this.setData({ keyword: '' });
    this.loadGroups(true);
  },

  onTitleTap(e) {
    const index = Number(e.currentTarget.dataset.index);
    if (index === this.data.titleIndex) return;
    const option = TITLE_OPTIONS[index] || TITLE_OPTIONS[0];
    this.setData({ titleIndex: index, titleValue: option.value });
    this.loadGroups(true);
  },

  retryLoad() {
    this.loadGroups(this.data.pager.status === 'error' && this.data.pager.page > 0 ? false : true);
  },

  loadHospital(hospitalId) {
    wx.request({
      url: app.globalData.baseUrl + '/api/hospitals/' + hospitalId,
      method: 'GET',
      success: (res) => {
        if (res.data) {
          this.setData({
            currentHospital: Object.assign({}, res.data, placeholder.hospitalExtra(res.data))
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
