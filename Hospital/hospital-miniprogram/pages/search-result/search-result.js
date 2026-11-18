const app = getApp();

Page({
  data: {
    keyword: '',
    departments: [],
    doctors: [],
    totalCount: 0,
    loading: false,
    hasSearched: false,
    scheduleMap: {}
  },

  onLoad(options) {
    if (options.keyword) {
      const keyword = decodeURIComponent(options.keyword);
      this.setData({ keyword });
      this.search(keyword);
    }
  },

  onSearchInput(e) {
    this.setData({ keyword: e.detail.value });
  },

  onSearchConfirm() {
    this.onSearch();
  },

  onSearch() {
    if (!this.data.keyword.trim()) {
      wx.showToast({
        title: '请输入搜索关键词',
        icon: 'none'
      });
      return;
    }
    this.search(this.data.keyword);
  },

  search(keyword) {
    console.log('开始搜索，关键词:', keyword);
    console.log('请求URL:', app.globalData.baseUrl + '/api/search');
    
    this.setData({ 
      loading: true, 
      hasSearched: true 
    });

    wx.request({
      url: app.globalData.baseUrl + '/api/search',
      method: 'GET',
      data: { keyword: keyword },
      success: (res) => {
        console.log('搜索响应:', res);
        this.setData({ loading: false });
        
        if (res.statusCode === 200) {
          if (res.data.success) {
            const doctors = res.data.doctors || [];
            this.setData({
              departments: res.data.departments || [],
              doctors: doctors,
              totalCount: res.data.totalCount || 0
            });
            
            this.checkDoctorsSchedule(doctors);
            
            console.log('搜索结果:', {
              departments: res.data.departments,
              doctors: doctors,
              totalCount: res.data.totalCount
            });
          } else {
            console.log('搜索失败:', res.data.message);
            this.setData({
              departments: [],
              doctors: [],
              totalCount: 0,
              scheduleMap: {}
            });
            wx.showToast({
              title: res.data.message || '搜索失败',
              icon: 'none',
              duration: 2000
            });
          }
        } else {
          console.log('HTTP错误，状态码:', res.statusCode);
          this.setData({
            departments: [],
            doctors: [],
            totalCount: 0,
            scheduleMap: {}
          });
          wx.showToast({
            title: '服务器错误',
            icon: 'none'
          });
        }
      },
      fail: (err) => {
        console.error('搜索请求失败:', err);
        this.setData({ loading: false });
        this.setData({
          departments: [],
          doctors: [],
          totalCount: 0,
          scheduleMap: {}
        });
        wx.showToast({
          title: '网络错误',
          icon: 'none'
        });
      }
    });
  },

  checkDoctorsSchedule(doctors) {
    const today = new Date().toISOString().split('T')[0];
    const scheduleMap = {};
    let completedCount = 0;
    
    doctors.forEach(doctor => {
      scheduleMap[doctor.doctorId] = false;
      
      wx.request({
        url: app.globalData.baseUrl + '/api/schedules/doctor/' + doctor.doctorId + '/date/' + today,
        method: 'GET',
        success: (res) => {
          completedCount++;
          let hasSchedule = false;
          
          if (res.data && res.data.success && Array.isArray(res.data.data)) {
            hasSchedule = res.data.data.length > 0;
          } else if (res.data && Array.isArray(res.data)) {
            hasSchedule = res.data.length > 0;
          }
          
          scheduleMap[doctor.doctorId] = hasSchedule;
          
          if (completedCount === doctors.length) {
            this.setData({ scheduleMap });
          }
        },
        fail: () => {
          completedCount++;
          scheduleMap[doctor.doctorId] = false;
          
          if (completedCount === doctors.length) {
            this.setData({ scheduleMap });
          }
        }
      });
    });
  },

  viewDept(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/department-detail/department-detail?id=${id}`
    });
  },

  viewDoctor(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/doctor-detail/doctor-detail?id=${id}`
    });
  },

  makeAppointment(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/appointment/appointment?doctorId=${id}`
    });
  }
})
