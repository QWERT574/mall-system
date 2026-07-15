import { createRouter, createWebHashHistory } from 'vue-router';

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home/index.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Auth/Login.vue'),
    meta: { title: '登录', guest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Auth/Register.vue'),
    meta: { title: '注册', guest: true }
  },
  {
    path: '/reset-password',
    name: 'ResetPassword',
    component: () => import('@/views/Auth/ResetPassword.vue'),
    meta: { title: '重置密码', guest: true }
  },
  {
    path: '/cart',
    name: 'Cart',
    component: () => import('@/views/Cart/index.vue'),
    meta: { title: '购物车', auth: true }
  },
  {
    path: '/order',
    name: 'Order',
    component: () => import('@/views/Order/index.vue'),
    meta: { title: '订单', auth: true }
  },
  {
    path: '/order-list',
    name: 'OrderList',
    component: () => import('@/views/Order/List.vue'),
    meta: { title: '订单列表', auth: true }
  },
  {
    path: '/payment/:id',
    name: 'OrderDetail',
    component: () => import('@/views/Order/Payment.vue'),
    meta: { title: '订单详情', auth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile/index.vue'),
    meta: { title: '个人中心', auth: true }
  },
  {
    path: '/security',
    name: 'Security',
    component: () => import('@/views/Security/Account.vue'),
    meta: { title: '账户安全', auth: true }
  },
  {
    path: '/product/list',
    name: 'ProductList',
    component: () => import('@/views/Product/List.vue'),
    meta: { title: '全部商品' }
  },
  {
    path: '/product/:id',
    name: 'ProductDetail',
    component: () => import('@/views/Product/Detail.vue'),
    meta: { title: '商品详情' }
  },
  {
    path: '/activity/list',
    name: 'ActivityList',
    component: () => import('@/views/Activity/List.vue'),
    meta: { title: '助农活动列表' }
  },
  {
    path: '/activity/:id',
    name: 'ActivityDetail',
    component: () => import('@/views/Activity/Detail.vue'),
    meta: { title: '活动详情' }
  },
  {
    path: '/address/list',
    name: 'AddressList',
    component: () => import('@/views/Address/List.vue'),
    meta: { title: '收货地址', auth: true }
  },
  {
    path: '/address/add',
    name: 'AddressAdd',
    component: () => import('@/views/Address/Edit.vue'),
    meta: { title: '新增地址', auth: true }
  },
  {
    path: '/address/edit/:id',
    name: 'AddressEdit',
    component: () => import('@/views/Address/Edit.vue'),
    meta: { title: '编辑地址', auth: true }
  },
  {
    path: '/review/order/:orderId',
    name: 'OrderReview',
    component: () => import('@/views/Review/OrderReview.vue'),
    meta: { title: '订单评价', auth: true }
  },
  {
    path: '/service',
    name: 'ServiceChat',
    component: () => import('@/views/service/Chat.vue'),
    meta: { title: '客服聊天', auth: true }
  },
  {
    path: '/ai',
    name: 'AIAssistant',
    component: () => import('@/views/AI/index.vue'),
    meta: { title: 'AI助手' }
  },
  {
    path: '/aftersale',
    name: 'AfterSaleList',
    component: () => import('@/views/AfterSale/index.vue'),
    meta: { title: '售后服务', auth: true }
  },
  {
    path: '/aftersale/create',
    name: 'AfterSaleCreate',
    component: () => import('@/views/AfterSale/Create.vue'),
    meta: { title: '申请售后', auth: true }
  },
  {
    path: '/aftersale/:id',
    name: 'AfterSaleDetail',
    component: () => import('@/views/AfterSale/Detail.vue'),
    meta: { title: '售后详情', auth: true }
  },
  {
    path: '/coupons',
    name: 'CouponCenter',
    component: () => import('@/views/Coupon/index.vue'),
    meta: { title: '领券中心' }
  },
  {
    path: '/discount',
    name: 'DiscountList',
    component: () => import('@/views/Discount/index.vue'),
    meta: { title: '优惠活动' }
  },
  {
    path: '/discount/:id',
    name: 'DiscountDetail',
    component: () => import('@/views/Discount/Detail.vue'),
    meta: { title: '活动详情' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '页面未找到' }
  }
];

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition;
    } else {
      return { top: 0 };
    }
  }
});

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 乡村振兴` : '乡村振兴';

  const token = localStorage.getItem('token');

  if (to.meta.auth && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } });
  } else if (to.meta.guest && token) {
    next('/');
  } else {
    next();
  }
});

export default router;

