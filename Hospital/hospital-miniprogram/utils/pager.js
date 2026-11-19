/**
 * 统一分页加载器（患者端所有长列表复用，禁止各页各写一套 onReachBottom）
 * ------------------------------------------------------------------
 * 后端口径：page 从 0 开始；size 默认 20、上限 100；
 * 响应：total、totalPages、hasMore；列表字段兼容 content/list/rows/records/data，
 * 总数兼容 total/totalElements，是否还有下一页兼容 hasMore/hasNext。
 *
 * 用法：
 *   const pager = require('../../utils/pager');
 *   this.pager = pager.create({
 *     size: 20,
 *     fetch: function (page, size) {
 *       return request.get('/api/xxx/page', { page: page, size: size });
 *     },
 *     onData: function (state) {
 *       this.setData({ list: state.list, pager: state.pager });
 *     },
 *     onReset: function () { this.setData({ list: [] }); }
 *   });
 *   // 页内：onLoad -> this.pager.load(); onReachBottom -> this.pager.next();
 *   // 切换医院/日期 -> this.pager.reset({ hospitalId: xxx });
 *   // 失败重试 -> this.pager.retry();
 *   // onUnload -> this.pager.destroy();
 *
 * state.pager 供 ui-pager 组件直接使用：
 *   { status, hasMore, total, page, loading, error }
 *   status: idle | loading | loadingMore | more | empty | error | done
 */

const DEFAULT_SIZE = 20;
const MAX_SIZE = 100;

function clampSize(size) {
  const value = Number(size) || DEFAULT_SIZE;
  return Math.max(1, Math.min(MAX_SIZE, value));
}

function normalize(res) {
  // 兼容 { data: 列表 } 与 { data: { content: 列表 } } 两种包装，避免把列表本身再解包成 undefined
  const payload = res && res.data !== undefined && !Array.isArray(res.data) && !Array.isArray(res)
    ? res.data
    : res;
  const data = payload || {};
  const candidates = [
    data.content,
    data.list,
    data.rows,
    data.records,
    Array.isArray(data.data) ? data.data : null
  ].filter(Array.isArray);
  const list = candidates.find(item => item.length > 0) || candidates[0] || [];
  const totalRaw = data.total !== undefined ? data.total : data.totalElements;
  const total = totalRaw !== undefined && totalRaw !== null ? Number(totalRaw) : null;
  let hasMore;
  if (data.hasMore !== undefined) {
    hasMore = Boolean(data.hasMore);
  } else if (data.hasNext !== undefined) {
    hasMore = Boolean(data.hasNext);
  } else if (data.totalPages !== undefined && data.totalPages !== null) {
    hasMore = readPage(data) + 1 < Number(data.totalPages);
  } else if (total !== null) {
    hasMore = readPage(data) * (data.size || DEFAULT_SIZE) + list.length < total;
  } else {
    hasMore = list.length >= (data.size || DEFAULT_SIZE);
  }
  return {
    list: list,
    total: total,
    totalPages: data.totalPages !== undefined ? Number(data.totalPages) : null,
    hasMore: hasMore,
    page: data.page !== undefined ? Number(data.page) : null
  };
}

function readPage(data) {
  if (data.page !== undefined && data.page !== null) return Number(data.page);
  if (data.currentPage !== undefined && data.currentPage !== null) return Number(data.currentPage);
  if (data.number !== undefined && data.number !== null) return Number(data.number);
  return 0;
}

function create(options) {
  const opts = options || {};
  const size = clampSize(opts.size);
  const pageSource = { value: 0 };
  let seq = 0;
  let destroyed = false;
  let state = {
    list: [],
    page: 0,
    total: null,
    totalPages: null,
    hasMore: true,
    status: 'idle',
    loading: false,
    error: '',
    size: size
  };

  function pagerView() {
    return {
      status: state.status,
      hasMore: state.hasMore,
      total: state.total,
      page: state.page,
      loading: state.loading,
      error: state.error,
      size: state.size
    };
  }

  function emit() {
    if (destroyed) return;
    const payload = {
      list: state.list,
      pager: pagerView(),
      total: state.total,
      hasMore: state.hasMore,
      page: state.page
    };
    if (typeof opts.onData === 'function') {
      opts.onData.call(opts.scope || null, payload);
    }
  }

  function setStatus(status) {
    state.status = status;
    state.loading = status === 'loading' || status === 'loadingMore';
  }

  function loadPage(targetPage, append) {
    if (destroyed) return Promise.resolve(state.list);
    if (state.loading) return Promise.resolve(state.list);

    const currentSeq = ++seq;
    setStatus(append && state.list.length ? 'loadingMore' : 'loading');
    state.error = '';
    emit();

    let promise;
    try {
      promise = opts.fetch(targetPage, size);
    } catch (error) {
      promise = Promise.reject(error);
    }

    return Promise.resolve(promise)
      .then(res => {
        // 切换筛选条件后旧请求回来，直接丢弃，避免串页
        if (currentSeq !== seq || destroyed) return state.list;
        const parsed = normalize(res);
        const incoming = parsed.list || [];
        state.page = targetPage;
        state.total = parsed.total;
        state.totalPages = parsed.totalPages;
        state.hasMore = parsed.hasMore;
        state.list = append && state.list.length ? state.list.concat(incoming) : incoming;
        if (!state.list.length) {
          setStatus('empty');
        } else if (!state.hasMore) {
          setStatus('done');
        } else {
          setStatus('more');
        }
        emit();
        return state.list;
      })
      .catch(error => {
        if (currentSeq !== seq || destroyed) return state.list;
        state.error = (error && (error.errMsg || error.message)) || '加载失败，请重试';
        setStatus('error');
        emit();
        return state.list;
      });
  }

  return {
    /** 重置到第一页并丢弃进行中的旧请求（切换医院/日期/筛选时调用） */
    reset(context) {
      pageSource.value = 0;
      seq += 1;
      state = {
        list: [],
        page: 0,
        total: null,
        totalPages: null,
        hasMore: true,
        status: 'idle',
        loading: false,
        error: '',
        size: size
      };
      if (typeof opts.onReset === 'function') {
        opts.onReset.call(opts.scope || null, context);
      }
      emit();
      return loadPage(0, false);
    },
    /** 首次加载 / 上拉加载更多 */
    load() {
      if (state.hasMore || state.status === 'idle' || state.status === 'error') {
        return loadPage(state.page, false);
      }
      return Promise.resolve(state.list);
    },
    next() {
      if (!state.hasMore || state.loading || state.status === 'idle' || state.status === 'error') {
        return Promise.resolve(state.list);
      }
      return loadPage(state.page + 1, true);
    },
    retry() {
      return loadPage(state.status === 'more' || state.page > 0 ? state.page : 0, state.page > 0);
    },
    destroy() {
      destroyed = true;
      seq += 1;
    },
    getState() {
      return Object.assign({ list: state.list.slice() }, pagerView());
    }
  };
}

/**
 * 一次性聚合多页（用于「需要全部号源判断能否预约」等无上拉场景）
 * 仍按 page/size/total/hasMore 翻页，避免借用 size=200 被后端截断。
 *   pager.fetchAll(function (page, size) { return request.get(url, { page, size }); }, { size: 100 })
 */
function fetchAll(fetch, options) {
  const opts = options || {};
  const size = clampSize(opts.size || MAX_SIZE);
  const maxPages = opts.maxPages || 100;
  const collected = [];

  function step(pageNo) {
    return Promise.resolve(fetch(pageNo, size)).then(res => {
      const parsed = normalize(res);
      if (parsed.list && parsed.list.length) {
        parsed.list.forEach(item => collected.push(item));
      }
      const more = parsed.hasMore && pageNo + 1 < maxPages;
      return more ? step(pageNo + 1) : collected;
    });
  }

  return step(0);
}

module.exports = {
  create: create,
  fetchAll: fetchAll,
  clampSize: clampSize,
  normalize: normalize,
  DEFAULT_SIZE: DEFAULT_SIZE,
  MAX_SIZE: MAX_SIZE
};
