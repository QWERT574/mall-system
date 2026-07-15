const app = getApp();
const { get, post, put, resolveImageUrl } = require('../../utils/request');
Page({
  data: {
    userInfo: null,
    loading: false,
    error: '',
    birthdayValue: '', // 用于存储日期选择器的实际值（格式：YYYY-MM-DD）
    birthdayDisplay: '' // 用于显示的格式化日期
  },

  onLoad() {
    this.loadUserInfo();
  },

  // 加载用户信息
  async loadUserInfo() {
    this.setData({ loading: true, error: '' });
    try {
      // 从全局数据获取用户信息
      const userInfo = app.globalData.userInfo;
      
      // 处理生日日期，转换为日期选择器可以使用的格式
      let birthdayValue = '';
      let birthdayDisplay = '';
      if (userInfo.birthday) {
        // 转换生日日期格式
        const birthday = new Date(userInfo.birthday);
        // 确保日期格式为YYYY-MM-DD
        birthdayValue = birthday.toISOString().split('T')[0];
        // 显示格式：YYYY年MM月DD日
        birthdayDisplay = `${birthday.getFullYear()}年${(birthday.getMonth() + 1).toString().padStart(2, '0')}月${birthday.getDate().toString().padStart(2, '0')}日`;
      }
      
      this.setData({
        userInfo: { ...userInfo, avatarUrl: resolveImageUrl(userInfo.avatar) },
        birthdayValue: birthdayValue,
        birthdayDisplay: birthdayDisplay,
        loading: false
      });
    } catch (error) {
      console.error('加载用户信息失败:', error);
      this.setData({
        error: '加载用户信息失败，请重试',
        loading: false
      });
      wx.showToast({ title: '加载用户信息失败', icon: 'none' });
    }
  },

  // 生日选择器变化事件处理函数
  onBirthdayChange(e) {
    const selectedDate = e.detail.value; // 格式：YYYY-MM-DD
    
    // 将日期转换为更友好的显示格式
    const date = new Date(selectedDate);
    const displayDate = `${date.getFullYear()}年${(date.getMonth() + 1).toString().padStart(2, '0')}月${date.getDate().toString().padStart(2, '0')}日`;
    
    this.setData({
      birthdayValue: selectedDate,
      birthdayDisplay: displayDate
    });
  },

  // 选择性别
  selectGender(e) {
    const gender = parseInt(e.currentTarget.dataset.gender);
    this.setData({
      'userInfo.gender': gender
    });
  },

  // 更新用户信息
  async updateUserInfo(e) {
    const userData = e.detail.value;
    this.setData({ loading: true });
    try {
      // 获取当前用户ID
      const userId = this.data.userInfo.id;
      userData.id = userId;
      
      // 转换性别字段为数值类型，因为后端需要Integer类型
      if (userData.gender !== undefined && userData.gender !== '') {
        userData.gender = parseInt(userData.gender);
      }
      
      // 调用后端API更新用户信息
      const updatedUser = await post('/api/user/update', userData);
      
      // 更新全局用户信息
      app.globalData.userInfo = updatedUser;
      
      // 更新本地数据
      this.setData({
        userInfo: updatedUser,
        loading: false
      });
      
      wx.showToast({ title: '个人信息已更新', icon: 'success' });
    } catch (error) {
      console.error('更新用户信息失败:', error);
      this.setData({ loading: false });
      wx.showToast({ title: '更新个人信息失败', icon: 'none' });
    }
  },

  // 选择头像
  chooseAvatar() {
    wx.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: async (res) => {
        const tempFilePath = res.tempFilePaths[0];
        try {
          this.setData({ loading: true });
          
          // 上传头像到服务器
          const uploadRes = await new Promise((resolve, reject) => {
            wx.uploadFile({
              url: app.globalData.backendBaseUrl + '/api/upload/avatar',
              filePath: tempFilePath,
              name: 'file',
              header: {
                'Authorization': 'Bearer ' + (wx.getStorageSync('token') || app.globalData.token)
              },
              success: (r) => {
                try {
                  resolve(JSON.parse(r.data));
                } catch (e) {
                  reject(new Error('解析上传结果失败'));
                }
              },
              fail: reject
            });
          });
          
          let avatarUrl = tempFilePath;
          if (uploadRes && uploadRes.code === 0 && uploadRes.data) {
            avatarUrl = uploadRes.data;
            // 如果返回的是相对路径，拼接完整URL
            if (avatarUrl.startsWith('/')) {
              avatarUrl = app.globalData.backendBaseUrl + avatarUrl;
            }
          }
          
          // 更新用户头像到后端
          try {
            const updatedUser = await post('/api/user/update', {
              id: this.data.userInfo.id,
              avatar: avatarUrl
            });
            app.globalData.userInfo = updatedUser;
            this.setData({ userInfo: updatedUser, loading: false });
          } catch (e) {
            // 即使后端更新失败，也更新本地显示
            const updatedUser = { ...this.data.userInfo, avatar: avatarUrl };
            app.globalData.userInfo = updatedUser;
            this.setData({ userInfo: updatedUser, loading: false });
          }
          
          wx.showToast({ title: '头像已更新', icon: 'success' });
        } catch (error) {
          console.error('更新头像失败:', error);
          this.setData({ loading: false });
          wx.showToast({ title: '更新头像失败', icon: 'none' });
        }
      }
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
          wx.navigateBack();
          wx.showToast({ title: '已退出登录', icon: 'success' });
        }
      }
    });
  }
});
