/**
 * 统一请求层
 * - 统一 baseUrl / 超时 / 重试
 * - 统一错误提示（避免各页面重复 wx.showToast）
 * - 统一返回体：resolve 时直接给出后端 data
 *
 * 用法：
 *   const http = require('../../utils/request');
 *   http.get('/api/hospitals').then(list => ...).catch(err => ...);
 *   http.post('/api/appointments', body, { silent: true });
 */
const config = require('../config/index');

let reLaunching = false;

function buildUrl(url) {
  if (/^https?:\/\//.test(url)) return url;
  if (!config.apiBase) return url;
  return config.apiBase + (url.charAt(0) === '/' ? url : '/' + url);
}

function toast(message) {
  wx.showToast({ title: message || '网络异常', icon: 'none', duration: 2000 });
}

/** 把 wx.request 的原始 errMsg 转成用户能看懂的提示 */
function friendlyMessage(error) {
  const msg = (error && error.message) || '';
  if (msg.indexOf('timeout') >= 0) {
    return '请求超时，请稍后重试';
  }
  if (msg.indexOf('request:fail') >= 0) {
    return '网络异常，请检查网络后重试';
  }
  return msg || '网络异常，请稍后重试';
}

function authHeader() {
  try {
    const token = wx.getStorageSync('authToken');
    return token ? { 'X-Auth-Token': token } : {};
  } catch (e) {
    return {};
  }
}

function once(options) {
  if (config.releaseUnconfigured) {
    const err = new Error('正式环境接口地址未配置，请联系管理员');
    if (!options.silent) {
      toast(err.message);
    }
    return Promise.reject(err);
  }
  return new Promise((resolve, reject) => {
    wx.request({
      url: buildUrl(options.url),
      method: (options.method || 'GET').toUpperCase(),
      data: options.data || {},
      header: Object.assign({ 'Content-Type': 'application/json' }, authHeader(), options.header || {}),
      timeout: options.timeout || config.requestTimeout,
      success(res) {
        if (res.statusCode === 401) {
          try {
            wx.removeStorageSync('authToken');
            wx.removeStorageSync('token');
            wx.removeStorageSync('userRole');
          } catch (e) { /* 忽略存储异常 */ }
          try {
            const app = getApp();
            if (app && app.globalData) {
              app.globalData.token = null;
              app.globalData.userRole = null;
            }
          } catch (e) { /* 忽略全局状态异常 */ }
          toast('登录已失效，请重新登录');
          if (!reLaunching) {
            reLaunching = true;
            setTimeout(() => {
              wx.reLaunch({
                url: '/pages/login/login',
                complete: () => { reLaunching = false; }
              });
            }, 900);
          }
          const err = new Error('未登录或登录已失效');
          err.response = res.data;
          err.code = 401;
          reject(err);
          return;
        }
        if (res.statusCode >= 200 && res.statusCode < 300) {
          const body = res.data;
          if (body && typeof body === 'object' && body.success === false) {
            const err = new Error(body.message || '请求失败');
            err.response = body;
            reject(err);
            return;
          }
          resolve(body);
          return;
        }
        const err = new Error('HTTP ' + res.statusCode);
        err.response = res.data;
        reject(err);
      },
      fail(err) {
        reject(new Error(err && err.errMsg ? err.errMsg : '网络请求失败'));
      }
    });
  });
}

function request(options) {
  const retry = options.retry === undefined ? config.retry : options.retry;
  let attempt = 0;
  // 重试次数必须真正递减，否则重试后的失败会被吞掉，页面既没有结果也没有错误提示
  const run = () => once(options).catch(error => {
    if (attempt < retry) {
      attempt += 1;
      return run();
    }
    if (!options.silent) {
      toast(friendlyMessage(error));
    }
    throw error;
  });
  return run();
}

module.exports = {
  request,
  get(url, data, options) {
    return request(Object.assign({ url, data, method: 'GET' }, options || {}));
  },
  post(url, data, options) {
    return request(Object.assign({ url, data, method: 'POST' }, options || {}));
  },
  put(url, data, options) {
    return request(Object.assign({ url, data, method: 'PUT' }, options || {}));
  },
  del(url, data, options) {
    return request(Object.assign({ url, data, method: 'DELETE' }, options || {}));
  },
  buildUrl
};
