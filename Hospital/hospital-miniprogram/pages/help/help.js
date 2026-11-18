const support = require('../../config/support');

Page({
  data: {
    supportPhone: support.hotline,
    workTime: support.workTime,
    faqs: [
      {
        q: '如何预约挂号？',
        a: '在首页选择医院后，点击医生卡片的「预约」，或在「周边」地图上选择医院点「预约」，按提示选择日期与时段即可。首次使用需要先在「我的 - 患者档案」绑定就诊人。',
        open: false
      },
      {
        q: '预约成功后怎么就诊？',
        a: '请提前 30 分钟到达医院，凭身份证或就诊卡到自助机/窗口取号，然后到对应科室候诊。预约记录可在「我的预约」查看。',
        open: false
      },
      {
        q: '如何取消或改约？',
        a: '进入「我的预约」，找到对应记录点击「取消预约」；如需改约请先取消再用同一医生重新预约。建议至少提前 2 小时操作。',
        open: false
      },
      {
        q: '长辈模式怎么开启？',
        a: '进入「我的 - 设置」，打开「长辈模式」开关即可。开启后字号更大、按钮更醒目，主要页面会自动切换为适老布局，随时可以关闭。',
        open: false
      },
      {
        q: '附近医院不准确怎么办？',
        a: '请在「周边」页点击顶部「重定位」重新获取位置，并确认微信已授权定位权限。也可以在搜索范围内切换更近的距离查看。',
        open: false
      },
      {
        q: '忘记绑定患者信息怎么办？',
        a: '进入「我的 - 绑定患者信息」，填写姓名、身份证号与手机号完成绑定。一个账号可以绑定多位家庭成员。',
        open: false
      }
    ]
  },

  toggleFaq(e) {
    const index = Number(e.currentTarget.dataset.index);
    const key = `faqs[${index}].open`;
    this.setData({ [key]: !this.data.faqs[index].open });
  },

  callSupport() {
    wx.makePhoneCall({
      phoneNumber: support.hotline,
      fail: () => {
        wx.showToast({ title: '拨号已取消', icon: 'none' });
      }
    });
  },

  goContact() {
    wx.navigateTo({ url: '/pages/contact/contact' });
  },

  goSettings() {
    wx.navigateTo({ url: '/pages/settings/settings' });
  }
});
