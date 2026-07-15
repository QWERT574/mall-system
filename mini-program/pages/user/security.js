const { get, post, put } = require('../../utils/request')

Page({
  data: {
    userInfo: {},
    phone: '',
    wechatBound: false,
    hasPassword: true
  },

  onLoad() {
    this.checkLogin()
  },

  checkLogin() {
    const app = getApp()
    const userInfo = app.globalData.userInfo || {}
    this.setData({
      userInfo,
      phone: userInfo.phone || '',
      wechatBound: !!userInfo.openid
    })
  },

  // 修改密码
  changePassword() {
    const that = this;
    wx.showModal({
      title: '修改密码',
      editable: true,
      placeholderText: '请输入旧密码',
      success(res) {
        if (!res.confirm || !res.content) return;
        const oldPassword = res.content;
        wx.showModal({
          title: '输入新密码',
          editable: true,
          placeholderText: '请输入新密码(6-20位)',
          success(res2) {
            if (!res2.confirm || !res2.content) return;
            const newPassword = res2.content;
            if (newPassword.length < 6) {
              wx.showToast({ title: '密码至少6位', icon: 'none' });
              return;
            }
            wx.showModal({
              title: '确认新密码',
              editable: true,
              placeholderText: '请再次输入新密码',
              success(res3) {
                if (!res3.confirm || !res3.content) return;
                if (res3.content !== newPassword) {
                  wx.showToast({ title: '两次密码不一致', icon: 'none' });
                  return;
                }
                that.doChangePassword(oldPassword, newPassword);
              }
            });
          }
        });
      }
    });
  },

  async doChangePassword(oldPassword, newPassword) {
    try {
      wx.showLoading({ title: '修改中...' });
      const app = getApp();
      await put('/api/user/change-password', {
        userId: app.globalData.userInfo.id,
        oldPassword: oldPassword,
        newPassword: newPassword
      });
      wx.hideLoading();
      wx.showToast({ title: '密码修改成功', icon: 'success' });
    } catch (e) {
      wx.hideLoading();
      wx.showToast({ title: '密码修改失败', icon: 'none' });
    }
  },

  // 绑定手机号
  bindPhone() {
    const that = this;
    wx.showModal({
      title: '绑定手机号',
      editable: true,
      placeholderText: '请输入手机号',
      success(res) {
        if (!res.confirm || !res.content) return;
        const phone = res.content.trim();
        if (!/^1\d{10}$/.test(phone)) {
          wx.showToast({ title: '手机号格式不正确', icon: 'none' });
          return;
        }
        that.doBindPhone(phone);
      }
    });
  },

  async doBindPhone(phone) {
    try {
      wx.showLoading({ title: '绑定中...' });
      const app = getApp();
      const updatedUser = await put('/api/user/update', {
        id: app.globalData.userInfo.id,
        phone: phone
      });
      app.globalData.userInfo = updatedUser;
      this.setData({ phone: phone, userInfo: updatedUser });
      wx.hideLoading();
      wx.showToast({ title: '手机号绑定成功', icon: 'success' });
    } catch (e) {
      wx.hideLoading();
      wx.showToast({ title: '绑定失败', icon: 'none' });
    }
  },

  // 微信绑定
  bindWechat() {
    wx.showToast({ title: '请在微信中授权绑定', icon: 'none' });
  }
})
