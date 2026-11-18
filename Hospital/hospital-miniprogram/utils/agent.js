/**
 * Agent 能力接入层（预留接口）
 *
 * 页面/业务方只依赖这里暴露的方法，后续接入真实智能体时只改本文件，
 * 不用改调用方。约定：
 *   - chat(options)   : 单轮/多轮对话，返回 Promise<{ text, actions }>
 *   - suggest(options): 场景化快捷建议（未接入时返回本地兜底文案）
 *   - on(event, cb)   : 订阅智能体事件（如需要主动提醒）
 *
 * 未配置 config/agent.js 时所有网络方法会抛出 AGENT_NOT_CONFIGURED，
 * 调用方应捕获并降级为本地功能。
 */

const agentConfig = require('../config/agent');

const ERROR_NOT_CONFIGURED = 'AGENT_NOT_CONFIGURED';
const listeners = {};

class AgentClient {
  constructor(config) {
    this.config = config;
    this.draft = {
      scene: null,
      context: {}
    };
  }

  /** 是否已配置可用的 Agent 网关 */
  isReady() {
    return !!(this.config && this.config.enabled && this.config.endpoint);
  }

  /** 运行时更新配置（例如切换到测试网关） */
  configure(options) {
    this.config = Object.assign({}, this.config, options || {});
    return this.isReady();
  }

  /** 记录当前页面上下文，供后续对话使用 */
  setContext(context) {
    this.draft.context = Object.assign({}, this.draft.context, context || {});
    return this.draft.context;
  }

  /**
   * 发送对话消息
   * @param {Object} options
   * @param {string} options.scene 场景（见 config/agent.js scenes）
   * @param {string} options.text 用户输入
   * @param {Array}  [options.messages] 历史消息，格式 [{role:'user'|'assistant', content:'...'}]
   * @param {Object} [options.context] 业务上下文（医院、科室、患者信息等，注意脱敏）
   * @returns {Promise<{text: string, actions: Array}>}
   */
  chat(options) {
    const payload = {
      scene: (options && options.scene) || this.config.scenes.home,
      text: (options && options.text) || '',
      messages: (options && options.messages) || [],
      context: Object.assign({}, this.draft.context, (options && options.context) || {})
    };

    return new Promise((resolve, reject) => {
      if (!this.isReady()) {
        reject({
          code: ERROR_NOT_CONFIGURED,
          message: '智能助手尚未接入，请稍后再试'
        });
        return;
      }

      wx.request({
        url: this.config.endpoint,
        method: 'POST',
        header: Object.assign({ 'content-type': 'application/json' }, this.config.headers || {}),
        data: payload,
        success: (res) => {
          if (res.statusCode === 200 && res.data) {
            resolve({
              text: res.data.text || '',
              actions: Array.isArray(res.data.actions) ? res.data.actions : []
            });
          } else {
            reject({ code: 'AGENT_BAD_RESPONSE', message: '智能助手响应异常' });
          }
        },
        fail: (err) => {
          reject({ code: 'AGENT_REQUEST_FAILED', message: '网络异常', detail: err });
        }
      });
    });
  }

  /**
   * 场景化快捷建议；未接入时用本地兜底，保证 UI 可用
   * @returns {Promise<Array<{label: string, action: string}>>}
   */
  suggest(options) {
    const scene = (options && options.scene) || '';
    const fallback = {
      patient_home: [
        { label: '帮我找科室', action: 'find_department' },
        { label: '查看我的预约', action: 'view_appointments' },
        { label: '怎么使用长辈模式', action: 'elderly_guide' }
      ],
      appointment_assist: [
        { label: '怎么取消预约', action: 'cancel_guide' },
        { label: '就诊要带什么', action: 'visit_guide' }
      ],
      report_explain: [
        { label: '指标异常怎么办', action: 'abnormal_guide' },
        { label: '报告在哪里查看', action: 'report_guide' }
      ],
      medication_reminder: [
        { label: '设置服药提醒', action: 'medication_setting' }
      ]
    };

    if (!this.isReady()) {
      return Promise.resolve(fallback[scene] || fallback.patient_home);
    }

    return this.chat({
      scene: scene,
      text: '',
      context: Object.assign({ intent: 'suggest' }, (options && options.context) || {})
    }).then((res) => res.actions || []);
  }

  /** 订阅事件（voice-input、reminder、session 等），预留给后续主动提醒 */
  on(event, callback) {
    if (typeof callback !== 'function') return;
    listeners[event] = listeners[event] || [];
    listeners[event].push(callback);
  }

  off(event, callback) {
    if (!listeners[event]) return;
    listeners[event] = listeners[event].filter((cb) => cb !== callback);
  }

  emit(event, payload) {
    (listeners[event] || []).forEach((cb) => {
      try {
        cb(payload);
      } catch (e) {
        console.warn('[Agent] 事件处理失败', e);
      }
    });
  }
}

module.exports = new AgentClient(agentConfig);
module.exports.ERROR_NOT_CONFIGURED = ERROR_NOT_CONFIGURED;
