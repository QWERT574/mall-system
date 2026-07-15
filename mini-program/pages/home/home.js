const app = getApp();
// 导入网络请求封装
const { get, resolveImageUrl } = require('../../utils/request');

Page({
  data: { 
    productList: [], 
    searchKeyword: '',
    searchHistory: [],
    total: 0,
    currentPage: 1,
    hasMore: true,
    loading: false,
    error: '',
    // 静态推广 banner：助农活动 + 优惠活动
    bannerList: [
      {
        type: 'promotion',
        style: 'promo-1',
        emoji: '🌾',
        title: '助农直采',
        subtitle: '产地直发 · 助力乡村振兴',
        link: '/pages/activity/list'
      },
      {
        type: 'promotion',
        style: 'promo-2',
        emoji: '🔥',
        title: '限时特惠',
        subtitle: '精选好物低至 5 折',
        link: '/pages/discount/index'
      },
      {
        type: 'promotion',
        style: 'promo-3',
        emoji: '🎁',
        title: '新人专享',
        subtitle: '注册即送 ¥50 大礼包',
        link: '/pages/coupon/index'
      }
    ]
  },
  
  onLoad(){ 
    this.loadList(); 
    this.loadSearchHistory();
  },
  
  // 加载商品列表
  async loadList(page = 1){  
    // 处理事件对象作为参数的情况（如点击重试按钮时）
    if (typeof page === 'object') {
      page = 1;
    }
    
    // 如果是第一页，显示加载动画
    if (page === 1) {
      this.setData({ loading: true, error: '' });
    } else {
      this.setData({ error: '' });
    }
    
    try {
      // 使用封装的GET请求
      const pageData = await get('/api/product/list', { 
        page: page, 
        pageSize: 20 
      });
      
      // 处理分页响应格式
        if (pageData) {
          // 处理商品图片链接，将相对路径转换为完整URL
          const processedRecords = pageData.records.map(product => {
            const coverUrl = resolveImageUrl(product.cover);
            
            if (!coverUrl) {
              return null;
            }
            
            return {
              ...product,
              cover: coverUrl,
              coverPath: product.cover && product.cover.startsWith('/') ? product.cover : ''
            };
          }).filter(p => p !== null);
          
          // 处理第一页或加载更多的情况
          const productList = page === 1 ? processedRecords : [...this.data.productList, ...processedRecords];
          
          // 静态推广 banner 已在 data 中初始化，商品加载不再覆盖
          
          this.setData({ 
          productList: productList,
          total: pageData.total,
          currentPage: page,
          hasMore: page < pageData.pages,
          loading: false
        });
      } else {
        this.setData({ 
          productList: [], 
          hasMore: false,
          loading: false
        });
      }
    } catch (error) {
      console.error('加载商品列表失败:', error);
      this.setData({ 
        productList: [], 
        hasMore: false,
        loading: false,
        error: '加载商品失败，请重试'
      });
    }
  },
  
  // 加载搜索历史
  loadSearchHistory(){ 
    // 获取当前用户ID
    const userId = app.globalData.userInfo ? app.globalData.userInfo.id : 0;
    // 使用用户ID作为缓存键
    const cacheKey = `searchHistory_${userId}`;
    const history = wx.getStorageSync(cacheKey) || [];
    this.setData({ searchHistory: history });
  },
  
  // 保存搜索历史
  saveSearchHistory(keyword){ 
    if(!keyword.trim()) return;
    
    // 获取当前用户ID
    const userId = app.globalData.userInfo ? app.globalData.userInfo.id : 0;
    // 使用用户ID作为缓存键
    const cacheKey = `searchHistory_${userId}`;
    
    let history = wx.getStorageSync(cacheKey) || [];
    
    // 去重，如果已存在则移到最前面
    const index = history.indexOf(keyword);
    if(index > -1){
      history.splice(index, 1);
    }
    
    // 添加到最前面
    history.unshift(keyword);
    
    // 限制历史记录数量为10条
    if(history.length > 10){
      history = history.slice(0, 10);
    }
    
    wx.setStorageSync(cacheKey, history);
    this.setData({ searchHistory: history });
  },
  
  // 搜索输入
  onSearchInput(e){ 
    this.setData({ searchKeyword: e.detail.value }); 
  },
  
  // 执行搜索
  async onSearch(page = 1){ 
    // 处理事件对象作为参数的情况（如点击搜索按钮时）
    if (typeof page === 'object') {
      page = 1;
    }
    
    const keyword = this.data.searchKeyword.trim();
    if(!keyword){ 
      this.loadList(page); 
      return; 
    }
    
    // 保存搜索历史
    this.saveSearchHistory(keyword);
    
    // 如果是第一页，显示加载动画
    if (page === 1) {
      this.setData({ loading: true, error: '' });
    } else {
      this.setData({ error: '' });
    }
    
    try {
      // 使用封装的GET请求
      const pageData = await get('/api/product/search', { 
        keyword: keyword, 
        page: page, 
        pageSize: 20 
      });
      
      // 处理分页响应格式
      if (pageData) {
        // 处理商品图片链接，将相对路径转换为完整URL
        const processedRecords = pageData.records.map(product => {
          const coverUrl = resolveImageUrl(product.cover);
          
          if (!coverUrl) {
            return null;
          }
          
          return { ...product, cover: coverUrl };
        }).filter(p => p !== null);
        
        // 处理第一页或加载更多的情况
        const productList = page === 1 ? processedRecords : [...this.data.productList, ...processedRecords];
        
        // 静态推广 banner 已在 data 中初始化，搜索不再覆盖
        
        this.setData({ 
          productList: productList,
          total: pageData.total,
          currentPage: page,
          hasMore: page < pageData.pages,
          loading: false
        }); 
      } else {
        this.setData({ 
          productList: [], 
          hasMore: false,
          loading: false
        });
      }
    } catch (error) {
      console.error('搜索失败:', error);
      this.setData({ 
        productList: [], 
        hasMore: false,
        loading: false,
        error: '搜索失败，请重试'
      });
    }
  },
  
  // 根据历史记录搜索
  searchByHistory(e){ 
    const keyword = e.currentTarget.dataset.keyword;
    this.setData({ searchKeyword: keyword });
    this.onSearch(); // 默认搜索第一页
  },
  
  // 清空搜索历史
  clearSearchHistory(){ 
    // 获取当前用户ID
    const userId = app.globalData.userInfo ? app.globalData.userInfo.id : 0;
    // 使用用户ID作为缓存键
    const cacheKey = `searchHistory_${userId}`;
    
    wx.showModal({
      title: '清空搜索历史',
      content: '确定要清空所有搜索历史吗？',
      success: (res) => {
        if(res.confirm){
          wx.removeStorageSync(cacheKey);
          this.setData({ searchHistory: [] });
        }
      }
    })
  },
  
  // 重置搜索
  onReset(){ 
    this.setData({ searchKeyword: '' }); 
    this.loadList(); // 默认加载第一页
  },
  
  // 加载更多商品
  loadMore(){ 
    if (!this.data.hasMore || this.data.loading) {
      return;
    }
    
    const nextPage = this.data.currentPage + 1;
    // 判断是普通列表还是搜索结果
    if (this.data.searchKeyword.trim()) {
      this.onSearch(nextPage);
    } else {
      this.loadList(nextPage);
    }
  },
  
  // 查看商品详情
  viewProduct(e){ 
    const id = e.currentTarget.dataset.id; 
    wx.navigateTo({ url: '/pages/product/product?id=' + id }); 
  },
  
  // 轮播图点击事件
  onBannerTap(e){ 
    const { type, id, link } = e.currentTarget.dataset;
    // 推广 banner 跳对应页面
    if (type === 'promotion' && link) {
      wx.navigateTo({ url: link });
      return;
    }
    // 商品 banner 跳商品详情
    if (id) {
      wx.navigateTo({ url: '/pages/product/product?id=' + id });
    }
  },
  
  // 跳转到订单列表
  goToOrderList(){ 
    wx.navigateTo({ url: '/pages/order/list' }); 
  },
  
  // 跳转到购物车
  goToCart(){ 
    wx.switchTab({ url: '/pages/cart/cart' }); 
  },
  
  // 跳转到用户中心
  goToUser(){ 
    wx.switchTab({ url: '/pages/user/index' }); 
  },
  
  // 跳转到分类页
  goToCategory(){ 
    wx.switchTab({ url: '/pages/category/category' }); 
  },
  
  // 分类点击事件
  onCategoryTap(e){ 
    const category = e.currentTarget.dataset.category;
    this.setData({ searchKeyword: category });
    this.onSearch();
  },

  // 预下载商品图片（解决HTTP协议限制）
  // 优化版：只下载当前显示的商品图片，避免内存溢出
  downloadProductImages(productList){
    // 只下载前6个商品的图片（当前屏幕可见的）
    const visibleProducts = productList.slice(0, 6);
    
    visibleProducts.forEach((product) => {
      // 只处理 HTTP 协议的图片
      if (product.cover && product.cover.startsWith('http://')) {
        wx.request({
          url: product.cover,
          responseType: 'arraybuffer',
          success: (res) => {
            if (res.statusCode === 200) {
              // 将 ArrayBuffer 转换为 Base64
              const base64 = wx.arrayBufferToBase64(res.data);
              // 添加 Base64 图片前缀
              const base64Image = 'data:image/jpeg;base64,' + base64;
              
              // 更新对应商品的图片路径为 Base64
              const updatedList = this.data.productList.map(item => {
                if (item.id === product.id) {
                  return { ...item, cover: base64Image };
                }
                return item;
              });
              this.setData({ productList: updatedList });
            }
          },
          fail: (err) => {
            console.warn('下载图片失败:', product.name, err);
          }
        });
      }
    });
  },

  onImageLoad(e) {
    // 图片加载成功
  },

  onImageError(e) {
    // 图片加载失败，使用默认图替换
    const idx = e.currentTarget.dataset.index;
    const type = e.currentTarget.dataset.type;
    if (type === 'banner' && idx !== undefined) {
      this.setData({ [`bannerList[${idx}].imageUrl`]: '/images/product-default.svg' });
    } else if (type === 'product' && idx !== undefined) {
      this.setData({ [`productList[${idx}].cover`]: '/images/product-default.svg' });
    }
  }
})
