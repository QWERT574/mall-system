App({
  globalData: {
    backendBaseUrl: 'http://localhost:8081',
    userInfo: null,
    token: null
  },

  onLaunch() {
    // 检查本地存储的登录状态
    this.checkLoginStatus();
    // 如果未登录，自动使用默认账号登录（开发环境）
    if (!this.globalData.token) {
      this.autoLogin();
    }
  },

  // 自动登录（开发环境使用默认买家账号）
  async autoLogin() {
    try {
      const res = await this.accountLogin('user', '123456');
      console.log('自动登录成功');
    } catch (e) {
      console.warn('自动登录失败，请手动登录:', e.message);
    }
  },

  // 账号密码登录
  accountLogin(username, password) {
    return new Promise((resolve, reject) => {
      wx.request({
        url: this.globalData.backendBaseUrl + '/api/auth/login',
        method: 'POST',
        data: { username, password },
        header: { 'content-type': 'application/json' },
        success: (res) => {
          if (res.statusCode === 200 && res.data.code === 0) {
            const data = res.data.data;
            this.globalData.token = data.token;
            this.globalData.userInfo = data.user;
            wx.setStorageSync('token', data.token);
            wx.setStorageSync('userInfo', data.user);
            resolve(data);
          } else {
            reject(new Error(res.data.message || '登录失败'));
          }
        },
        fail: (err) => {
          reject(new Error('网络请求失败: ' + err.errMsg));
        }
      });
    });
  },

  // 检查登录状态
  checkLoginStatus() {
    const token = wx.getStorageSync('token');
    const userInfo = wx.getStorageSync('userInfo');
    
    if (token && userInfo) {
      this.globalData.token = token;
      this.globalData.userInfo = userInfo;
    }
  },

  // 登录函数
  async login() {
    try {
      // 1. 调用微信登录接口获取code
      const loginResult = await this.wxLogin();
      const code = loginResult.code;
      
      // 2. 调用后端登录接口获取openid和token
      const res = await this.requestLogin(code);
      
      // 3. 存储登录信息
      this.globalData.token = res.token;
      this.globalData.userInfo = res.user;
      
      wx.setStorageSync('token', res.token);
      wx.setStorageSync('userInfo', res.user);
      
      return res;
    } catch (error) {
      console.error('登录失败:', error);
      throw error;
    }
  },

  // 微信登录获取code
  wxLogin() {
    return new Promise((resolve, reject) => {
      wx.login({
        success: (res) => {
          if (res.code) {
            resolve(res);
          } else {
            reject(new Error('微信登录失败: ' + res.errMsg));
          }
        },
        fail: (err) => {
          reject(new Error('微信登录失败: ' + err.errMsg));
        }
      });
    });
  },

  // 请求后端登录接口
  requestLogin(code) {
    return new Promise((resolve, reject) => {
      wx.request({
        url: this.globalData.backendBaseUrl + '/api/auth/login',
        method: 'POST',
        data: { code },
        header: {
          'content-type': 'application/json'
        },
        success: (res) => {
          if (res.statusCode === 200 && res.data.code === 0) {
            resolve(res.data.data);
          } else {
            reject(new Error('后端登录失败: ' + (res.data.message || res.errMsg)));
          }
        },
        fail: (err) => {
          reject(new Error('网络请求失败: ' + err.errMsg));
        }
      });
    });
  },

  // 退出登录
  logout() {
    this.globalData.token = null;
    this.globalData.userInfo = null;
    
    wx.removeStorageSync('token');
    wx.removeStorageSync('userInfo');
  }
})
