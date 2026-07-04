const app = getApp();
// 导入网络请求封装
const { get, post, resolveImageUrl } = require('../../utils/request');

Page({
  data:{
    cart:[],
    total:0,
    isAllSelected: false,
    selectedCount: 0,
    selectedTotal: 0,
    defaultAddress: null, // 用户默认地址
    selectedAddressId: null // 用户选择的地址ID
  },
  
  onShow(){ 
    this.loadCart(); 
    this.loadDefaultAddress(); // 加载默认地址
  },
  
  // 加载用户默认地址
  async loadDefaultAddress() {
    try {
      // 获取当前用户信息
      const userInfo = app.globalData.userInfo;
      if (!userInfo) {
        console.log('用户未登录，无法获取默认地址');
        return;
      }
      
      // 调用后端API获取用户默认地址
      const defaultAddress = await get('/api/user/address/default', { userId: userInfo.id });
      if (defaultAddress) {
        this.setData({ 
          defaultAddress: defaultAddress,
          selectedAddressId: defaultAddress.id // 默认选中默认地址
        });
      } else {
        console.log('用户没有默认地址');
        this.setData({ defaultAddress: null });
      }
    } catch (error) {
      console.error('加载默认地址失败:', error);
      // 不影响用户体验，仅记录日志
    }
  },
  
  // 加载购物车数据
  async loadCart(){
    try {
      // 获取当前用户信息
      const userInfo = app.globalData.userInfo;
      
      if (userInfo) {
        try {
          // 从后端API获取购物车数据
          const cart = await get('/api/cart/list', { userId: userInfo.id });
          console.log('从后端获取的购物车数据:', cart);
          
          // 为每个商品添加selected属性（使用后端的checked字段），并处理图片URL
          const processedCart = (cart || []).map(item => {
            return {
              ...item,
              cover: resolveImageUrl(item.cover),
              selected: item.checked === 1 // 将后端的checked(1/0)转换为前端的selected(true/false)
            };
          });
          
          this.setData({ cart: processedCart });
          // 将后端购物车数据保存到本地缓存
          wx.setStorageSync('localCart', processedCart);
        } catch (apiError) {
          console.error('从后端加载购物车失败:', apiError);
          // 如果后端加载失败，尝试从本地缓存获取
          const localCart = wx.getStorageSync('localCart') || [];
          this.setData({ cart: localCart });
        }
      } else {
        // 如果没有登录，尝试从本地缓存获取购物车数据
        const localCart = wx.getStorageSync('localCart') || [];
        this.setData({ cart: localCart });
      }
      
      this.calculateSelected();
    } catch (error) {
      console.error('加载购物车失败:', error);
      // 最终降级到空购物车
      this.setData({ cart: [] });
      this.calculateSelected();
    }
  },
  
  // 切换单个商品选中状态
  async toggleSelectItem(e){
    const index = e.currentTarget.dataset.index;
    let cart = this.data.cart;
    const item = cart[index];
    const newChecked = item.checked === 1 ? 0 : 1;
    
    try {
      // 调用后端API更新选中状态
      await post(`/api/cart/${item.id}/update-checked`, { checked: newChecked });
      
      // 更新本地数据
      cart[index].checked = newChecked;
      cart[index].selected = newChecked === 1;
      this.setData({ cart: cart });
      this.calculateSelected();
    } catch (error) {
      console.error('更新选中状态失败:', error);
      wx.showToast({ title: '更新失败', icon: 'error' });
    }
  },
  
  // 切换全选状态
  async toggleSelectAll(){
    const isAllSelected = !this.data.isAllSelected;
    const newChecked = isAllSelected ? 1 : 0;
    const cart = this.data.cart;
    
    try {
      // 批量更新所有商品的选中状态
      for (let item of cart) {
        await post(`/api/cart/${item.id}/update-checked`, { checked: newChecked });
      }
      
      // 更新本地数据
      const updatedCart = cart.map(item => ({
        ...item,
        checked: newChecked,
        selected: isAllSelected
      }));
      
      this.setData({ 
        cart: updatedCart,
        isAllSelected: isAllSelected
      });
      this.calculateSelected();
    } catch (error) {
      console.error('更新全选状态失败:', error);
      wx.showToast({ title: '更新失败', icon: 'error' });
    }
  },
  
  // 计算已选商品数量和总价
  calculateSelected(){
    const cart = this.data.cart;
    let selectedCount = 0;
    let selectedTotal = 0;
    let isAllSelected = true;
    
    cart.forEach(item => {
      if(item.selected){
        selectedCount += item.quantity;
        selectedTotal += item.price * item.quantity;
      } else {
        isAllSelected = false;
      }
    });
    
    this.setData({
      selectedCount: selectedCount,
      selectedTotal: selectedTotal,
      isAllSelected: isAllSelected
    });
  },
  
  // 减少数量
  async decreaseQuantity(e){
    const id = e.currentTarget.dataset.id;
    let cart = this.data.cart;
    const idx = cart.findIndex(i=>i.id===id);
    if(idx>-1){
      const item = cart[idx];
      const newQuantity = item.quantity - 1;
      
      try {
        if(newQuantity > 0){
          // 更新数量
          await post(`/api/cart/${id}/update-quantity`, { quantity: newQuantity });
          // 更新本地数据
          cart[idx].quantity = newQuantity;
        } else {
          // 删除商品
          await post(`/api/cart/${id}/delete`);
          // 更新本地数据
          cart.splice(idx, 1);
        }
        
        this.setData({ cart: cart });
        this.calculateSelected();
      } catch (error) {
        console.error('更新数量失败:', error);
        wx.showToast({ title: '更新失败', icon: 'error' });
      }
    }
  },
  
  // 增加数量
  async increaseQuantity(e){
    const id = e.currentTarget.dataset.id;
    let cart = this.data.cart;
    const idx = cart.findIndex(i=>i.id===id);
    if(idx>-1){
      const item = cart[idx];
      const newQuantity = item.quantity + 1;
      
      try {
        // 调用后端API更新数量
        await post(`/api/cart/${id}/update-quantity`, { quantity: newQuantity });
        
        // 更新本地数据
        cart[idx].quantity = newQuantity;
        this.setData({ cart: cart });
        this.calculateSelected();
      } catch (error) {
        console.error('更新数量失败:', error);
        wx.showToast({ title: '更新失败', icon: 'error' });
      }
    }
  },
  
  // 删除商品
  async removeItem(e){
    const id = e.currentTarget.dataset.id;
    try {
      // 调用后端API删除商品
      await post(`/api/cart/${id}/delete`);
      
      // 更新本地数据
      const newCart = this.data.cart.filter(i=>i.id!==id);
      this.setData({ cart: newCart });
      this.calculateSelected();
      wx.showToast({title:'已删除'});
    } catch (error) {
      console.error('删除商品失败:', error);
      wx.showToast({ title: '删除失败', icon: 'error' });
    }
  },
  
  // 去首页
  goToHome(){
    wx.switchTab({ url: '/pages/home/home' });
  },

  // 前往地址选择页面
  goToAddress(){
    wx.navigateTo({
      url: '/pages/user/address?from=cart',
      events: {
        onSelectAddress: (address) => {
          this.setData({
            defaultAddress: address,
            selectedAddressId: address.id
          });
        }
      }
    });
  },
  
  // 结算
  async checkout(){ 
    const cart = this.data.cart;
    const selectedItems = cart.filter(item => item.selected);
    
    if(selectedItems.length === 0){
      wx.showToast({
        title: '请选择要结算的商品', 
        icon: 'none',
        duration: 2000
      });
      return;
    }
    
    try {
      // 获取当前用户信息
      const userInfo = app.globalData.userInfo;
      if (!userInfo) {
        wx.showToast({ title: '请先登录', icon: 'error' });
        return;
      }
      
      // 直接使用默认地址创建订单，无需跳转选择地址页面
      const orderData = { 
        openid: userInfo.openid,
        userId: userInfo.id,
        addressId: this.data.selectedAddressId || null, // 使用默认地址ID
        items: selectedItems.map(i => ({
          productId: i.productId, 
          quantity: i.quantity,
          specId: i.specId || null
        })) 
      };
      
      // 显示订单创建中的提示
      wx.showLoading({
        title: '正在创建订单...',
        mask: true
      });
      
      // 使用封装的post函数创建订单
      const orderResult = await post('/api/order/create', orderData);
      
      // 隐藏加载提示
      wx.hideLoading();
      
      // 显示创建成功提示
      wx.showToast({
        title: '订单创建成功',
        icon: 'success',
        duration: 1500
      });
      
      // 清空购物车中已选中的商品
      try {
        for (const item of selectedItems) {
          await post(`/api/cart/${item.id}/delete`);
        }
        // 清空本地缓存的已选商品
        const newCart = this.data.cart.filter(item => !item.selected);
        this.setData({ cart: newCart });
        this.calculateSelected();
        // 更新本地缓存
        wx.setStorageSync('localCart', newCart);
      } catch (clearError) {
        console.error('清空购物车失败:', clearError);
        // 即使清空购物车失败，也继续跳转到订单详情页
      }
      
      // 延迟跳转到订单详情页，让用户看到成功提示
      setTimeout(() => {
        // 跳转到订单详情页
        wx.navigateTo({url:'/pages/order/order?id=' + orderResult.id});
      }, 1500);
      
    } catch (error) {
      // 隐藏加载提示
      wx.hideLoading();
      
      // 显示更友好的错误提示
      const errorMsg = error.response?.data?.message || error.message || '创建订单失败，请稍后重试';
      wx.showToast({
        title: errorMsg, 
        icon: 'error',
        duration: 3000
      });
      console.error('创建订单失败:', error);
    }
  },
})
