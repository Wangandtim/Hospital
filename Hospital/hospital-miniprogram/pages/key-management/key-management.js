const app = getApp();

Page({
  data: {
    hospitalId: '',
    hospitalName: '',
    currentKey: null,
    allKeys: [],
    hospitals: {},
    hospitalOptions: [],
    hospitalIndex: 0,
    selectedHospitalId: '',
    selectedHospitalName: '',
    adminRole: '',
    emergencyLocked: false,
    emergencyLockReason: '',
    todayWrites: 0,
    emergencyRequests: [],
    recyclables: [],
    snapshots: []
  },

  onLoad(options) {
    this.setData({ adminRole: wx.getStorageSync('adminRole') || '' });
    if (options.hospitalId) {
      this.setData({ hospitalId: options.hospitalId });
      this.loadHospitalKey();
      this.loadHospitalName();
      this.loadEmergencyStatus();
    } else {
      this.loadAllKeys();
      this.loadHospitals();
    }
  },

  loadHospitalName() {
    wx.request({
      url: app.globalData.baseUrl + '/api/hospitals/' + this.data.hospitalId,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.hospitalName) {
          this.setData({ hospitalName: res.data.hospitalName });
        }
      }
    });
  },

  loadHospitalKey() {
    wx.request({
      url: app.globalData.baseUrl + '/api/keys/hospital/' + this.data.hospitalId,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.success) {
          this.setData({ currentKey: res.data });
        }
      }
    });
  },

  loadEmergencyStatus() {
    if (!this.data.hospitalId) return;
    wx.request({
      url: app.globalData.baseUrl + '/api/emergency/status/' + this.data.hospitalId,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.success) {
          this.setData({
            emergencyLocked: !!res.data.locked,
            emergencyLockReason: res.data.lockReason || '',
            todayWrites: res.data.todayWrites || 0
          });
        }
      }
    });
    this.loadSnapshots();
    this.loadEmergencyRequests();
    this.loadRecycleBin();
  },

  loadRecycleBin() {
    if (!this.data.hospitalId) return;
    const items = [];
    wx.request({
      url: app.globalData.baseUrl + '/api/departments/deleted/' + this.data.hospitalId,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.success) {
          (res.data.data || []).forEach(dept => {
            items.push({
              type: 'department',
              id: dept.deptId,
              label: '科室：' + (dept.deptName || dept.name || dept.deptId),
              deleteTime: dept.deleteTime || ''
            });
          });
        }
        this.loadDeletedSchedules(items);
      },
      fail: () => this.loadDeletedSchedules(items)
    });
  },

  loadDeletedSchedules(items) {
    wx.request({
      url: app.globalData.baseUrl + '/api/schedules/deleted/' + this.data.hospitalId,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.success) {
          (res.data.data || []).forEach(schedule => {
            items.push({
              type: 'schedule',
              id: schedule.scheduleId,
              label: '排班：' + (schedule.workDate || '') + ' ' + (schedule.timeSlot || ''),
              deleteTime: schedule.deleteTime || ''
            });
          });
        }
        this.setData({ recyclables: items });
      },
      fail: () => this.setData({ recyclables: items })
    });
  },

  restoreRecyclable(e) {
    const type = e.currentTarget.dataset.type;
    const id = e.currentTarget.dataset.id;
    if (!type || !id) return;
    wx.showModal({
      title: '恢复数据',
      content: '将该记录恢复到删除前状态，确定继续吗？',
      confirmText: '恢复',
      cancelText: '取消',
      success: (modalRes) => {
        if (!modalRes.confirm) return;
        const url = type === 'department'
          ? '/api/departments/' + id + '/restore'
          : '/api/schedules/' + id + '/restore';
        wx.showLoading({ title: '恢复中...' });
        wx.request({
          url: app.globalData.baseUrl + url,
          method: 'POST',
          success: (res) => {
            wx.hideLoading();
            if (res.data && res.data.success) {
              wx.showToast({ title: '已恢复', icon: 'success' });
              this.loadRecycleBin();
            } else {
              wx.showModal({
                title: '恢复失败',
                content: (res.data && res.data.message) || '请稍后重试。',
                showCancel: false
              });
            }
          },
          fail: () => {
            wx.hideLoading();
            wx.showModal({
              title: '网络超时',
              content: '恢复请求失败，请检查网络后重试。',
              showCancel: false
            });
          }
        });
      }
    });
  },

  loadEmergencyRequests() {
    if (!this.data.hospitalId) return;
    wx.request({
      url: app.globalData.baseUrl + '/api/emergency/requests/' + this.data.hospitalId,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.success) {
          this.setData({ emergencyRequests: res.data.data || [] });
        }
      }
    });
  },

  previewEmergencyCert(e) {
    const requestId = e.currentTarget.dataset.requestId;
    const type = e.currentTarget.dataset.type;
    if (!requestId || !type) return;
    const authToken = wx.getStorageSync('authToken') || wx.getStorageSync('token') || '';

    wx.showLoading({ title: '加载证件...' });
    wx.downloadFile({
      url: app.globalData.baseUrl + '/api/emergency/cert/' + requestId + '/' + type,
      header: authToken ? { 'X-Auth-Token': authToken } : {},
      success: (res) => {
        wx.hideLoading();
        if (res.statusCode === 200 && res.tempFilePath) {
          wx.previewImage({ urls: [res.tempFilePath] });
        } else {
          wx.showModal({
            title: '无法查看',
            content: '证件不存在或当前账号无权查看。',
            showCancel: false
          });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showModal({
          title: '网络超时',
          content: '证件加载失败，请检查网络后重试。',
          showCancel: false
        });
      }
    });
  },

  verifyEmergencyCert(e) {
    const requestId = e.currentTarget.dataset.id;
    if (!requestId) return;

    wx.showActionSheet({
      itemList: ['证件与本人一致', '证件与本人不一致'],
      success: (sheetRes) => {
        const verified = sheetRes.tapIndex === 0;
        wx.showLoading({ title: '提交中...' });
        wx.request({
          url: app.globalData.baseUrl + '/api/emergency/request/' + requestId + '/verify-cert',
          method: 'POST',
          data: { verified: verified, remark: verified ? '院管核对一致' : '院管核对不一致' },
          success: (res) => {
            wx.hideLoading();
            if (res.data && res.data.success) {
              wx.showToast({ title: '已记录核对结论', icon: 'success' });
              this.loadEmergencyRequests();
            } else {
              wx.showModal({
                title: '操作失败',
                content: (res.data && res.data.message) || '请稍后重试。',
                showCancel: false
              });
            }
          },
          fail: () => {
            wx.hideLoading();
            wx.showModal({ title: '网络超时', content: '请检查网络后重试。', showCancel: false });
          }
        });
      }
    });
  },

  terminateEmergencyRequest(e) {
    const requestId = e.currentTarget.dataset.id;
    if (!requestId) return;
    wx.showModal({
      title: '终止应急权限',
      content: '终止后该医生已获得的应急令牌立即失效，确定继续吗？',
      confirmText: '终止',
      cancelText: '取消',
      success: (modalRes) => {
        if (!modalRes.confirm) return;
        wx.showLoading({ title: '提交中...' });
        wx.request({
          url: app.globalData.baseUrl + '/api/emergency/request/' + requestId + '/terminate',
          method: 'POST',
          success: (res) => {
            wx.hideLoading();
            if (res.data && res.data.success) {
              wx.showToast({ title: '已终止', icon: 'success' });
              this.loadEmergencyRequests();
            } else {
              wx.showModal({
                title: '操作失败',
                content: (res.data && res.data.message) || '请稍后重试。',
                showCancel: false
              });
            }
          },
          fail: () => {
            wx.hideLoading();
            wx.showModal({ title: '网络超时', content: '请检查网络后重试。', showCancel: false });
          }
        });
      }
    });
  },

  loadSnapshots() {
    wx.request({
      url: app.globalData.baseUrl + '/api/emergency/snapshots/' + this.data.hospitalId,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.success) {
          this.setData({ snapshots: (res.data.data || []).slice(0, 10) });
        }
      }
    });
  },

  restoreSnapshot(e) {
    const snapshotId = e.currentTarget.dataset.id;
    wx.showModal({
      title: '回滚数据',
      content: '将把该科室/排班恢复到快照时的状态，确定继续吗？',
      confirmText: '回滚',
      cancelText: '取消',
      success: (modalRes) => {
        if (!modalRes.confirm) return;
        wx.showLoading({ title: '回滚中...' });
        wx.request({
          url: app.globalData.baseUrl + '/api/emergency/snapshots/' + snapshotId + '/restore',
          method: 'POST',
          success: (res) => {
            wx.hideLoading();
            if (res.data && res.data.success) {
              wx.showToast({ title: '已回滚', icon: 'success' });
              this.loadSnapshots();
            } else {
              wx.showModal({
                title: '回滚失败',
                content: (res.data && res.data.message) || '请稍后重试。',
                showCancel: false
              });
            }
          },
          fail: () => {
            wx.hideLoading();
            wx.showModal({
              title: '网络超时',
              content: '回滚请求失败，请检查网络后重试。',
              showCancel: false
            });
          }
        });
      }
    });
  },

  freezeEmergency() {
    wx.showModal({
      title: '冻结本院应急权限',
      content: '所有应急管理员和医生应急申请会立即失效，已登录的应急会话会被强制退出，冻结 24 小时。确定继续吗？',
      confirmText: '立即冻结',
      cancelText: '取消',
      success: (modalRes) => {
        if (!modalRes.confirm) return;
        wx.request({
          url: app.globalData.baseUrl + '/api/emergency/revoke-all/' + this.data.hospitalId,
          method: 'POST',
          success: (res) => {
            if (res.data && res.data.success) {
              wx.showToast({ title: '已冻结', icon: 'success' });
              this.loadEmergencyStatus();
            } else {
              wx.showModal({
                title: '冻结失败',
                content: (res.data && res.data.message) || '请稍后重试。',
                showCancel: false
              });
            }
          },
          fail: () => {
            wx.showModal({
              title: '网络超时',
              content: '冻结请求失败，请检查网络后重试。',
              showCancel: false
            });
          }
        });
      }
    });
  },

  unfreezeEmergency() {
    wx.showModal({
      title: '解除应急冻结',
      content: '解除后本院可重新使用应急密钥或医生申请。确定解除吗？',
      confirmText: '解除',
      cancelText: '取消',
      success: (modalRes) => {
        if (!modalRes.confirm) return;
        wx.request({
          url: app.globalData.baseUrl + '/api/emergency/unfreeze/' + this.data.hospitalId,
          method: 'POST',
          success: (res) => {
            if (res.data && res.data.success) {
              wx.showToast({ title: '已解除', icon: 'success' });
              this.loadEmergencyStatus();
            } else {
              wx.showModal({
                title: '解除失败',
                content: (res.data && res.data.message) || '请稍后重试。',
                showCancel: false
              });
            }
          },
          fail: () => {
            wx.showModal({
              title: '网络超时',
              content: '解除请求失败，请检查网络后重试。',
              showCancel: false
            });
          }
        });
      }
    });
  },

  loadAllKeys() {
    wx.request({
      url: app.globalData.baseUrl + '/api/keys/all',
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.success) {
          const list = (res.data.data || []).map(item => ({
            ...item,
            statusText: item.status === 1 ? '有效' : '无效'
          }));
          this.setData({ allKeys: list });
        }
      }
    });
  },

  loadHospitals() {
    wx.request({
      url: app.globalData.baseUrl + '/api/hospitals',
      method: 'GET',
      success: (res) => {
        const hospitals = {};
        const options = [];
        if (res.data && Array.isArray(res.data)) {
          res.data.forEach(hospital => {
            hospitals[hospital.hospitalId] = hospital;
            options.push({
              hospitalId: hospital.hospitalId,
              hospitalName: hospital.hospitalName
            });
          });
        }
        const allKeys = (this.data.allKeys || []).map(item => ({
          ...item,
          hospitalNameText: (hospitals[item.hospitalId] && hospitals[item.hospitalId].hospitalName) || item.hospitalId
        }));
        this.setData({
          hospitals: hospitals,
          hospitalOptions: options,
          allKeys: allKeys
        });
      }
    });
  },

  onHospitalSelect(e) {
    const index = Number(e.detail.value);
    const hospital = this.data.hospitalOptions[index];
    if (!hospital) return;
    this.setData({
      hospitalIndex: index,
      selectedHospitalId: hospital.hospitalId,
      selectedHospitalName: hospital.hospitalName
    });
  },

  generateForSelectedHospital() {
    if (!this.data.selectedHospitalId) {
      wx.showModal({
        title: '请先选择医院',
        content: '请选择需要生成应急密钥的医院。',
        showCancel: false
      });
      return;
    }

    const hospitalId = this.data.selectedHospitalId;
    const hospitalName = this.data.selectedHospitalName;
    const existing = this.data.allKeys.find(item => item.hospitalId === hospitalId);

    const doGenerate = () => {
      wx.showLoading({ title: '生成中...' });
      wx.request({
        url: app.globalData.baseUrl + '/api/keys/hospital/' + hospitalId + '/generate',
        method: 'POST',
        success: (res) => {
          wx.hideLoading();
          if (res.data && res.data.success) {
            this.loadAllKeys();
            wx.showModal({
              title: `${hospitalName} 应急密钥`,
              content: `密钥：${res.data.dailyKey}\n有效期至：${res.data.expireTime}`,
              confirmText: '复制密钥',
              cancelText: '关闭',
              success: (modalRes) => {
                if (modalRes.confirm) {
                  wx.setClipboardData({ data: res.data.dailyKey });
                }
              }
            });
          } else {
            wx.showModal({
              title: '生成失败',
              content: (res.data && res.data.message) || '请稍后重试。',
              showCancel: false
            });
          }
        },
        fail: () => {
          wx.hideLoading();
          wx.showModal({
            title: '网络超时',
            content: '密钥生成请求失败，请检查网络后重试。',
            showCancel: false
          });
        }
      });
    };

    if (existing) {
      wx.showModal({
        title: '确认重新生成',
        content: `该院已有密钥，重新生成后旧密钥立即失效，正在使用旧密钥的应急管理员会被强制退出。`,
        confirmText: '重新生成',
        cancelText: '取消',
        success: (modalRes) => {
          if (modalRes.confirm) {
            doGenerate();
          }
        }
      });
      return;
    }

    doGenerate();
  },

  generateKey() {
    const hospitalId = this.data.hospitalId;
    
    wx.request({
      url: app.globalData.baseUrl + '/api/keys/hospital/' + hospitalId + '/generate',
      method: 'POST',
      success: (res) => {
        if (res.data && res.data.success) {
          wx.showToast({ title: '密钥生成成功', icon: 'success' });
          this.setData({ currentKey: res.data });
        } else {
          wx.showToast({ title: res.data?.message || '生成失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.showToast({ title: '网络错误', icon: 'none' });
      }
    });
  },

  renewKey() {
    const hospitalId = this.data.hospitalId;
    
    wx.request({
      url: app.globalData.baseUrl + '/api/keys/hospital/' + hospitalId + '/renew',
      method: 'POST',
      success: (res) => {
        if (res.data && res.data.success) {
          wx.showToast({ title: '密钥已续期', icon: 'success' });
          this.loadHospitalKey();
        } else {
          wx.showToast({ title: res.data?.message || '续期失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.showToast({ title: '网络错误', icon: 'none' });
      }
    });
  },

  invalidateKey() {
    const hospitalId = this.data.hospitalId;
    
    wx.showModal({
      title: '确认失效',
      content: '确定要使该密钥失效吗？',
      success: (modalRes) => {
        if (modalRes.confirm) {
          wx.request({
            url: app.globalData.baseUrl + '/api/keys/hospital/' + hospitalId + '/invalidate',
            method: 'POST',
            success: (res) => {
              if (res.data && res.data.success) {
                wx.showToast({ title: '密钥已失效', icon: 'success' });
                this.loadHospitalKey();
              } else {
                wx.showToast({ title: res.data?.message || '操作失败', icon: 'none' });
              }
            },
            fail: () => {
              wx.showToast({ title: '网络错误', icon: 'none' });
            }
          });
        }
      }
    });
  },

  generateKeyForHospital(hospitalId) {
    const hospitalName = this.getHospitalName(hospitalId);
    wx.showModal({
      title: '确认重新生成',
      content: `${hospitalName} 的旧密钥将立即失效，正在使用旧密钥的应急管理员会被强制退出。`,
      confirmText: '重新生成',
      cancelText: '取消',
      success: (modalRes) => {
        if (!modalRes.confirm) return;
        this.doGenerateForKey(hospitalId, hospitalName);
      }
    });
  },

  doGenerateForKey(hospitalId, hospitalName) {
    wx.request({
      url: app.globalData.baseUrl + '/api/keys/hospital/' + hospitalId + '/generate',
      method: 'POST',
      success: (res) => {
        if (res.data && res.data.success) {
          this.loadAllKeys();
          wx.showModal({
            title: `${hospitalName} 应急密钥`,
            content: `密钥：${res.data.dailyKey}\n有效期至：${res.data.expireTime}`,
            confirmText: '复制密钥',
            cancelText: '关闭',
            success: (modalRes) => {
              if (modalRes.confirm) {
                wx.setClipboardData({ data: res.data.dailyKey });
              }
            }
          });
        } else {
          wx.showToast({ title: (res.data && res.data.message) || '生成失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.showModal({
          title: '网络超时',
          content: '密钥生成请求失败，请检查网络后重试。',
          showCancel: false
        });
      }
    });
  },

  getHospitalName(hospitalId) {
    return this.data.hospitals[hospitalId]?.hospitalName || hospitalId;
  },

  getStatusText(status) {
    return status === 1 ? '有效' : '无效';
  },

  copyRecoveryKey() {
    const key = this.data.currentKey?.recoveryKey || '';
    if (key) {
      wx.setClipboardData({
        data: key,
        success: () => {
          wx.showToast({ title: '已复制常设应急码', icon: 'success' });
        }
      });
    }
  },

  regenerateRecoveryKey() {
    const hospitalId = this.data.hospitalId;
    wx.showModal({
      title: '刷新重置常设码',
      content: '同一医院共用一把常设码。重置后旧码立即失效，全院都会切换到新码，请重新打印并封存到值班室。确定继续吗？',
      confirmText: '重置',
      cancelText: '取消',
      success: (modalRes) => {
        if (!modalRes.confirm) return;
        wx.request({
          url: app.globalData.baseUrl + '/api/keys/hospital/' + hospitalId + '/recovery/regenerate',
          method: 'POST',
          success: (res) => {
            if (res.data && res.data.success) {
              this.loadHospitalKey();
              wx.showModal({
                title: '已重置',
                content: `新常设应急码：${res.data.recoveryKey}`,
                confirmText: '复制',
                cancelText: '关闭',
                success: (copyRes) => {
                  if (copyRes.confirm) {
                    wx.setClipboardData({ data: res.data.recoveryKey });
                  }
                }
              });
            } else {
              wx.showModal({
                title: '重置失败',
                content: (res.data && res.data.message) || '请稍后重试。',
                showCancel: false
              });
            }
          },
          fail: () => {
            wx.showModal({
              title: '网络超时',
              content: '重置请求失败，请检查网络后重试。',
              showCancel: false
            });
          }
        });
      }
    });
  },

  copyKey() {
    const key = this.data.currentKey?.dailyKey || '';
    if (key) {
      wx.setClipboardData({
        data: key,
        success: () => {
          wx.showToast({ title: '已复制', icon: 'success' });
        }
      });
    }
  },

  goBack() {
    wx.navigateBack();
  }
});