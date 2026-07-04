const app = getApp();
// 导入网络请求封装
const { get, post, resolveImageUrl } = require('../../utils/request');

Page({
  data:{
    product:{},
    specs: [], // 从后端获取的规格列表
    selectedSpec: null,
    quantity: 1,
    loading: false,
    buyLoading: false,
    showCartAnimation: false,
    cartAnimationX: 0,
    cartAnimationY: 0,
    cartAnimationOpacity: 1,
    error: ''
  },
  
  onLoad(query){
    const id = query.id;
    console.log('商品详情页面加载，商品ID：', id);
    this.loadProductDetail(id);
  },
  
  async loadProductDetail(id){
    // 处理从WXML bindtap传入的事件对象
    if (id && typeof id === 'object') {
      id = this.data.product.id || this.options.id;
    }
    // 如果没有提供ID，从页面栈中获取
    if (!id) {
      const pages = getCurrentPages();
      const currentPage = pages[pages.length - 1];
      id = currentPage.options.id;
    }
    
    // 如果还是没有ID，显示错误
    if (!id) {
      this.setData({ 
        loading: false,
        error: '无效的商品ID，请返回重试',
        product: {},
        specs: []
      });
      return;
    }
    
    this.setData({ loading: true, error: '' });
    try {
      // 加载商品详情
      const product = await get(`/api/product/${id}`);
      console.log('获取商品详情成功：', product);
      
      // 处理商品图片链接，将相对路径转换为完整URL
      let coverUrl = resolveImageUrl(product.cover);
      
      // 如果没有图片或图片为空，使用默认图片
      if (!coverUrl) {
        coverUrl = 'https://via.placeholder.com/800x400?text=商品图片';
      }
      product.cover = coverUrl;
      
      this.setData({ product: product });
      
      // 加载商品规格
      const specs = await get(`/api/product/${id}/specs`);
      console.log('获取商品规格成功：', specs);
      this.setData({ specs: specs || [] });
    } catch (error) {
      console.error('加载商品详情失败：', error);
      this.setData({ 
        error: '加载商品详情失败，请重试',
        product: { id: id }, // 保留商品ID，以便重试
        specs: []
      });
      wx.showToast({
        title: '加载商品详情失败',
        icon: 'none'
      });
    } finally {
      this.setData({ loading: false });
    }
  },
  
  // 选择规格
  selectSpec(e){
    const spec = e.currentTarget.dataset.spec;
    this.setData({ selectedSpec: spec });
  },
  
  // 减少数量
  decreaseQuantity(){
    if(this.data.quantity > 1){
      this.setData({ quantity: this.data.quantity - 1 });
    }
  },
  
  // 增加数量
  increaseQuantity(){ 
    // 根据是否有规格选择，使用不同的库存值
    const stock = this.data.selectedSpec ? this.data.selectedSpec.stock : this.data.product.stock;
    if(this.data.quantity < stock){ 
      this.setData({ quantity: this.data.quantity + 1 }); 
    }
  },
  
  // 加入购物车
  async addToCart(){
    const p = this.data.product;
    const selectedSpec = this.data.selectedSpec;
    const quantity = this.data.quantity;
    
    // 如果没有选择规格，使用默认规格
    if (!selectedSpec && this.data.specs.length > 0) {
      this.setData({ selectedSpec: this.data.specs[0] });
    }
    
    const spec = this.data.selectedSpec;
    
    this.setData({ loading: true });
    
    // 执行加入购物车动画
    this.runCartAnimation();
    
    try {
      // 延迟执行实际的加入购物车逻辑，让动画先播放
      await new Promise(resolve => setTimeout(resolve, 500));
      
      // 获取当前用户信息
      const userInfo = app.globalData.userInfo;
      
      // 计算商品价格
      const price = spec ? spec.price : p.price;
      
      // 获取当前商品的库存
      const stock = spec ? spec.stock : p.stock;
      
      if (userInfo) {
        // 如果用户已登录，调用后端API将商品添加到后端购物车
        try {
          // 调用后端API添加商品到购物车
          await post('/api/cart/add', {
            userId: userInfo.id,
            productId: p.id,
            specId: spec ? spec.id : null,
            quantity: quantity
          });
          
          // 刷新本地缓存，从后端获取最新的购物车数据
          const cart = await get('/api/cart/list', { userId: userInfo.id });
          console.log('从后端获取的购物车数据:', cart);
          
          // 为每个商品添加selected属性（使用后端的checked字段）
          const processedCart = (cart || []).map(item => ({
            ...item,
            selected: item.checked === 1 // 将后端的checked(1/0)转换为前端的selected(true/false)
          }));
          
          // 将后端购物车数据保存到本地缓存的localCart中
          wx.setStorageSync('localCart', processedCart);
        } catch (apiError) {
          console.error('调用后端API添加购物车失败:', apiError);
          // 如果后端API调用失败，使用本地缓存添加商品
          this.addToLocalCart(p, spec, quantity, price, stock);
        }
      } else {
        // 如果用户未登录，只添加到本地缓存
        this.addToLocalCart(p, spec, quantity, price, stock);
      }
      
      wx.showToast({title: '已加入购物车', icon: 'success'});
    } catch (error) {
      console.error('加入购物车失败:', error);
      wx.showToast({title: '加入购物车失败', icon: 'error'});
    } finally {
      this.setData({ loading: false });
    }
  },
  
  // 立即购买
  async buyNow() {
    const p = this.data.product;
    const spec = this.data.selectedSpec;
    const quantity = this.data.quantity;
    const userInfo = app.globalData.userInfo;

    if (!userInfo) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }

    this.setData({ buyLoading: true });
    try {
      // 先加入购物车
      await post('/api/cart/add', {
        userId: userInfo.id,
        productId: p.id,
        specId: spec ? spec.id : null,
        quantity: quantity
      });

      // 获取购物车列表
      const cart = await get('/api/cart/list', { userId: userInfo.id });
      const processedCart = (cart || []).map(item => ({
        ...item,
        selected: item.checked === 1
      }));

      // 找到刚加入的商品并选中
      const targetItem = processedCart.find(i => i.productId === p.id);
      if (targetItem) {
        targetItem.selected = true;
        targetItem.checked = 1;
      }

      wx.setStorageSync('localCart', processedCart);

      // 跳转到购物车页面
      wx.switchTab({ url: '/pages/cart/cart' });
    } catch (e) {
      console.error('购买失败:', e);
      wx.showToast({ title: '操作失败', icon: 'none' });
    } finally {
      this.setData({ buyLoading: false });
    }
  },

  // 仅添加到本地缓存的辅助方法
  addToLocalCart(p, spec, quantity, price, stock) {
    let cart = wx.getStorageSync('localCart') || [];
    // 使用规格ID作为唯一标识
    const specId = spec ? spec.id : null;
    // 查找商品是否已存在（使用productId和specId作为唯一标识）
    const idx = cart.findIndex(i => i.productId === p.id && i.specId === specId);
    
    if(idx > -1){
      // 确保不超过库存
      const newQuantity = Math.min(cart[idx].quantity + quantity, stock);
      cart[idx].quantity = newQuantity;
      cart[idx].stock = stock;
    } else {
      cart.push({
        productId: p.id,
        name: p.name,
        cover: p.cover,
        price: price,
        specId: specId,
        specName: spec ? spec.specName : '',
        quantity: quantity,
        stock: stock,
        checked: 0, // 默认未选中
        selected: false // 默认未选中
      });
    }
    
    // 将更新后的购物车数据保存到本地缓存的localCart中
    wx.setStorageSync('localCart', cart);
  },
  
  // 加入购物车动画
  runCartAnimation(){
    // 获取按钮位置
    const query = wx.createSelectorQuery();
    query.select('.add-to-cart-btn').boundingClientRect();
    query.selectViewport().scrollOffset();
    
    query.exec(res => {
      // 计算动画路径（从按钮位置到屏幕右下角）
      const btnRect = res[0];
      const scrollOffset = res[1];
      
      // 起始位置（按钮中心）
      const startX = btnRect.left + btnRect.width / 2;
      const startY = btnRect.top + scrollOffset.scrollTop + btnRect.height / 2;
      
      // 结束位置（屏幕右下角，模拟购物车图标位置）
      const endX = wx.getSystemInfoSync().windowWidth - 60;
      const endY = wx.getSystemInfoSync().windowHeight - 60;
      
      // 设置初始动画状态
      this.setData({
        showCartAnimation: true,
        cartAnimationX: startX - 40, // 40是图片宽度的一半
        cartAnimationY: startY - 40, // 40是图片高度的一半
        cartAnimationOpacity: 1
      });
      
      // 执行动画
      let step = 0;
      const totalSteps = 20;
      const animate = () => {
        step++;
        const progress = step / totalSteps;
        
        // 计算当前位置（抛物线轨迹）
        const currentX = startX + (endX - startX) * progress;
        const currentY = startY + (endY - startY) * progress + Math.sin(progress * Math.PI) * 50;
        
        // 计算透明度（逐渐消失）
        const opacity = 1 - progress;
        
        this.setData({
          cartAnimationX: currentX - 40,
          cartAnimationY: currentY - 40,
          cartAnimationOpacity: opacity
        });
        
        if(step < totalSteps){
          setTimeout(animate, 20);
        } else {
          // 动画结束
          this.setData({ showCartAnimation: false });
        }
      };
      
      // 启动动画
      animate();
    });
  }
})
