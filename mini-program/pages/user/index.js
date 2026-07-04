const app = getApp();
const { post, get, resolveImageUrl } = require('../../utils/request');
Page({
  data: {
    userInfo: null,
    isLogin: false,
    pendingCount: 0,
    shippingCount: 0
  },
  
  onShow() {
    this.checkLogin();
  },
  
  // 检查登录状态
  checkLogin() {
    const userInfo = app.globalData.userInfo;
    console.log('检查登录状态，userInfo:', userInfo);
    
    // 如果没有userInfo，初始化一个空对象，避免页面显示异常
    if (!userInfo) {
      this.setData({
        userInfo: {},
        isLogin: false
      });
    } else {
      // 处理头像URL
      if (userInfo.avatar) {
        userInfo.avatarUrl = resolveImageUrl(userInfo.avatar);
      }
      this.setData({
        userInfo: userInfo,
        isLogin: true
      });
      this.loadOrderCounts();
    }
  },

  // 加载订单计数
  async loadOrderCounts() {
    try {
      const userInfo = app.globalData.userInfo;
      if (!userInfo || !userInfo.id) return;
      const res = await get('/api/order/list', { page: 1, size: 100 });
      const orders = Array.isArray(res) ? res : (res.records || res.list || []);
      let pendingCount = 0, shippingCount = 0;
      orders.forEach(o => {
        if (o.status === 0) pendingCount++;
        if (o.status === 2) shippingCount++;
      });
      this.setData({ pendingCount, shippingCount });
    } catch (e) {
      // 计数加载失败不影响主流程
    }
  },
  
  // 显示账号登录弹窗
  showAccountLogin() {
    const that = this;
    wx.showModal({
      title: '账号登录',
      editable: true,
      placeholderText: '用户名',
      content: '',
      confirmText: '下一步',
      success(res) {
        if (res.confirm && res.content) {
          const username = res.content.trim();
          wx.showModal({
            title: '输入密码',
            editable: true,
            placeholderText: '密码',
            success(res2) {
              if (res2.confirm && res2.content) {
                that.accountLogin(username, res2.content);
              }
            }
          });
        }
      }
    });
  },

  // 账号密码登录
  async accountLogin(username, password) {
    try {
      wx.showLoading({ title: '登录中...' });
      const res = await post('/api/auth/login', { username, password });
      if (res && res.token) {
        app.globalData.token = res.token;
        app.globalData.userInfo = res.user;
        wx.setStorageSync('token', res.token);
        wx.setStorageSync('userInfo', res.user);
        this.setData({
          userInfo: res.user,
          isLogin: true
        });
        wx.hideLoading();
        wx.showToast({ title: '登录成功', icon: 'success' });
      }
    } catch (error) {
      wx.hideLoading();
      console.error('账号登录失败:', error);
      wx.showToast({ title: '登录失败', icon: 'none' });
    }
  },

  // 微信授权登录
  async wxLogin() {
    try {
      wx.showLoading({ title: '登录中...' });
      
      // 1. 获取用户信息授权
      const userInfoResult = await this.getUserProfile();
      
      // 2. 调用app.login()进行登录
      const loginResult = await app.login();
      
      // 3. 更新用户信息
      this.setData({
        userInfo: app.globalData.userInfo,
        isLogin: true
      });
      
      wx.hideLoading();
      wx.showToast({ title: '登录成功', icon: 'success' });
    } catch (error) {
      console.error('登录失败:', error);
      wx.hideLoading();
      wx.showToast({ title: '登录失败', icon: 'none' });
    }
  },
  
  // 获取用户信息
  getUserProfile() {
    return new Promise((resolve, reject) => {
      wx.getUserProfile({
        desc: '用于完善会员资料',
        success: (res) => {
          resolve(res);
        },
        fail: (err) => {
          reject(new Error('获取用户信息失败: ' + err.errMsg));
        }
      });
    });
  },
  
  // 退出登录
  logout() {
    wx.showModal({
      title: '退出登录',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          app.logout();
          this.setData({
            userInfo: null,
            isLogin: false
          });
          wx.showToast({ title: '已退出登录', icon: 'success' });
        }
      }
    });
  },
  
  // 联系客服
  contactUs() {
    wx.showModal({
      title: '联系客服',
      content: '客服电话：123456789\n客服微信：minimall_service',
      showCancel: false
    });
  },
  
  // 关于我们
  aboutUs() {
    wx.showModal({
      title: '关于我们',
      content: 'MiniMall 是一个基于微信小程序的电商平台，提供优质的商品和服务。\n版本：1.0.0\n开发者：MiniMall Team',
      showCancel: false
    });
  },

  // 跳转到AI助手页面
  navigateToAI() {
    wx.navigateTo({
      url: '/pages/ai/ai'
    });
  },

  // 跳转到个人信息页面
  navigateToProfile() {
    wx.navigateTo({
      url: '/pages/user/profile'
    });
  },

  // 跳转到地址管理页面
  goToOrder(e) {
    const status = e && e.currentTarget && e.currentTarget.dataset ? e.currentTarget.dataset.status : '';
    let url = '/pages/order/list';
    if (status !== undefined && status !== '') {
      url += '?status=' + status;
    }
    wx.navigateTo({ url })
  },

  goToAfterSale() {
    wx.navigateTo({ url: '/pages/aftersale/list' })
  },

  goToCoupon() {
    wx.switchTab({ url: '/pages/coupon/index' })
  },

  goToDiscount() {
    wx.navigateTo({ url: '/pages/discount/index' })
  },

  navigateToAddress() {
    wx.navigateTo({
      url: '/pages/user/address'
    });
  },

  goToChat() {
    wx.navigateTo({ url: '/pages/chat/chat' })
  },

  goToActivity() {
    wx.navigateTo({ url: '/pages/activity/list' })
  },

  navigateToSecurity() {
    wx.navigateTo({ url: '/pages/user/security' })
  }
})