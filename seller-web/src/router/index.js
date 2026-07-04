import { createRouter, createWebHashHistory } from 'vue-router';

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Auth/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Auth/Register.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layouts/SellerLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard/index.vue'),
        meta: { title: '仪表盘' }
      },
      {
        path: 'products',
        name: 'ProductList',
        component: () => import('@/views/Product/index.vue'),
        meta: { title: '商品管理' }
      },
      {
        path: 'product/create',
        name: 'ProductCreate',
        component: () => import('@/views/Product/Form.vue'),
        meta: { title: '添加商品' }
      },
      {
        path: 'product/edit/:id',
        name: 'ProductEdit',
        component: () => import('@/views/Product/Form.vue'),
        meta: { title: '编辑商品' }
      },
      {
        path: 'orders',
        name: 'OrderList',
        component: () => import('@/views/Order/index.vue'),
        meta: { title: '订单管理' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile/index.vue'),
        meta: { title: '商家信息' }
      },
      {
        path: 'customer-service',
        name: 'CustomerService',
        component: () => import('@/views/CustomerService/Chat.vue'),
        meta: { title: '客服聊天' }
      },
      {
        path: 'reviews',
        name: 'ReviewManagement',
        component: () => import('@/views/Review/index.vue'),
        meta: { title: '评价管理' }
      },
      {
        path: 'aftersale',
        name: 'AfterSale',
        component: () => import('@/views/AfterSale/index.vue'),
        meta: { title: '售后管理' }
      },
      {
        path: 'coupons',
        name: 'CouponManage',
        component: () => import('@/views/Coupon/index.vue'),
        meta: { title: '优惠券管理' }
      },
      {
        path: 'discounts',
        name: 'DiscountManage',
        component: () => import('@/views/Discount/index.vue'),
        meta: { title: '打折活动' }
      }
    ]
  }
];

const router = createRouter({
  history: createWebHashHistory(),
  routes
});

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('seller_token');
  const user = JSON.parse(localStorage.getItem('seller_user') || '{}');
  
  // 如果需要认证但没有 token
  if (to.meta.requiresAuth && !token) {
    // 如果有用户信息但没有 token，也允许访问（让页面自己处理）
    if (user && user.id) {
      console.warn('Token 丢失但用户信息存在，允许访问');
      next();
    } else {
      next('/login');
    }
  } else if (to.meta.requiresAuth && user.userType !== 1) {
    alert('该账号不是商家账号');
    next('/login');
  } else if ((to.path === '/login' || to.path === '/register') && token) {
    next('/dashboard');
  } else {
    next();
  }
});

export default router;
