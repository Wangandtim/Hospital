/**
 * 全局接口配置（唯一来源，所有页面/请求层都从这里取地址）
 *
 * 地址优先级：本地覆盖 API_BASE > ext.json 的 apiBase > 编译环境默认值
 *
 * 正式版没有把域名写死在仓库里，避免误用占位地址：
 *   1. 优先在 ext.json 中配置 { "apiBase": "https://你的正式域名" }；
 *   2. 或在发布前填写下方 RELEASE_API_BASE；
 *   3. 都没有时 release 构建会明确报错，不会静默请求占位域名。
 *   还需要在微信公众平台「开发设置 - 服务器域名」把该域名加入 request 合法域名。
 *
 * 真机调试（手机和电脑需在同一局域网）：
 *   wx.setStorageSync('API_BASE', 'http://192.168.1.10:18080')
 * 恢复默认：wx.removeStorageSync('API_BASE')
 */
// 正式版接口地址：上线前把这里改成正式 HTTPS 域名（占位示例：https://api.example-hospital.com）
// 也可以不改这里，改为在 ext.json 中配置 { "apiBase": "https://你的正式域名" }
const RELEASE_API_BASE = '';
const EXT_API_BASE_KEY = 'apiBase';

const ENV_API_BASE = {
  // 本地调试：当前后端实例端口（8080，主库 3306）
  develop: 'http://localhost:8080',
  // 体验版：联调阶段可先指向测试机，正式提审前替换
  trial: 'http://localhost:8080',
  // 正式版：读取上方 RELEASE_API_BASE
  release: RELEASE_API_BASE
};

const OVERRIDE_KEY = 'API_BASE';

function detectEnvVersion() {
  try {
    const info = wx.getAccountInfoSync();
    if (info && info.miniProgram && info.miniProgram.envVersion) {
      return info.miniProgram.envVersion;
    }
  } catch (e) {
    // 低版本基础库或不支持该 API 时按开发环境处理
  }
  return 'develop';
}

function normalize(url) {
  return String(url || '').trim().replace(/\/+$/, '');
}

function readExtApiBase() {
  try {
    if (typeof wx.getExtConfigSync !== 'function') {
      return '';
    }
    const ext = wx.getExtConfigSync();
    const value = ext && (ext[EXT_API_BASE_KEY] || ext.api_base);
    return normalize(value);
  } catch (e) {
    return '';
  }
}

function resolveApiBase() {
  try {
    const override = normalize(wx.getStorageSync(OVERRIDE_KEY));
    if (override) {
      return override;
    }
  } catch (e) {
    // 存储不可用时继续回退
  }
  const extBase = readExtApiBase();
  if (extBase) {
    return extBase;
  }
  return normalize(ENV_API_BASE[detectEnvVersion()] || ENV_API_BASE.develop);
}

const envVersion = detectEnvVersion();
const apiBase = resolveApiBase();
const releaseUnconfigured = envVersion === 'release' && !apiBase;

if (releaseUnconfigured) {
  console.error('[config] 正式环境接口地址未配置，请填写 RELEASE_API_BASE');
}

module.exports = {
  apiBase,
  envVersion,
  releaseUnconfigured,
  requestTimeout: 10000,
  retry: 1,
  pageSize: 10
};
