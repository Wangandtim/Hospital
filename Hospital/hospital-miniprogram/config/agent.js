/**
 * Agent 接入配置（预留）
 *
 * 后续接入大模型 / 智能体时：
 *   1. 把 endpoint 改为网关地址；
 *   2. 把 enabled 改为 true；
 *   3. 如需鉴权，补充 headers（例如 Authorization: Bearer xxx）。
 *
 * 小程序正式环境要求 https，并且需要把域名加入「request 合法域名」。
 */
module.exports = {
  enabled: false,
  endpoint: '',
  headers: {},
  // 预留场景：首页导诊、预约辅助、报告解读、用药提醒
  scenes: {
    home: 'patient_home',
    appointment: 'appointment_assist',
    report: 'report_explain',
    medication: 'medication_reminder'
  }
};
