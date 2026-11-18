const app = getApp();

Page({
  data: {
    doctors: [],
    deptId: '',
    hospitalId: '',
    currentHospital: null,
    scheduleMap: {}
  },

  onLoad(options) {
    console.log('医生列表页面加载，参数:', options);
    if (options.hospitalId) {
      this.setData({ hospitalId: options.hospitalId });
      this.loadDoctorsByHospital(options.hospitalId);
    } else if (options.deptId) {
      this.setData({ deptId: options.deptId });
      this.loadDoctorsByDept(options.deptId);
    } else {
      wx.showToast({
        title: '未指定医院或科室',
        icon: 'none'
      });
    }
  },

  loadDoctorsByHospital(hospitalId) {
    console.log('加载医生，医院ID:', hospitalId);
    wx.showLoading({ title: '加载中...' });
    
    wx.request({
      url: app.globalData.baseUrl + '/api/doctors/hospital/' + hospitalId,
      method: 'GET',
      success: (res) => {
        wx.hideLoading();
        console.log('获取医生成功:', res.data);
        if (res.data && Array.isArray(res.data)) {
          this.setData({ doctors: res.data });
          this.checkDoctorsSchedule(res.data);
          
          // 获取医院信息
          wx.request({
            url: app.globalData.baseUrl + '/api/hospitals/' + hospitalId,
            method: 'GET',
            success: (hospitalRes) => {
              if (hospitalRes.data) {
                this.setData({ currentHospital: hospitalRes.data });
              }
            }
          });
        }
      },
      fail: (err) => {
        wx.hideLoading();
        console.error('获取医生失败:', err);
        wx.showToast({
          title: '获取医生失败',
          icon: 'none'
        });
      }
    });
  },

  loadDoctorsByDept(deptId) {
    console.log('加载医生，科室ID:', deptId);
    wx.showLoading({ title: '加载中...' });
    
    wx.request({
      url: app.globalData.baseUrl + '/api/doctors/dept/' + deptId,
      method: 'GET',
      success: (res) => {
        wx.hideLoading();
        if (res.data && Array.isArray(res.data)) {
          this.setData({ doctors: res.data });
          this.checkDoctorsSchedule(res.data);
        }
      },
      fail: (err) => {
        wx.hideLoading();
        console.error('获取医生失败:', err);
        wx.showToast({
          title: '获取医生失败',
          icon: 'none'
        });
      }
    });
  },

  checkDoctorsSchedule(doctors) {
    if (!doctors || doctors.length === 0) {
      this.setData({ scheduleMap: {} });
      return;
    }

    const now = new Date();
    const today = now.getFullYear() + '-' +
      String(now.getMonth() + 1).padStart(2, '0') + '-' +
      String(now.getDate()).padStart(2, '0');

    const scheduleMap = {};
    doctors.forEach(doctor => {
      scheduleMap[doctor.doctorId] = false;
    });

    const hospitalId = this.data.hospitalId || (doctors[0] && doctors[0].hospitalId);
    if (!hospitalId) {
      this.setData({ scheduleMap });
      return;
    }

    // 一次拉取全院当日排班，替代原先“每个医生一次请求”的 N 次网络往返
    wx.request({
      url: app.globalData.baseUrl + '/api/schedules/hospital/' + hospitalId + '/date/' + today,
      method: 'GET',
      success: (res) => {
        const list = res.data && res.data.success
          ? (res.data.data || [])
          : (Array.isArray(res.data) ? res.data : []);
        list.forEach(item => {
          if (item && item.doctorId && item.doctorId in scheduleMap) {
            scheduleMap[item.doctorId] = true;
          }
        });
        this.setData({ scheduleMap });
      },
      fail: () => {
        this.setData({ scheduleMap });
      }
    });
  },

  hasSchedule(doctorId) {
    return this.data.scheduleMap[doctorId] === true;
  },

  makeAppointment(e) {
    const doctorId = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/appointment/appointment?doctorId=${doctorId}&hospitalId=${this.data.hospitalId}`
    });
  }
})