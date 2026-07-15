const app = getApp();
const { get, post, put, del } = require('../../utils/request');
Page({
  data: {
    addresses: [],
    selectedAddressId: '',
    isEditMode: false,
    isAddMode: false,
    currentAddress: null,
    loading: false,
    error: ''
  },

  onLoad(options) {
    // 检查是否从订单页面跳转过来，用于选择地址
    this.setData({
      selectedAddressId: options.selected || ''
    });
    // 获取事件通道，用于向上一页回传地址
    this.eventChannel = this.getOpenerEventChannel && this.getOpenerEventChannel();
    this.loadAddresses();
  },

  // 加载地址列表
  async loadAddresses() {
    this.setData({ loading: true, error: '' });
    try {
      // 获取当前用户ID
      const userId = app.globalData.userInfo.id;
      
      // 调用后端API获取地址列表
      const addresses = await get(`/api/user/address/list?userId=${userId}`);
      this.setData({
        addresses: addresses || [],
        loading: false
      });
    } catch (error) {
      console.error('加载地址列表失败:', error);
      this.setData({
        error: '加载地址失败，请重试',
        loading: false
      });
      wx.showToast({ title: '加载地址失败', icon: 'none' });
    }
  },

  // 添加新地址
  addAddress() {
    this.setData({
      isAddMode: true,
      currentAddress: null
    });
  },

  // 编辑地址
  editAddress(e) {
    const addressId = e.currentTarget.dataset.id;
    const address = this.data.addresses.find(item => item.id === addressId);
    this.setData({
      isEditMode: true,
      currentAddress: address
    });
  },

  // 删除地址
  deleteAddress(e) {
    const addressId = e.currentTarget.dataset.id;
    wx.showModal({
      title: '删除地址',
      content: '确定要删除这个地址吗？',
      success: async (res) => {
        if (res.confirm) {
          try {
            await post(`/api/user/address/delete/${addressId}`);
            // 更新地址列表
            const updatedAddresses = this.data.addresses.filter(item => item.id !== addressId);
            this.setData({ addresses: updatedAddresses });
            wx.showToast({ title: '地址已删除', icon: 'success' });
          } catch (error) {
            console.error('删除地址失败:', error);
            wx.showToast({ title: '删除地址失败', icon: 'none' });
          }
        }
      }
    });
  },

  // 设置默认地址
  async setDefaultAddress(e) {
    const addressId = e.currentTarget.dataset.id;
    try {
      // 获取当前地址
      const address = this.data.addresses.find(item => item.id === addressId);
      
      // 更新地址为默认地址
      address.isDefault = true;
      await post('/api/user/address/update', address);
      
      // 更新地址列表
      const updatedAddresses = this.data.addresses.map(item => ({
        ...item,
        isDefault: item.id === addressId
      }));
      this.setData({ addresses: updatedAddresses });
      wx.showToast({ title: '默认地址已设置', icon: 'success' });
    } catch (error) {
      console.error('设置默认地址失败:', error);
      wx.showToast({ title: '设置默认地址失败', icon: 'none' });
    }
  },

  // 选择地址（从订单页面跳转过来时使用）
  selectAddress(e) {
    const address = e.currentTarget.dataset.address;
    // 返回地址信息给上一页
    const pages = getCurrentPages();
    const prevPage = pages[pages.length - 2];
    
    // 方式1：通过页面间事件通信返回地址信息
    if (this.eventChannel) {
      this.eventChannel.emit('onSelectAddress', address);
    }
    
    // 方式2：直接调用上一页的方法返回地址信息（兼容）
    if (prevPage && prevPage.onSelectAddress) {
      prevPage.onSelectAddress(address);
    }
    
    wx.navigateBack();
  },

  // 取消编辑/添加
  cancelEdit() {
    this.setData({
      isEditMode: false,
      isAddMode: false,
      currentAddress: null
    });
  },

  // 保存地址
  async saveAddress(e) {
    const formData = e.detail.value;
    // 简单验证
    if (!formData.recipient || !formData.phone || !formData.detailAddress) {
      wx.showToast({ title: '请填写完整地址信息', icon: 'none' });
      return;
    }

    try {
      // 转换字段名，使其与后端实体匹配
      const addressData = {
        userId: app.globalData.userInfo.id,
        consignee: formData.recipient, // 前端字段名是recipient，后端是consignee
        phone: formData.phone,
        province: formData.province,
        city: formData.city,
        district: formData.district,
        detail: formData.detailAddress, // 前端字段名是detailAddress，后端是detail
        isDefault: false // 默认不是默认地址
      };
      
      if (this.data.isEditMode) {
        // 编辑现有地址
        addressData.id = this.data.currentAddress.id;
        addressData.isDefault = this.data.currentAddress.isDefault;
        await post('/api/user/address/update', addressData);
      } else {
        // 添加新地址
        await post('/api/user/address/add', addressData);
      }
      
      // 关闭编辑/添加模式
      this.setData({
        isEditMode: false,
        isAddMode: false,
        currentAddress: null
      });
      
      // 重新加载地址列表
      this.loadAddresses();
      wx.showToast({ title: '地址已保存', icon: 'success' });
    } catch (error) {
      console.error('保存地址失败:', error);
      wx.showToast({ title: '保存地址失败', icon: 'none' });
    }
  }
});
