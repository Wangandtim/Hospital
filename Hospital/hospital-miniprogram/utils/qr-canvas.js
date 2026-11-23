const qrcode = require('./qrcode');

/**
 * 在旧版 canvas 上绘制二维码。
 * @param {string} canvasId 画布 id
 * @param {string} text 二维码内容
 * @param {object} scope 页面实例
 * @param {number} size 绘制边长（逻辑 px）
 */
function draw(canvasId, text, scope, size) {
  const qr = qrcode(0, 'M');
  qr.addData(text);
  qr.make();

  const count = qr.getModuleCount();
  const margin = 4;
  const cell = Math.max(2, Math.floor(size / (count + margin * 2)));
  const offset = Math.floor((size - cell * count) / 2);

  const ctx = wx.createCanvasContext(canvasId, scope);
  ctx.setFillStyle('#FFFFFF');
  ctx.fillRect(0, 0, size, size);
  ctx.setFillStyle('#171D1B');

  for (let row = 0; row < count; row++) {
    for (let col = 0; col < count; col++) {
      if (qr.isDark(row, col)) {
        ctx.fillRect(offset + col * cell, offset + row * cell, cell, cell);
      }
    }
  }
  ctx.draw();
}

module.exports = {
  draw: draw
};
