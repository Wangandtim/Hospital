/**
 * 长辈关怀模式
 *
 * 开启后：字号整体放大、按钮与卡片加大、隐藏次要信息，
 * 各页面通过根节点 class="elderly" 即可套用全局样式（见 app.wxss 的 .elderly 区段）。
 *
 * 存储键：elderlyMode
 */

const STORAGE_KEY = 'elderlyMode';

function isElderly() {
  try {
    return !!wx.getStorageSync(STORAGE_KEY);
  } catch (e) {
    return false;
  }
}

function setElderly(on) {
  const value = !!on;
  try {
    wx.setStorageSync(STORAGE_KEY, value);
  } catch (e) {
    // 存储失败时仅在本次运行生效
  }

  const app = getApp();
  if (app && app.globalData) {
    app.globalData.elderlyMode = value;
  }
  return value;
}

module.exports = {
  isElderly,
  setElderly
};
