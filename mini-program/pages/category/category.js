const app = getApp();
// 导入网络请求封装
const { get, resolveImageUrl } = require('../../utils/request');

Page({
  data: {
    topCategories: [], // 分类列表
    selectedCategory: null, // 选中的分类
    productList: [],
    loading: false,
    error: '',
    currentPage: 1,
    hasMore: true
  },
  
  onLoad() {
    this.loadCategories();
  },
  
  // 加载分类列表
  async loadCategories() {
    this.setData({ loading: true, error: '' });
    try {
      // 获取所有一级分类
      const allCategories = await get('/api/category/list');
      console.log('获取到的分类数据:', allCategories);
      
      if (allCategories && allCategories.length > 0) {
        // 处理分类数据，确保有parentId字段
        const topCategories = allCategories.map(cat => {
          // 如果没有parentId字段，默认设置为0（一级分类）
          if (!cat.hasOwnProperty('parentId')) {
            cat.parentId = 0;
          }
          return cat;
        }).filter(cat => cat.parentId === 0);
        
        console.log('处理后的一级分类:', topCategories);
        
        if (topCategories.length > 0) {
          this.setData({ 
            topCategories: topCategories,
            selectedCategory: topCategories[0],
            loading: false
          });
          
          // 加载第一个分类的商品
          this.loadProductsByCategory(topCategories[0].id);
        } else {
          // 如果没有一级分类，直接使用所有分类
          this.setData({ 
            topCategories: allCategories,
            selectedCategory: allCategories[0],
            loading: false
          });
          
          // 加载第一个分类的商品
          if (allCategories.length > 0) {
            this.loadProductsByCategory(allCategories[0].id);
          } else {
            this.setData({ 
              productList: [], 
              loading: false
            });
            wx.showToast({
              title: '暂无分类数据',
              icon: 'none'
            });
          }
        }
      } else {
        this.setData({ 
          topCategories: [], 
          selectedCategory: null,
          productList: [],
          loading: false
        });
        wx.showToast({
          title: '暂无分类数据',
          icon: 'none'
        });
      }
    } catch (error) {
      console.error('加载分类列表失败:', error);
      this.setData({ 
        topCategories: [], 
        selectedCategory: null,
        productList: [],
        loading: false,
        error: '加载分类失败，请重试'
      });
      wx.showToast({
        title: '加载分类失败，请重试',
        icon: 'none'
      });
    }
  },
  
  // 切换分类
  onCategoryChange(e) {
    const categoryId = e.currentTarget.dataset.id;
    const category = this.data.topCategories.find(cat => cat.id === categoryId);
    this.setData({ 
      selectedCategory: category,
      productList: [],
      currentPage: 1,
      hasMore: true,
      loading: false,
      error: ''
    });
    this.loadProductsByCategory(categoryId);
  },

  // 重试加载
  retryLoad() {
    if (this.data.selectedCategory) {
      this.loadProductsByCategory(this.data.selectedCategory.id);
    }
  },
  
  // 根据分类加载商品
  async loadProductsByCategory(categoryId, page = 1, pageSize = 20) {
    // 如果是第一页，显示加载动画
    if (page === 1) {
      this.setData({ loading: true, error: '' });
    } else {
      this.setData({ error: '' });
    }
    
    try {
      const pageData = await get(`/api/product/category/${categoryId}`, {
        page: page,
        pageSize: pageSize
      });
      
      if (pageData) {
        // 处理商品图片链接，将相对路径转换为完整URL
        const records = (pageData.records || []).map(product => ({
          ...product,
          cover: resolveImageUrl(product.cover)
        }));
        // 处理第一页或加载更多的情况
        const productList = page === 1 ? records : [...this.data.productList, ...records];
        
        this.setData({
          productList: productList,
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
      console.error('加载分类商品失败:', error);
      this.setData({ 
        productList: [], 
        hasMore: false,
        loading: false,
        error: '加载商品失败，请重试'
      });
    }
  },
  
  // 加载更多商品
  loadMore() {
    if (!this.data.hasMore || this.data.loading) {
      return;
    }
    
    const nextPage = this.data.currentPage + 1;
    this.loadProductsByCategory(this.data.selectedCategory.id, nextPage);
  },
  
  // 查看商品详情
  viewProduct(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/product/product?id=${id}` });
  }
});